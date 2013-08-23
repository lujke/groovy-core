/*
 * Copyright 2003-2012 the original author or authors.
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
package org.codehaus.groovy.vmplugin.v7;

import groovy.mop.internal.*;

/*
import groovy.lang.AdaptingMetaClass;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyInterceptable;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import groovy.lang.MissingMethodException;
import groovy.lang.MetaClassImpl.MetaConstructor;
*/

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Modifier;

import org.codehaus.groovy.GroovyBugError;
/*import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.runtime.GeneratedClosure;
import org.codehaus.groovy.runtime.NullObject;
import org.codehaus.groovy.runtime.GroovyCategorySupport.CategoryMethod;
import org.codehaus.groovy.runtime.dgmimpl.NumberNumberMetaMethod;
import org.codehaus.groovy.runtime.metaclass.ClosureMetaClass;
import org.codehaus.groovy.runtime.metaclass.MetaClassRegistryImpl;
import org.codehaus.groovy.runtime.metaclass.MethodMetaProperty;
import org.codehaus.groovy.runtime.metaclass.NewInstanceMetaMethod;
import org.codehaus.groovy.runtime.metaclass.NewStaticMetaMethod;
import org.codehaus.groovy.runtime.metaclass.ReflectionMetaMethod;
import org.codehaus.groovy.runtime.wrappers.Wrapper;*/

import org.codehaus.groovy.runtime.NullObject;
import org.codehaus.groovy.vmplugin.v7.IndyInterface.CALL_TYPES;

import static org.codehaus.groovy.vmplugin.v7.IndyInterface.*;
import static org.codehaus.groovy.vmplugin.v7.IndyGuardsFiltersAndSignatures.*;

public abstract class Selector {
    public Object[] args, originalArguments;
    public MethodType targetType,currentType;
    public String name;
    public MethodHandle handle;
    public boolean cache = true;
    public MutableCallSite callSite;
    public Class sender;
//    public boolean isVargs;
    public boolean safeNavigation, safeNavigationOrig, spread;
    public boolean skipSpreadCollector;
    public boolean thisCall;
    public Class selectionBase;
    public CALL_TYPES callType;

    /**
     * Returns the Selector
     */
    public static Selector getSelector(MutableCallSite callSite, Class sender, String methodName, int callID, boolean safeNavigation, boolean thisCall, boolean spreadCall, Object[] arguments) {
        CALL_TYPES callType = CALL_TYPES.values()[callID];
        switch (callType) {
            case INIT: return new InitSelector(callSite, sender, methodName, callType, safeNavigation, thisCall, spreadCall, arguments);
            case METHOD: return new MethodSelector(callSite, sender, methodName, callType, safeNavigation, thisCall, spreadCall, arguments);
            case GET: 
                return new PropertySelector(callSite, sender, methodName, callType, safeNavigation, thisCall, spreadCall, arguments);
            case SET:
                throw new GroovyBugError("your call tried to do a property set, which is not supported.");
        }
        return null;
    }
    public abstract void setCallSiteTarget();

    /**
     * Helper method to transform the given arguments, consisting of the receiver 
     * and the actual arguments in an Object[], into a new Object[] consisting
     * of the receiver and the arguments directly. Before the size of args was 
     * always 2, the returned Object[] will have a size of 1+n, where n is the
     * number arguments.
     */
    private static Object[] spread(Object[] args, boolean spreadCall) {
        if (!spreadCall) return args;
        Object[] normalArguments = (Object[]) args[1];
        Object[] ret = new Object[normalArguments.length+1];
        ret[0] = args[0];
        System.arraycopy(normalArguments, 0, ret, 1, ret.length-1);
        return ret;
    }

    private static class PropertySelector extends MethodSelector {
        private boolean insertName = false;

        public PropertySelector(MutableCallSite callSite, Class sender, String methodName, CALL_TYPES callType, boolean safeNavigation, boolean thisCall, boolean spreadCall, Object[] arguments) {
            super(callSite, sender, methodName, callType, safeNavigation, thisCall, spreadCall, arguments);
        }

