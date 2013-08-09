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

import java.util.Collection;

import groovy.mop.MetaClass;
import groovy.mop.MetaMethod;
import groovy.mop.MetaProperty;
import groovy.mop.internal.pcollection.*;

/**
 * This meta class provides the user with methods to interface with the real
 * internal meta class.
 * 
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 * @see MetaClass
 */
public class PublicMetaClassInterface implements MetaClass {
    private final DefaultMetaClass metaClass;
    
    PublicMetaClassInterface(DefaultMetaClass mc) {
        this.metaClass = mc;
    }

    @Override
    public Collection<? extends MetaProperty> getMetaProperties() {
        PSet<MetaProperty> properties = metaClass.getProperties(null,null);
        return new ListAdapter(properties);
    }

    @Override
    public MetaProperty getMetaProperty(String name) {
        PSet<MetaProperty> properties = metaClass.getProperties(null,name);
        if (properties.isEmpty()) return null;
        return properties.iterator().next();
    }

    @Override
    public Collection<? extends MetaMethod> getMetaMethods() {
        PSet<? extends MetaMethod> methods = metaClass.getMethods(null,null);
        return new ListAdapter(methods);
    }

    @Override
    public Collection<? extends MetaMethod> getMetaMethods(String name, Class... argumentTypes) {
        PSet<? extends MetaMethod> methods = metaClass.getMethods(null,name);
        return SignatureHelper.filter(methods, argumentTypes);
    }

    @Override
    public Collection<? extends MetaMethod> respondsTo(String name, Object... args) {
        return getMetaMethods(name, MetaClassHelper.convertToTypeArray(args));
    }

    @Override
    public Class<?> getTheClass() {
        return metaClass.getTheClass();
    }

}
