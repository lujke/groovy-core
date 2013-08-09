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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import groovy.mop.Realm;
import groovy.mop.MetaClass;

public class DefaultRealm implements Realm {
    private static DefaultRealm ROOT = new DefaultRealm("ROOT"); 
    private static class MCRef {
        private DefaultMetaClass mc;
        public MCRef(DefaultMetaClass mc) {
            this.mc = mc;
        }
    }

    private final String name; 

    private final ClassValue<MCRef> cv = new ClassValue() {
        @Override
        protected MCRef computeValue(Class type) {
            DefaultMetaClass mc = new DefaultMetaClass(DefaultRealm.this, type);
            MetaClassEventSystemImpl.fireMetaClassCreation(DefaultRealm.this, mc);
            return new MCRef(mc);
        }
    };

    private DefaultRealm(String name) {
        this.name = name;
    }

    @Override
    public MetaClass getMetaClass(Class<?> theClass) {
        return new PublicMetaClassInterface(getMetaClassInternal(theClass));
    }

    public DefaultMetaClass getMetaClassInternal(Class<?> theClass) {
        return cv.get(theClass).mc;
    }

    public void setMetaClassInternal(DefaultMetaClass mc) {
        cv.get(mc.getTheClass()).mc = mc;
    }

    public static DefaultRealm getRoot() {
        return ROOT;
    }

    @Override
    public String toString() {
        return "Realm "+name;
    }

    public MetaClassRef getMetaClassHandleReferece(Class<?> theClass) {
        DefaultMetaClass mc = getMetaClassInternal(theClass);
        //TODO: add switchpoints
        MethodHandle handle = MethodHandles.constant(DefaultMetaClass.class, mc);
        return new MetaClassRef(handle);
    }
}
