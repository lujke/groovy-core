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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import groovy.mop.internal.*;

import org.codehaus.groovy.runtime.ExceptionUtils;
import org.codehaus.groovy.runtime.NullObject;

/**
 * This is a Helper class for method invocations from Java world, 
 * if the groovy generated class and none of its super classes 
 * can be utilized for it.
 * <p>
 * Some defintions:
 * <dl>
 * <dt>receiver</dt>
 * <dd>A receiver is what we will use to call the method on. 
 * <dd>If the receiver is null a call on {@link NullObject} will be done
 * <dd>If the receiver is a class the call will be done to static method or on
 *      Class
 * <dt>method name</dt>
 * <dd>The name of the method, must not be null</dd>
 * <dt>arguments</dt>
 * <dd>As argument in the API here usually an Object[] containing zero or more 
 *     arguments for the method call.
 * <dd>
 * <dt>return value</dt>
 * <dd>The value returned by the method. If the method is void, then the value 
 *      will be null.
 * </dl>
 * <b>HINT:</b> Best performance can be achieved by using the target
 * class directly of course.
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
public final class GroovyInvoker {
    /** 
     * Invokes a method.
     * @param receiver - the object the method is invoked on
     * @param name - name of the method
     * @param args - arguments to the method call
     * @return - the result of the method call
     */
    public static <R> R invoke(Object receiver, String name, Object... args) {
       DefaultRealm realm = DefaultRealm.getRoot();
       Class c = null;
       if (receiver instanceof Class) {
           c = (Class) receiver;
       } else if (receiver != null) {
           c = receiver.getClass();
       } else {
           c = NullObject.class;
       }
       MOPCall call = new MOPCall(c,receiver,name,args);
       realm.getMetaClassInternal(c).selectMethod(call);
       /*MethodHandle mh = adaptForArrayCall(m.getTarget(), args);
       MethodHandles h;
       return (R) invokeExact(mh,args);*/
       return null;
    }

    public static <R> R invoke(MethodHandle mh, Object... args) {
        try {
            return (R) mh.invokeWithArguments(args);
        } catch (Throwable e) {
            ExceptionUtils.sneakyThrow(e);
            return null;
        }
    }

    public static <R> R invokeExact(MethodHandle mh, Object... args) {
        try {
            return (R) mh.invokeExact(args);
        } catch (Throwable e) {
            Unchecked.rethrow(e);
            return null;
        }
    }

    private static class Unchecked {

        public static void rethrow( final Throwable checkedException ) {
         Unchecked.<RuntimeException>thrownInsteadOf( checkedException );
        }

        //@SuppressWarnings("unchecked")
        private static <T extends Throwable> void thrownInsteadOf(Throwable t) throws T {
         throw (T) t;
        }
       }
}
