/*
 * Copyright 2003-20013 the original author or authors.
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

import groovy.mop.MetaMethod;
import groovy.mop.MetaProperty;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Represents a property on a bean which may have a getter and/or a setter
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author Jochen Theodorou
 */
public class DefaultMetaProperty extends AbstractClassMember implements MetaProperty {
    private final Class type;
    private final List<MetaMethod> getter;
    private final MetaMethod setter, fieldSetter, fieldGetter;

    public DefaultMetaProperty(String name, Class type, List<MetaMethod> getter, MetaMethod setter, MetaMethod fieldGetter, MetaMethod fieldSetter) {
        super(name, Modifier.PUBLIC);
        this.type = type;
        this.getter = getter;
        this.setter = setter;
        this.fieldGetter = fieldGetter;
        this.fieldSetter = fieldSetter;
    }

    /**
     * @return the type of the property
     */
    public Class getType() {
        return type;
    }

    @Override
    public List<MetaMethod> getPropertyGetter() {
        return getter;
    }

    @Override
    public MetaMethod getPropertySetter() {
        return setter;
    }

    @Override
    public MetaMethod getFieldGetter() {
        return fieldGetter;
    }

    @Override
    public MetaMethod getFieldSetter() {
        return fieldSetter;
    }
}
