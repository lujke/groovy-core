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

import java.util.concurrent.atomic.AtomicReference;

import groovy.mop.Realm;
import groovy.mop.MetaClass;

public class DefaultRealm implements Realm {
    private final ClassValue<AtomicReference<DefaultMetaClass>> cv = new ClassValue() {
        @Override
        protected AtomicReference<DefaultMetaClass> computeValue(Class type) {
            DefaultMetaClass mc = new DefaultMetaClass(DefaultRealm.this, type);
            return new AtomicReference<DefaultMetaClass>(mc);
        }
    };

    @Override
    public MetaClass getMetaClass(Class<?> theClass) {
        return new MetaClassHandle(cv.get(theClass).get());
    }
    
    public DefaultMetaClass getMetaClassInternal(Class<?> theClass) {
        return cv.get(theClass).get();
    }
    
    public void setMetaClassInternal(DefaultMetaClass mc) {
        cv.get(mc.getTheClass()).lazySet(mc);
    }
}