        /**
         * this method chooses a property from the meta class.
         */
        /*@Override
        public void chooseMeta(MetaClassImpl mci) {
            Object receiver = getCorrectedReceiver();
            if (receiver instanceof GroovyObject) {
                Class aClass = receiver.getClass();
                Method reflectionMethod = null;
                try {
                    reflectionMethod = aClass.getMethod("getProperty", String.class);
                    if (!reflectionMethod.isSynthetic()) {
                        handle = MethodHandles.insertArguments(GROOVY_OBJECT_GET_PROPERTY, 1, name);
                        return;
                    }
                } catch (ReflectiveOperationException e)  {}
            } else if (receiver instanceof Class) {
                handle = MOP_GET;
                handle = MethodHandles.insertArguments(handle, 2, name);
                handle = MethodHandles.insertArguments(handle, 0, this.mc);
                return;
            }

            if (method!=null || mci==null) return;
            MetaProperty res = mci.getEffectiveGetMetaProperty(mci.getTheClass(), receiver, name, false);
            if (res instanceof MethodMetaProperty) {
                MethodMetaProperty mmp = (MethodMetaProperty) res;
                method = mmp.getMetaMethod();
                insertName = true;
            } else if (res instanceof CachedField) {
                CachedField cf = (CachedField) res;
                Field f = cf.field;
                try {
                    handle = LOOKUP.unreflectGetter(f);
                    if (Modifier.isStatic(f.getModifiers())) {
                        // normally we would do the following
                        // handle = MethodHandles.dropArguments(handle,0,Class.class);
                        // but because there is a bug in invokedynamic in all jdk7 versions 
                        // maybe use Unsafe.ensureClassInitialized
                        handle = META_PROPERTY_GETTER.bindTo(res);
                    }
                } catch (IllegalAccessException iae) {
                    throw new GroovyBugError(iae);
                }
            } else {
                handle = META_PROPERTY_GETTER.bindTo(res);
            } 
        }*/

        /**
         * Additionally to the normal {@link MethodSelector#setHandleForMetaMethod()}
         * task we have to also take care of generic getter methods, that depend
         * one the name.
         */
        @Override
        public void setHandleForMetaMethod() {
            if (handle!=null) return;
            super.setHandleForMetaMethod();
            if (handle != null && insertName && handle.type().parameterCount()==2) {
                handle = MethodHandles.insertArguments(handle, 1, name);
            }
        }

        /**
         * The MOP requires all get property operations to go through 
         * {@link GroovyObject#getProperty(String)}. We do this in case 
         * no property was found before.
         */
        /*@Override
        public void setMetaClassCallHandleIfNedded(boolean standardMetaClass) {
            if (handle!=null) return;
            useMetaClass = true;
            if (LOG_ENABLED) LOG.info("set meta class invocation path for property get.");
            handle = MethodHandles.insertArguments(MOP_GET, 2, this.name);
            handle = MethodHandles.insertArguments(handle, 0, mc);
        }*/
    }

    private static class InitSelector extends MethodSelector {
//        private boolean beanConstructor;

        public InitSelector(MutableCallSite callSite, Class sender, String methodName, CALL_TYPES callType, boolean safeNavigation, boolean thisCall, boolean spreadCall, Object[] arguments) {
            super(callSite, sender, methodName, callType, safeNavigation, thisCall, spreadCall, arguments);
        }

        /**
         * For a constructor call we always use the static meta class from the registry
         */
        /*@Override
        public void getMetaClass() {
            Object receiver = args[0];
            mc = GroovySystem.getMetaClassRegistry().getMetaClass((Class) receiver);
        }*/

        /**
         * This method chooses a constructor from the meta class.
         */
        /*@Override
        public void chooseMeta(MetaClassImpl mci) {
            if (mci==null) return;
            if (LOG_ENABLED) LOG.info("getting constructor");
            Object[] newArgs = removeRealReceiver(args);
            method = mci.retrieveConstructor(newArgs);
            if (method instanceof MetaConstructor) {
                MetaConstructor mcon = (MetaConstructor) method;
                if (mcon.isBeanConstructor()) {
                    if (LOG_ENABLED) LOG.info("do beans constructor");
                    beanConstructor = true;
                }
            }
        }*/

