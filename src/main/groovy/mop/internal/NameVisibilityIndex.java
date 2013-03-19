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

public class NameVisibilityIndex<V> {
    public static final NameVisibilityIndex EMPTY = new NameVisibilityIndex(Hamt.EMPTY, Hamt.EMPTY);
    
    public final Hamt<String, PSet<V>> pub;
    public final Hamt<String, PSet<V>> priv;
    private PSet<V> pubSet;
    
    public NameVisibilityIndex(Hamt pub, Hamt priv){
        this.pub = pub;
        this.priv = priv;
    }

    public PSet<V> get(String name) {
        return pub.getEntry(name).getValue().append(priv.getEntry(name).getValue());
    }
    
    public PSet<V> subPublic() {
        if (pubSet!=null) return pubSet;
        PSet newSet = EmptySet.create();
        for (PSet set : pub) newSet = newSet.append(set);
        return pubSet = newSet;
    }

    public NameVisibilityIndex plus(String name, List<V> publics, List<V> privates) {
        PSet set = SetCreator.create(publics);
        Hamt newPub = pub.plus(name,set);
        set = SetCreator.create(privates);
        Hamt newPriv = priv.plus(name,set);
        return new NameVisibilityIndex<>(newPub, newPriv);
    }

    public NameVisibilityIndex<V> merge(List<NameVisibilityIndex<V>> mergeList) {
        Hamt newPub = pub;
        Hamt newPriv = priv;
        for (NameVisibilityIndex nvi : mergeList) {
            newPub = pub.merge(nvi.pub);
            newPriv = priv.merge(nvi.priv);
        }
        return new NameVisibilityIndex(newPub, newPriv);
    }
}
