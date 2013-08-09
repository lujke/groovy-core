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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.*;
import java.util.*;

public class MethodHelper<T> {
    
    /* TODO: set correct value
     * { DefaultMetaMethod old, DefaultMetaMethod new ->
     *   old.signature == new.signature
     * }
     
    private final static MethodHandle equalTypesFilter = null;
    */
    private final static Lookup LOOKUP = MethodHandles.lookup();

    private static NameVisibilityIndex<DefaultMetaMethod> makeIndexFromClass(Class<?> theClass) {
        // add public and private methods into this
        Method[] declaredMethods = theClass.getDeclaredMethods();
        AccessibleObject.setAccessible(declaredMethods, true);
        if (declaredMethods.length == 0) return NameVisibilityIndex.EMPTY;

        NameVisibilityIndex temp = NameVisibilityIndex.EMPTY;
        // we to fill a map that goes by name and we want to gather all 
        // the methods before adding them, since the index will have to 
        // handle inheritance
        // (1)  sort by name
        Arrays.sort(declaredMethods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        // (2)  for each name make list of public and private methods
        //      and add the result to our map
        String lastName = null;
        LinkedList<DefaultMetaMethod> privateMethods = null;
        LinkedList<DefaultMetaMethod> publicMethods = null;

        for (Method m : declaredMethods) {
            // TODO: add mop renaming
            MethodHandle mh = unreflect(m);
            if (mh==null) continue; //TODO: see comment in unreflect method
            DefaultMetaMethod dmm = new DefaultMetaMethod(m.getName(), m.getModifiers(), mh);

            if (lastName==null) {
                privateMethods = new LinkedList();
                publicMethods = new LinkedList();
            } else if (!m.getName().equals(lastName)) {
                temp = temp.plus(lastName, publicMethods, privateMethods);
                privateMethods = new LinkedList();
                publicMethods = new LinkedList();
            }

            lastName = m.getName();
            if (Modifier.isPrivate(m.getModifiers())) {
                privateMethods.add(dmm);
            } else {
                publicMethods.add(dmm);
            }
        }
        return temp;
    }

    private static MethodHandle unreflect(Method m) {
        try {
            return LOOKUP.unreflect(m);
        } catch (IllegalAccessException e) {
            // TODO:ignore method?
            // on some systems like for example GAE the method
            // might be in general blocked. We can either ignore it (current solution)
            // or create a handle, which will produce an error if called.
            return null;
        }
    }

    public static NameVisibilityIndex<DefaultMetaMethod> createIndex(DefaultMetaClass metaClass) {
        Class<?> theClass = metaClass.getTheClass();

        NameVisibilityIndex<DefaultMetaMethod> methodsFromClass = makeIndexFromClass(theClass);
        NameVisibilityIndex<DefaultMetaMethod> theClassExtensions = metaClass.getExtensions(); 

        //  current class extension method hide current class methods
        return methodsFromClass.merge(theClassExtensions);
    }
}
