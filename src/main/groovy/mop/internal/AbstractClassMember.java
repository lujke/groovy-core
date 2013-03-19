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

import groovy.mop.ClassMember;

import java.lang.reflect.Modifier;

public abstract class AbstractClassMember implements ClassMember {
    private final String name;
    private final int modifiers;
    
    public AbstractClassMember(String name, int modifiers) {
        this.name = name;
        this.modifiers = modifiers;
    }
    
    public boolean isStatic() {
        return (getModifiers() & Modifier.STATIC) != 0;
    }

    public boolean isAbstract() {
        return (getModifiers() & Modifier.ABSTRACT) != 0;
    }

    public final boolean isPrivate() {
        return (getModifiers() & Modifier.PRIVATE) != 0;
    }

    public final boolean isProtected() {
        return (getModifiers() & Modifier.PROTECTED) != 0;
    }

    public final boolean isPublic() {
        return (getModifiers() & Modifier.PUBLIC) != 0;
    }

    public int getModifiers() {
        return modifiers;
    }

    /**
     * @return name of the member
     */
    public String getName() {
        return name;
    }
}