        /**
         * Adds {@link MetaConstructor} handling.
         */
        /*@Override
        public void setHandleForMetaMethod() {
            if (method==null) return;
            if (method instanceof MetaConstructor) {
                if (LOG_ENABLED) LOG.info("meta method is MetaConstructor instance");
                MetaConstructor mc = (MetaConstructor) method;
                isVargs = mc.isVargsMethod();
                Constructor con = mc.getCachedConstrcutor().cachedConstructor;
                try {
                    handle = LOOKUP.unreflectConstructor(con);
                    if (LOG_ENABLED) LOG.info("successfully unreflected constructor");
                } catch (IllegalAccessException e) {
                    throw new GroovyBugError(e);
                }
            } else {
                super.setHandleForMetaMethod();
            }
            if (beanConstructor) {
                // we have handle that takes no arguments to create the bean, 
                // we have to use its return value to call #setBeanProperties with it
                // and the meta class.

                // to do this we first bind the values to #setBeanProperties
                MethodHandle con = BEAN_CONSTRUCTOR_PROPERTY_SETTER.bindTo(mc);
                // inner class case
                MethodType foldTargetType = MethodType.methodType(Object.class);
                if (args.length==3) {
                    con = MethodHandles.dropArguments(con, 1, targetType.parameterType(1));
                    foldTargetType = foldTargetType.insertParameterTypes(0, targetType.parameterType(1));
                }
                handle = MethodHandles.foldArguments(con, handle.asType(foldTargetType));
            }
            if (method instanceof MetaConstructor) {
                handle = MethodHandles.dropArguments(handle, 0, Class.class);
            }
        }*/

        /**
         * In case of a bean constructor we don't do any varags or implicit null argument 
         * transformations. Otherwise we do the same as for {@link MethodSelector#correctParameterLength()}
         */
        /*@Override
        public void correctParameterLength() {
            if (beanConstructor) return;
            super.correctParameterLength();
        }*/

        /**
         * In case of a bean constructor we don't do any coercion, otherwise
         * we do the same as for {@link MethodSelector#correctCoerce()}
         */
        /*@Override
        public void correctCoerce() {
            if (beanConstructor) return;
            super.correctCoerce();
        }*/

        /**
         * Set MOP based constructor invocation path.
         */
        /*@Override
        public void setMetaClassCallHandleIfNedded(boolean standardMetaClass) {
            if (handle!=null) return;
            useMetaClass = true;
            if (LOG_ENABLED) LOG.info("set meta class invocation path");
            handle = MOP_INVOKE_CONSTRUCTOR.bindTo(mc);
            handle = handle.asCollector(Object[].class, targetType.parameterCount()-1);
            handle = MethodHandles.dropArguments(handle, 0, Class.class);
            if (LOG_ENABLED) LOG.info("create collector for arguments");
        }*/
    }

    /**
     * Method invocation based {@link Selector}.
     * This Selector is called for method invocations and is base for cosntructor
     * calls as well as getProperty calls.
     * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
     */
    private static class MethodSelector extends Selector {
        protected DefaultMetaClass mc;
//        private boolean isCategoryMethod;
        public MethodSelector(MutableCallSite callSite, Class sender, String methodName, CALL_TYPES callType, Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object[] arguments) {
            this.callType = callType;
            this.targetType = callSite.type();
            this.name = methodName;
            this.originalArguments = arguments;
            this.args = spread(arguments, spreadCall);
            this.callSite = callSite;
            this.sender = sender;
            this.safeNavigationOrig = safeNavigation;
            this.safeNavigation = safeNavigation && arguments[0]==null;
            this.thisCall = thisCall;
            this.spread = spreadCall;
            this.cache = !spread;

            if (LOG_ENABLED) {
                String msg =
                    "----------------------------------------------------"+
                    "\n\t\tinvocation of method '"+methodName+"'"+
                    "\n\t\tinvocation type: "+callType+
                    "\n\t\tsender: "+sender+
                    "\n\t\ttargetType: "+targetType+
                    "\n\t\tsafe navigation: "+safeNavigation+
                    "\n\t\tthisCall: "+thisCall+
                    "\n\t\tspreadCall: "+spreadCall+
                    "\n\t\twith "+arguments.length+" arguments";
                for (int i=0; i<arguments.length; i++) {
                    msg += "\n\t\t\targument["+i+"] = "+arguments[i];
                }
                LOG.info(msg);
            }
        }

        /**
         * Sets the null constant for safe navigation.
         * In case of foo?.bar() and foo being null, we don't call the method,
         * instead we simply return null. This produces a handle, which will 
         * return the constant.
         */
        public boolean setNullForSafeNavigation() {
            if (!safeNavigation) return false;
            handle = MethodHandles.dropArguments(NULL_REF,0,targetType.parameterArray());
            if (LOG_ENABLED) LOG.info("set null returning handle for safe navigation");
            return true;
        }

