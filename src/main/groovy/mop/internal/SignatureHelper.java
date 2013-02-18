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

import groovy.lang.GString;
import groovy.mop.MetaMethod;
import groovy.mop.internal.pcollection.PSet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SignatureHelper {
    private static Map primitiveTypesMap = new HashMap();
    static {
        primitiveTypesMap.put(byte.class, Byte.class);
        primitiveTypesMap.put(boolean.class, Boolean.class);
        primitiveTypesMap.put(char.class, Character.class);
        primitiveTypesMap.put(double.class, Double.class);
        primitiveTypesMap.put(float.class, Float.class);
        primitiveTypesMap.put(int.class, Integer.class);
        primitiveTypesMap.put(long.class, Long.class);
        primitiveTypesMap.put(short.class, Short.class);
    }

    public static Class autoboxType(Class type) {
        final Class res = (Class) primitiveTypesMap.get(type);
        return res == null ? type : res;
    }
    
    private static boolean isVargs(Class[] types) {
        if (types.length==0) return false;
        return types[types.length-1].isArray();
    }
    
    public static boolean canBeCalledWithTypes(DefaultMetaMethod mm, Class... types) {
        if (types==null) return true;
        
        Class[] parameterTypes = mm.getParameterClasses();
        if (isVargs(parameterTypes)) return vargsMatch(parameterTypes, types);
        // nonvarargs match here
        if (types.length!=parameterTypes.length) return false;
        if (assignableTypes(parameterTypes, types, types.length)) return true;
        
        return false;
    }

    private static boolean vargsMatch(Class[] parameterTypes, Class[] types) {
        int argLength = types.length;
        int parLength = parameterTypes.length;
        // in varargs situations we can call (c1,c2,...cn,X...) without
        // argument for X. in that case parLength will be n+1 and
        // arguLength n. If argLength is less than that, we 
        // have a ci, that has no argument, which is no match then.
        if (argLength<parLength-1) return false; 
        // we check all ci, that is the arguments 0..parLength-1, if they
        // are matching
        if (!assignableTypes(parameterTypes, types, parLength-1)) return false;
        
        // if the lengths match we have either an array as argument or a single 
        // argument, that will be boxed into the array
        Class lastPar = parameterTypes[parLength-1];
        if (argLength == parLength) {
            if (assignableType(lastPar, types[argLength-1])) return true;
            return assignableType(lastPar.getComponentType(), types[argLength-1]);
        }
        
        // argLength > parLength, more arguments means that all of them have to go into X
        // no if require since only remaining case
        for (int i=parLength; i<argLength; i++) {
            if (assignableType(lastPar, types[i])) continue;
            return false;
        }
        return true;
    }

    private static boolean assignableTypes(Class[] parameterTypes, Class[] types, int length) {
        for (int i=0; i<length; i++) {
            if (assignableType(parameterTypes[i], types[i])) continue;
            return false;
        }
        return true;
    }
    
    public static boolean assignableType(Class parameterType, Class argumentType) {
        if (parameterType == argumentType) return true;
        if (argumentType == null) return true;
        if (parameterType == Object.class) return true;

        parameterType = autoboxType(parameterType);
        argumentType = autoboxType(argumentType);
        if (parameterType == argumentType) return true;

        // note: there is no coercion for boolean and char. Range matters, precision doesn't
        if (parameterType == Integer.class) {
            if (argumentType == Integer.class
                    || argumentType == Short.class
                    || argumentType == Byte.class
                    || argumentType == BigInteger.class)
                return true;
        } else if (parameterType == Double.class) {
            if (argumentType == Double.class
                    || argumentType == Integer.class
                    || argumentType == Long.class
                    || argumentType == Short.class
                    || argumentType == Byte.class
                    || argumentType == Float.class
                    || argumentType == BigDecimal.class
                    || argumentType == BigInteger.class)
                return true;
        } else if (parameterType == BigDecimal.class) {
            if (argumentType == Double.class
                    || argumentType == Integer.class
                    || argumentType == Long.class
                    || argumentType == Short.class
                    || argumentType == Byte.class
                    || argumentType == Float.class
                    || argumentType == BigDecimal.class
                    || argumentType == BigInteger.class)
                return true;
        } else if (parameterType == BigInteger.class) {
            if (argumentType == Integer.class
                    || argumentType == Long.class
                    || argumentType == Short.class
                    || argumentType == Byte.class
                    || argumentType == BigInteger.class)
                return true;
        } else if (parameterType == Long.class) {
            if (argumentType == Long.class
                    || argumentType == Integer.class
                    || argumentType == Short.class
                    || argumentType == Byte.class)
                return true;
        } else if (parameterType == Float.class) {
            if (argumentType == Float.class
                    || argumentType == Integer.class
                    || argumentType == Long.class
                    || argumentType == Short.class
                    || argumentType == Byte.class)
                return true;
        } else if (parameterType == Short.class) {
            if (argumentType == Short.class
                    || argumentType == Byte.class)
                return true;
        } else if (parameterType == String.class) {
            if (argumentType == String.class ||
                    GString.class.isAssignableFrom(argumentType)) {
                return true;
            }
        }

        return parameterType.isAssignableFrom(argumentType);
    }

    public static Collection<? extends MetaMethod> filter(PSet<? extends MetaMethod> methods, Class[] argumentTypes) {
        // TODO Auto-generated method stub
        return null;
    }
}
