/*
 * Copyright 2003-2007 the original author or authors.
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

import java.lang.invoke.*;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Represents a Method on an object a little like {@link java.lang.reflect.Method}.
 *
 * @author Jochen Theodorou
 */
public class DefaultMetaMethod implements MetaMethod {
    public final static MethodHandle equalTargetType=null;
    
    
    private final MethodType signature;
    private final int modifiers;
    private final MethodHandle target;
    private final String name;
    private final Class declaringClass;

    public DefaultMetaMethod(Class declaringClass, String name, MethodType signature) {
        this.signature = signature;
        target = null;
        modifiers = Modifier.PUBLIC;
        this.name = name;
        this.declaringClass = declaringClass;
    }

    public DefaultMetaMethod(Class declaringClass, String name, int modifiers, MethodHandle target) {
        this.name = name;
        this.modifiers = modifiers;
        this.target = target;
        this.signature = target.type();
        this.declaringClass = declaringClass;
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getName() {
        return name;
    }

    public Class getReturnType() {
        return signature.returnType();
    }

    public String toString() {
        return super.toString()
            + "[name: "
            + getName()
            + " params: "
            + Arrays.toString(getParameterClasses())
            + " returns: "
            + getReturnType()
            + " owner: "
            + getDeclaringClass()
            + "]";
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

    public Class getDeclaringClass() {
        return declaringClass;
    }

    public Class[] getParameterClasses() {
        return signature.parameterArray();
    }

    public MethodHandle getTarget(){
        return target;
    }
    
    public MethodType getSignature() {
        return signature;
    }
}