        /**
         * Gives the meta class to an Object.
         */
        public void getMetaClass() {
            DefaultRealm realm = DefaultRealm.getRoot();
            Object receiver = args[0];
            Class c = null;
            if (receiver instanceof Class) {
                c = (Class) receiver;
            } else if (receiver != null) {
                c = receiver.getClass();
            } else {
                c = NullObject.class;
            }
            mc = realm.getMetaClassInternal(c);
        }

        /**
         * Creates a MethodHandle using a before selected MetaMethod.
         * If the MetaMethod has reflective information available, then
         * we will use that information to create the target MethodHandle. 
         * If that is not the case we will produce a handle, which will use the
         * MetaMethod itself for invocation.
         */
        public void setHandleForMetaMethod() {
            MOPCall call = new MOPCall(selectionBase,name,args);
            mc.selectMethod(call);
            handle = call.target;
        }

        /*private MethodHandle correctClassForNameAndUnReflectOtherwise(Method m) throws IllegalAccessException {
            if (m.getDeclaringClass()==Class.class && m.getName().equals("forName") && m.getParameterTypes().length==1) {
                return MethodHandles.insertArguments(CLASS_FOR_NAME, 1, true, sender.getClassLoader());
            } else {
                return LOOKUP.unreflect(m);
            }
        }*/

        /**
         * Helper method to manipulate the given type to replace Wrapper with Object.
         */
        /*private MethodType removeWrapper(MethodType targetType) {
            Class[] types = targetType.parameterArray();
            for (int i=0; i<types.length; i++) {
                if (types[i]==Wrapper.class) {
                    targetType = targetType.changeParameterType(i, Object.class);
                }
            }
            return targetType;
        }*/

        /**
         * Corrects method argument wrapping.
         * In cases in which we want to force a certain method selection
         * we use Wrapper classes to transport the static type information.
         * This method will be used to undo the wrapping.
         */
        /*public void correctWrapping() {
            if (useMetaClass) return;
            Class[] pt = handle.type().parameterArray();
            if (currentType!=null) pt = currentType.parameterArray();
            for (int i=1; i<args.length; i++) {
                if (args[i] instanceof Wrapper) {
                    Class type = pt[i];
                    MethodType mt = MethodType.methodType(type, Wrapper.class);
                    handle = MethodHandles.filterArguments(handle, i, UNWRAP_METHOD.asType(mt));
                    if (LOG_ENABLED) LOG.info("added filter for Wrapper for argument at pos "+i);
                }
            }
        }*/

