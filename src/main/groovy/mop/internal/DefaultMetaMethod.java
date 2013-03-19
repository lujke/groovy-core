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
import java.util.Arrays;

/**
 * Represents a Method on an object a little like {@link java.lang.reflect.Method}.
 *
 * @author Jochen Theodorou
 */
public class DefaultMetaMethod extends AbstractClassMember implements MetaMethod {
    private final MethodType signature;
    private final MethodHandle target;

    public DefaultMetaMethod(String name, int modifiers, MethodHandle target) {
        super(name, modifiers);
        this.target = target;
        this.signature = target.type();
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
            + "]";
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
