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
import java.util.LinkedList;

public class MOPCall {
    public LinkedList<DefaultMetaMethod> errorList;
    public String name;
    public Class baseClass;
    public Object receiver;
    public Object[] args;
    public Class[] types;
    public MethodHandle target;

    public MOPCall(Class baseClass, String name, Object[] args) {
        this.name = name;
        this.receiver = args[0];
        this.args = args;
        this.baseClass = baseClass;
        this.types = MetaClassHelper.convertToTypeArray(args);
    }

    private static Object[] rewrap(Object receiver, Object[] args) {
        Object[] newArgs = new Object[args.length+1];
        newArgs[0] = receiver;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }

    //TODO: maybe remove this constructor
    public MOPCall(Class baseClass, Object receiver, String name, Object[] args) {
        this(baseClass, name, rewrap(receiver,args)); 
    }

}