        /**
         * Handles cases in which we have to correct the length of arguments
         * using the parameters. This might be needed for vargs and for one 
         * parameter calls without arguments (null is used then).  
         */
/*        public void correctParameterLength() {
            if (handle==null) return;

            Class[] params = handle.type().parameterArray();
            if (currentType!=null) params = currentType.parameterArray();
            if (!isVargs) {
                if (spread && useMetaClass) return;
                if (params.length==2 && args.length==1) {
                    //TODO: this Object[] can be constant
                    handle = MethodHandles.insertArguments(handle, 1, new Object[]{null});
                }
                return;
            }

            Class lastParam = params[params.length-1];
            Object lastArg = unwrapIfWrapped(args[args.length-1]);
            if (params.length == args.length) {
                // may need rewrap
                if (lastArg == null) return;
                if (lastParam.isInstance(lastArg)) return;
                if (lastArg.getClass().isArray()) return;
                // arg is not null and not assignment compatible
                // so we really need to rewrap
                handle = handle.asCollector(lastParam, 1);
            } else if (params.length > args.length) {
                // we depend on the method selection having done a good 
                // job before already, so the only case for this here is, that
                // we have no argument for the array, meaning params.length is
                // args.length+1. In that case we have to fill in an empty array
                handle = MethodHandles.insertArguments(handle, params.length-1, Array.newInstance(lastParam.getComponentType(), 0));
                if (LOG_ENABLED) LOG.info("added empty array for missing vargs part");
            } else { //params.length < args.length
                // we depend on the method selection having done a good 
                // job before already, so the only case for this here is, that
                // all trailing arguments belong into the vargs array
                handle = handle.asCollector(
                        lastParam,
                        args.length - params.length + 1);
                if (LOG_ENABLED) LOG.info("changed surplus arguments to be collected for vargs call");
            }
        }
*/
        /**
         * There are some conversions we have to do explicitly.
         * These are GString to String, Number to Byte and Number to BigInteger
         * conversions.
         */
/*        public void correctCoerce() {
            if (useMetaClass) return;

            Class[] parameters = handle.type().parameterArray();
            if (currentType!=null) parameters = currentType.parameterArray();
            if (args.length != parameters.length) {
                throw new GroovyBugError("At this point argument array length and parameter array length should be the same");
            }
            for (int i=0; i<args.length; i++) {
                if (parameters[i]==Object.class) continue; 
                Object arg = unwrapIfWrapped(args[i]);
                // we have to handle here different cases in which we do no
                // transformations. We depend on our method selection to have
                // selected only a compatible method, that means for a null
                // argument we don't have to do anything. Same of course is if
                // the argument is an instance of the parameter type. We also
                // exclude boxing, since the MethodHandles will do that part
                // already for us. Another case is the conversion of a primitive
                // to another primitive or of the wrappers, or a combination of 
                // these. This is also handled already. What is left is the 
                // GString conversion and the number conversions.

                if (arg==null) continue;
                Class got = arg.getClass();

                // equal class, nothing to do
                if (got==parameters[i]) continue;

                Class wrappedPara = TypeHelper.getWrapperClass(parameters[i]);
                // equal class with one maybe a primitive, the later explicitCastArguments will solve this case
                if (wrappedPara==TypeHelper.getWrapperClass(got)) continue;

                // equal in terms of an assignment in Java. That means according to Java widening rules, or
                // a subclass, interface, superclass relation, this case then handles also 
                // primitive to primitive conversion. Those case are also solved by explicitCastArguments.
                if (parameters[i].isAssignableFrom(got)) continue;

                // to aid explicitCastArguments we convert to the wrapper type to let is only unbox
                handle = TypeTransformers.addTransformer(handle, i, arg, wrappedPara);
                if (LOG_ENABLED) LOG.info("added transformer at pos "+i+" for type "+got+" to type "+wrappedPara);
            }
        }
*/

        /**
         * Gives a replacement receiver for null.
         * In case of the receiver being null we want to do the method
         * invocation on NullObject instead.
         */
/*        public void correctNullReceiver() {
            if (args[0]!=null) return;
            handle = handle.bindTo(NullObject.getNullObject());
            handle = MethodHandles.dropArguments(handle, 0, targetType.parameterType(0));
            if (LOG_ENABLED) LOG.info("binding null object receiver and dropping old receiver");
        }*/

        public void correctSpreading() {
            if (!spread || skipSpreadCollector) return;
            handle = handle.asSpreader(Object[].class, args.length-1);
        }
        
        /**
         * Sets all argument and receiver guards.
         */
        public void setGuards (Object receiver) {
            if (handle==null) return;
            if (!cache) return;

            MethodHandle fallback = makeFallBack(callSite, sender, name, callType.ordinal(), targetType, safeNavigationOrig, thisCall, spread);

            // special guards for receiver
            //TODO: add same meta class check
            /*if (receiver instanceof GroovyObject) {
                GroovyObject go = (GroovyObject) receiver;
                MetaClass mc = (MetaClass) go.getMetaClass();
                MethodHandle test = SAME_MC.bindTo(mc); 
                // drop dummy receiver
                test = test.asType(MethodType.methodType(boolean.class,targetType.parameterType(0)));
                handle = MethodHandles.guardWithTest(test, handle, fallback);
                if (LOG_ENABLED) LOG.info("added meta class equality check");
            }

            if (!useMetaClass && isCategoryMethod) {
                // category method needs Thread check
                // cases:
                // (1) method is a category method
                //     We need to check if the category in the current thread is still active.
                //     Since we invalidate on leaving the category checking for it being
                //     active directly is good enough.
                // (2) method is in use scope, but not from category
                //     Since entering/leaving a category will invalidate, there is no need for any special check
                // (3) method is not in use scope /and not from category
                //     Since entering/leaving a category will invalidate, there is no need for any special check
                if (method instanceof NewInstanceMetaMethod) {
                    handle = MethodHandles.guardWithTest(HAS_CATEGORY_IN_CURRENT_THREAD_GUARD, handle, fallback);
                    if (LOG_ENABLED) LOG.info("added category-in-current-thread-guard for category method");
                }
            }

            // handle constant meta class and category changes
            handle = switchPoint.guardWithTest(handle, fallback);
            if (LOG_ENABLED) LOG.info("added switch point guard");
*/
            // guards for receiver and parameter
            Class[] pt = handle.type().parameterArray();
            for (int i=0; i<args.length; i++) {
                Object arg = args[i];
                MethodHandle test = null;
                if (arg==null) {
                    test = IS_NULL.asType(MethodType.methodType(boolean.class, pt[i]));
                    if (LOG_ENABLED) LOG.info("added null argument check at pos "+i);
                } else { 
                    Class argClass = arg.getClass();
                    if (Modifier.isFinal(argClass.getModifiers()) && TypeHelper.argumentClassIsParameterClass(argClass,pt[i])) continue;
                    test = SAME_CLASS.
                                bindTo(argClass).
                                asType(MethodType.methodType(boolean.class, pt[i]));
                    if (LOG_ENABLED) LOG.info("added same class check at pos "+i);
                }
                Class[] drops = new Class[i];
                for (int j=0; j<drops.length; j++) drops[j] = pt[j];
                test = MethodHandles.dropArguments(test, 0, drops);
                handle = MethodHandles.guardWithTest(test, handle, fallback);
            }
        }

