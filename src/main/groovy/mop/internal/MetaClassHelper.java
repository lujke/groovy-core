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

import java.math.BigDecimal;
import java.math.BigInteger;

import groovy.lang.GString;


/**
 * @author John Wilson
 * @author Jochen Theodorou
 */
public class MetaClassHelper {
    /*
     * dist binary layout:
     * 0-20: interface
     * 21-22: primitive dist
     * 23-43: object dist
     * 44-48: vargs penalty
     */
    private static final int
        OBJECT_SHIFT = 23, INTERFACE_SHIFT = 0,
        PRIMITIVE_SHIFT = 21, VARGS_SHIFT = 44;
    
    private static final Class[] PRIMITIVES = {
        byte.class, Byte.class, short.class, Short.class,
        int.class, Integer.class, long.class, Long.class,
        BigInteger.class, float.class, Float.class,
        double.class, Double.class, BigDecimal.class,
        Number.class, Object.class
    };

    private static final int[][] PRIMITIVE_DISTANCE_TABLE = {
        //              byte    Byte    short   Short   int     Integer     long    Long    BigInteger  float   Float   double  Double  BigDecimal, Number, Object
        /* byte*/{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,},
        /*Byte*/{1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,},
        /*short*/{14, 15, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,},
        /*Short*/{14, 15, 1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,},
        /*int*/{14, 15, 12, 13, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,},
        /*Integer*/{14, 15, 12, 13, 1, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,},
        /*long*/{14, 15, 12, 13, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,},
        /*Long*/{14, 15, 12, 13, 10, 11, 1, 0, 2, 3, 4, 5, 6, 7, 8, 9,},
        /*BigInteger*/{9, 10, 7, 8, 5, 6, 3, 4, 0, 14, 15, 12, 13, 11, 1, 2,},
        /*float*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 0, 1, 2, 3, 4, 5, 6,},
        /*Float*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 1, 0, 2, 3, 4, 5, 6,},
        /*double*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 0, 1, 2, 3, 4,},
        /*Double*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 1, 0, 2, 3, 4,},
        /*BigDecimal*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 3, 4, 0, 1, 2,},
        /*Number*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 3, 4, 2, 0, 1,},
        /*Object*/{14, 15, 12, 13, 10, 11, 8, 9, 7, 5, 6, 3, 4, 2, 1, 0,},
    };

    private static int getPrimitiveIndex(Class c) {
        for (byte i = 0; i < PRIMITIVES.length; i++) {
            if (PRIMITIVES[i] == c) return i;
        }
        return -1;
    }

    /**
     * Convert argument array to the type array. This conversion
     * is null safe. If an args element is null
     * the resulting class will be null. If args itself is null
     * we return null.
     *
     * @param args the arguments
     * @return the types of the arguments
     */
    public static Class[] convertToTypeArray(Object[] args) {
        if (args == null) return null;
        int s = args.length;
        Class[] ans = new Class[s];
        for (int i = 0; i < s; i++) {
            Object o = args[i];
            ans[i] = getClassWithNull(o);
        }
        return ans;
    }

    private static Class getClassWithNull(Object arg) {
        if (arg == null) return null;
        return arg.getClass();
    }
    
