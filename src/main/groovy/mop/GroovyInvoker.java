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
import java.lang.invoke.MethodType;
import java.util.Map;

import groovy.mop.internal.*;

import org.codehaus.groovy.runtime.ExceptionUtils;
import org.codehaus.groovy.runtime.NullObject;

/**
 * This is a Helper class for method invocations from Java world, 
 * if the groovy generated class and none of its super classes 
 * can be utilized for it.
 * <p>
 * Some definitions:
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
    private static final MethodType INVOKE_EXACT_TYPE = MethodType.methodType(Object.class,MethodHandle.class,Object.class,Object[].class);

    /** 
     * Invokes a method.
     * @param receiver - the object the method is invoked on
     * @param name - name of the method
     * @param args - arguments to the method call
     * @return - the result of the method call
     */
    public static <R> R invoke(Object receiver, String name, Object... args) {
        MethodHandle target = getMethodHandle(receiver, name, args);
        MethodHandle mh = MethodHandles.spreadInvoker(target.type(), args.length+1);
        mh = MethodHandles.explicitCastArguments(mh, INVOKE_EXACT_TYPE);
        try {
            return (R) mh.invokeExact(target, receiver, args);
        } catch (Throwable e) {
            Unchecked.rethrow(e);
            return null;
        }
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

    public static MethodHandle getMethodHandle(Object receiver, String name, Object... args) {
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
        return call.target;
    }

    public static void setProperty(Script script, String string, String[] args) {
        // TODO TBD

    }

    public static Script createScript(Class scriptClass, Binding context) {
        Script script = null;
        // for empty scripts
        if (scriptClass == null) {
            script = new Script() {
                public Object run() {
                    return null;
                }
            };
        } else {
            final Object object = newInstance(scriptClass);
            if (object instanceof Script) {
                script = (Script) object;
            } else {
                // it could just be a class, so lets wrap it in a Script
                // wrapper
                // though the bindings will be ignored
                script = new Script() {
                    public Object run() {
                        Object args = getBinding().getVariables().get("args");
                        if (args != null && args instanceof String[]) {
                            GroovyInvoker.invoke(object, "main", args);
                        } else {
                            GroovyInvoker.invoke(object, "main");
                        }
                        return null;
                    }
                };
                setProperties(object, context.getVariables());
            }
        }
        script.setBinding(context);
        return script;
    }

    public static void setProperties(Object receiver, Map<String,?> propertyValues) {
        //TODO: TBD
    }

    public static <T> T newInstance(Class<T> clazz) {
      //TODO: TBD
        return null;
    }
}
