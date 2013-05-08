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

import java.lang.ref.SoftReference;
import java.util.ArrayList;

import groovy.mop.MetaClass;
import groovy.mop.MetaClassEventSystem;
import groovy.mop.Realm;
import groovy.mop.internal.pcollection.EmptySet;
import groovy.mop.internal.pcollection.PSet;
import groovy.mop.internal.pcollection.SetCreator;

public class MetaClassEventSystemImpl extends MetaClassEventSystem {
    
    private PSet<SoftReference<MetaClassCreationEventListener>> creationListener = EmptySet.create();
    public void addMetaClassCreationEventListener(MetaClassCreationEventListener listener) {
        PSet<SoftReference<MetaClassCreationEventListener>> newSet = SetCreator.create(new SoftReference<>(listener));
        synchronized(this) {
            creationListener = creationListener.append(newSet);
        }
    }
    public void removeMetaClassRegistryChangeEventListener(MetaClassCreationEventListener listener) {
        synchronized(this) {
            for (SoftReference<MetaClassCreationEventListener> ref : creationListener) {
                if (ref.get()!=listener) continue;
                creationListener = creationListener.minus(ref);
            }
        }
    }
    public MetaClassCreationEventListener[] getMetaClassCreationEventListeners() {
        synchronized(this) {
            ArrayList<MetaClassCreationEventListener> list = new ArrayList();
            for (SoftReference<MetaClassCreationEventListener> ref : creationListener) {
                MetaClassCreationEventListener l = ref.get();
                if (l!=null) list.add(l);
            }
            return list.toArray(new MetaClassCreationEventListener[list.size()]);
        }
    }
    protected static void fireMetaClassCreation(DefaultRealm realm, DefaultMetaClass dmc) {
        MetaClassEventSystemImpl sys = (MetaClassEventSystemImpl) MetaClassEventSystem.getInstance();
        if (sys.creationListener.isEmpty()) return;
        MetaClass mc = new MetaClassHandle(dmc);
        MetaClassCreationEvent event = new MetaClassCreationEvent(realm, mc);
        synchronized (sys) {
            for (SoftReference<MetaClassCreationEventListener> ref : sys.creationListener) {
                MetaClassCreationEventListener l = ref.get();
                if (l==null) continue;
                l.onMetaClassCreationEvent(event);
            }
        }
    }
}
