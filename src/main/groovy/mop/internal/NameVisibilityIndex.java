/*
 * Copyright 2003-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.mop.internal;

import java.util.List;

import groovy.mop.internal.pcollection.*;
import groovy.mop.internal.pcollection.Hamt.Entry;

public class NameVisibilityIndex<V> {
    // I use Object here because otherwise generics complain
    public static final NameVisibilityIndex EMPTY = new NameVisibilityIndex(Hamt.EMPTY, Hamt.EMPTY, EmptySet.create()) {
        public String toString() {return "NameVisibilityIndex.EMPTY";};
        @Override
        public NameVisibilityIndex merge(NameVisibilityIndex other) {
            return other;
        };
    };

    public final Hamt<String, PSet<V>> pub;
    public final Hamt<String, PSet<V>> priv;
    public final PSet<V> constr;
//    private PSet<V> pubSet;

    public NameVisibilityIndex(Hamt pub, Hamt priv, PSet constr){
        this.pub = pub;
        this.priv = priv;
        this.constr = constr;
    }

    public PSet<V> getPubPriv(String name) {
        Entry<String, PSet<V>> pubEntry = pub.getEntry(name);
        Entry<String, PSet<V>> privEntry = priv.getEntry(name);
        if (pubEntry==null && privEntry==null) return EmptySet.create();
        if (pubEntry==null) return privEntry.getValue();
        if (privEntry==null) return pubEntry.getValue();
        return pub.getEntry(name).getValue().append(priv.getEntry(name).getValue());
    }

    /*public PSet<V> subPublic() {
        if (pubSet!=null) return pubSet;
        PSet newSet = EmptySet.create();
        for (PSet set : pub) newSet = newSet.append(set);
        return pubSet = newSet;
    }*/

    public NameVisibilityIndex plus(String name, List<V> publics, List<V> privates) {
        PSet set = SetCreator.create(publics);
        Hamt newPub = set.isEmpty()? pub : pub.plus(name,set);
        set = SetCreator.create(privates);
        Hamt newPriv = set.isEmpty()? priv : priv.plus(name,set);
        if (newPub == pub && newPriv == priv) return this;
        return new NameVisibilityIndex<>(newPub, newPriv, constr);
    }

    public NameVisibilityIndex<V> merge(NameVisibilityIndex<V> other) {
        if (other==EMPTY) return this;
        Hamt newPub = pub.merge(other.pub);
        Hamt newPriv = priv.merge(other.priv);
        PSet newConstr = constr.append(other.constr);
        if (newPub==pub && newPriv==priv && newConstr==constr) return this;
        return new NameVisibilityIndex(newPub, newPriv, newConstr);
    }
}