    public static long calculateParameterDistance(Class[] arguments, Class[] parameters) {
        if (parameters.length == 0 && arguments.length == 0) return 0;

        long ret = 0;
        int noVargsLength = parameters.length - 1;

        // if the number of parameters does not match we have 
        // a vargs usage
        //
        // case A: arguments.length<parameters.length
        //
        //         In this case arguments.length is always equal to
        //         noVargsLength because only the last parameter
        //         might be a optional vargs parameter
        //
        //         VArgs penalty: 1l
        //
        // case B: arguments.length>parameters.length
        //
        //         In this case all arguments with a index bigger than
        //         paramMinus1 are part of the vargs, so a 
        //         distance calculation needs to be done against 
        //         parameters[noVargsLength].getComponentType()
        //
        //         VArgs penalty: 2l+arguments.length-parameters.length
        //
        // case C: arguments.length==parameters.length && 
        //         isAssignableFrom( parameters[noVargsLength],
        //                           arguments[noVargsLength] )
        //
        //         In this case we have no vargs, so calculate directly
        //
        //         VArgs penalty: 0l
        //
        // case D: arguments.length==parameters.length && 
        //         !isAssignableFrom( parameters[noVargsLength],
        //                            arguments[noVargsLength] )
        //
        //         In this case we have a vargs case again, we need 
        //         to calculate arguments[noVargsLength] against
        //         parameters[noVargsLength].getComponentType
        //
        //         VArgs penalty: 2l
        //
        //         This gives: VArgs_penalty(C)<VArgs_penalty(A)
        //                     VArgs_penalty(A)<VArgs_penalty(D)
        //                     VArgs_penalty(D)<VArgs_penalty(B)

        /**
         * In general we want to match the signature that allows us to use
         * as less arguments for the vargs part as possible. That means the
         * longer signature usually wins if both signatures are vargs, while
         * vargs looses always against a signature without vargs.
         *
         *  A vs B :
         *      def foo(Object[] a) {1}     -> case B
         *      def foo(a,b,Object[] c) {2} -> case A
         *      assert foo(new Object(),new Object()) == 2
         *  --> A preferred over B
         *
         *  A vs C :
         *      def foo(Object[] a) {1}     -> case B
         *      def foo(a,b)        {2}     -> case C
         *      assert foo(new Object(),new Object()) == 2
         *  --> C preferred over A
         *
         *  A vs D :
         *      def foo(Object[] a) {1}     -> case D
         *      def foo(a,Object[] b) {2}   -> case A
         *      assert foo(new Object()) == 2
         *  --> A preferred over D
         *
         *  This gives C<A<B,D
         *
         *  B vs C :
         *      def foo(Object[] a) {1}     -> case B
         *      def foo(a,b) {2}            -> case C
         *      assert foo(new Object(),new Object()) == 2
         *  --> C preferred over B, matches C<A<B,D
         *
         *  B vs D :
         *      def foo(Object[] a)   {1}   -> case B
         *      def foo(a,Object[] b) {2}   -> case D
         *      assert foo(new Object(),new Object()) == 2
         *  --> D preferred over B
         *
         *  This gives C<A<D<B 
         */

        // first we calculate all arguments, that are for sure not part
        // of vargs.  Since the minimum for arguments is noVargsLength
        // we can safely iterate to this point
        for (int i = 0; i < noVargsLength; i++) {
            if (!SignatureHelper.assignableType(parameters[i],arguments[i])) return -1;
            ret += calculateParameterDistance(arguments[i], parameters[i]);
        }

        if (arguments.length == parameters.length) {
            // case C&D, we use baseType to calculate and set it
            // to the value we need according to case C and D
            Class baseType = parameters[noVargsLength]; // case C
            if (SignatureHelper.assignableType(baseType, arguments[noVargsLength])) {
                ret += calculateParameterDistance(arguments[noVargsLength], baseType);
            } else { // case D
                baseType = baseType.getComponentType();
                if (SignatureHelper.assignableType(baseType, arguments[noVargsLength])) {
                    ret += calculateParameterDistance(arguments[noVargsLength], baseType);
                    ret += 2l << VARGS_SHIFT; // penalty for vargs
                } else {
                    return -1;
                }
            }
        } else if (arguments.length > parameters.length) {
            // case B
            // we give our a vargs penalty for each exceeding argument and iterate
            // by using parameters[noVargsLength].getComponentType()
            ret += (2l + arguments.length - parameters.length) << VARGS_SHIFT; // penalty for vargs
            Class vargsType = parameters[noVargsLength].getComponentType();
            for (int i = noVargsLength; i < arguments.length; i++) {
                ret += calculateParameterDistance(arguments[i], vargsType);
            }
        } else {
            // case A
            // we give a penalty for vargs, since we have no direct
            // match for the last argument
            ret += 1l << VARGS_SHIFT;
        }

        return ret;
    }

    private static long calculateParameterDistance(Class argument, Class parameter) {
        if (parameter == argument) return 0;

        if (parameter.isInterface()) {
            return getMaximumInterfaceDistance(argument, parameter) << INTERFACE_SHIFT;
        }

        long objectDistance = 0;
        if (argument != null) {
            long pd = getPrimitiveDistance(parameter, argument);
            if (pd != -1) return pd << PRIMITIVE_SHIFT;

            // add one to dist to be sure interfaces are preferred
            objectDistance += PRIMITIVES.length + 1;

            // GROOVY-5114 : if we have to choose between two methods
            // foo(Object[]) and foo(Object) and that the argument is an array type
            // then the array version should be preferred
            if (argument.isArray() && !parameter.isArray()) {
                objectDistance += 4;
            }
            Class clazz = SignatureHelper.autoboxType(argument);
            while (clazz != null) {
                if (clazz == parameter) break;
                if (clazz == GString.class && parameter == String.class) {
                    objectDistance += 2;
                    break;
                }
                clazz = clazz.getSuperclass();
                objectDistance += 3;
            }
        } else {
            // choose the distance to Object if a parameter is null
            // this will mean that Object is preferred over a more
            // specific type
            Class clazz = parameter;
            if (clazz.isPrimitive()) {
                objectDistance += 2;
            } else {
                while (clazz != Object.class) {
                    clazz = clazz.getSuperclass();
                    objectDistance += 2;
                }
            }
        }
        return objectDistance << OBJECT_SHIFT;
    }

    private static int getMaximumInterfaceDistance(Class c, Class interfaceClass) {
        // -1 means a mismatch
        if (c == null) return -1;
        // 0 means a direct match
        if (c == interfaceClass) return 0;
        Class[] interfaces = c.getInterfaces();
        int max = -1;
        for (Class anInterface : interfaces) {
            int sub = getMaximumInterfaceDistance(anInterface, interfaceClass);
            // we need to keep the -1 to track the mismatch, a +1
            // by any means could let it look like a direct match
            // we want to add one, because there is an interface between
            // the interface we search for and the interface we are in.
            if (sub != -1) sub++;
            // we are interested in the longest path only
            max = Math.max(max, sub);
        }
        // we do not add one for super classes, only for interfaces
        int superClassMax = getMaximumInterfaceDistance(c.getSuperclass(), interfaceClass);
        return Math.max(max, superClassMax);
    }

    private static int getPrimitiveDistance(Class from, Class to) {
        // we know here that from!=to, so a distance of 0 is never valid
        // get primitive type indexes
        int fromIndex = getPrimitiveIndex(from);
        int toIndex = getPrimitiveIndex(to);
        if (fromIndex == -1 || toIndex == -1) return -1;
        return PRIMITIVE_DISTANCE_TABLE[toIndex][fromIndex];
    }
}