        /**
         * do the actual call site target set, if the call is supposed to be cached
         */
        public void doCallSiteTargetSet() {
            if (!cache) {
                if (LOG_ENABLED) LOG.info("call site stays uncached");
            } else {
                callSite.setTarget(handle);
                if (LOG_ENABLED) LOG.info("call site target set, preparing outside invocation");
            }
        }

        /**
         * Sets the selection base.
         */
        public void setSelectionBase() {
            if (thisCall) {
                selectionBase = sender;
            } else if (args[0]==null) {
                selectionBase = NullObject.class;
            } else {
                selectionBase = mc.getTheClass();
            }
            if (LOG_ENABLED) LOG.info("selection base set to "+selectionBase);
        }

        /**
         * setting a call site target consists of the following steps:
         * # get the meta class
         * # select a method/constructor/property from it, if it is a MetaClassImpl
         * # make a handle out of the selection
         * # if nothing could be selected select a path through the given MetaClass or the GroovyObject
         * # apply transformations for vargs, implicit null argument, coercion, wrapping, null receiver and spreading
         */
        @Override
        public void setCallSiteTarget() {
            if (!setNullForSafeNavigation()) {
                getMetaClass();
                if (LOG_ENABLED) LOG.info("meta class is "+mc);
                setSelectionBase();
                setHandleForMetaMethod();
                /*correctParameterLength();
                correctCoerce();
                correctWrapping();
                correctNullReceiver();*/
                correctSpreading();

                if (LOG_ENABLED) LOG.info("casting explicit from "+handle.type()+" to "+targetType);
                handle =  MethodHandles.explicitCastArguments(handle,targetType);
            }
            setGuards(args[0]);
            doCallSiteTargetSet();
        }
    }

    /**
     * Unwraps the given object from a {@link Wrapper}. If not
     * wrapped, the given object is returned.
     */
    /*private static Object unwrapIfWrapped(Object object) {
        if (object instanceof Wrapper) return unwrap(object);
        return object;
    }*/

    /**
     * Returns {@link NullObject#getNullObject()} if the receiver
     * (args[0]) is null. If it is not null, the recevier itself
     * is returned.
     */
    public Object getCorrectedReceiver() {
        Object receiver = args[0];
        if (receiver==null) {
            if (LOG_ENABLED) LOG.info("receiver is null");
            receiver = NullObject.getNullObject();
        }
        return receiver;
    }

    /**
     * Returns if a method is static
     */
    /*private static boolean isStatic(Method m) {
        int mods = m.getModifiers();
        return (mods & Modifier.STATIC) != 0;
    }*/

    /**
     * Helper method to remove the receiver from the argument array
     * by producing a new array.
     */
    /*private static Object[] removeRealReceiver(Object[] args) {
        Object[] ar = new Object[args.length-1];
        for (int i=1; i<args.length; i++) {
            ar[i-1] = args[i];
        }
        return ar;
    }*/
}