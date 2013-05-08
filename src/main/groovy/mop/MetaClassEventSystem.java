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
package groovy.mop;

import groovy.mop.internal.MetaClassEventSystemImpl;

import java.util.EventObject;
import java.util.EventListener;

public abstract class MetaClassEventSystem {
    private final static MetaClassEventSystem instance = new MetaClassEventSystemImpl();
    public static MetaClassEventSystem getInstance() {
        return instance;
    }
    
    public static class MetaClassCreationEvent extends EventObject {
        private final MetaClass newMetaClass;
        public MetaClassCreationEvent(Realm source, MetaClass newMetaClass) {
            super(source);
            this.newMetaClass = newMetaClass;
        }

        public MetaClass getNewMetaClass() {
            return newMetaClass;
        }
    }
    public interface MetaClassCreationEventListener extends EventListener {
        void onMetaClassCreationEvent(MetaClassCreationEvent mcce);
    }
    public abstract void addMetaClassCreationEventListener(MetaClassCreationEventListener listener);
    public abstract void removeMetaClassRegistryChangeEventListener(MetaClassCreationEventListener listener);
    public abstract MetaClassCreationEventListener[] getMetaClassCreationEventListeners();
}
