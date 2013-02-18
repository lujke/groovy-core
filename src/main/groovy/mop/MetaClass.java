/*
 * Copyright 2003-2009 the original author or authors.
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

import java.util.Collection;

/**
 * <p>An interface that defines the API usable by clients of Groovy's Meta Object Protocol (MOP). These methods are
 * implemented by the reference implementation of the {@link groovy.lang.MetaClass} interface.</p>
 * <p>All of the methods can be considered save in that they will never answer 
 * with an exception nor will the returned structures be subject to concurrent
 * changes. All structures returned in the methods here are of the style of
 * persistent collections.</p> 
 *
 * @see MetaClassImpl
 *
 * @author John Wilson
 * @author Graeme Rocher
 * @author Jochen Theodorou
 */
public interface MetaClass {

    /**
     * Obtain an immutable list of all meta properties available on this meta class
     *
     * @see groovy.lang.MetaProperty
     * @return A list of MetaProperty instances
     */
    Collection<? extends MetaProperty> getMetaProperties();

    /**
     * Obtain a meta property of the given name available on this meta class
     *
     * @see groovy.lang.MetaProperty
     * @return A list of MetaProperty instances
     */
    MetaProperty getMetaProperty(String name);

    /**
     * Obtain an immutable of all the meta methods available on this meta class
     *
     * @see groovy.lang.MetaMethod
     * @return A list of MetaMethod instances
     */
    Collection<? extends MetaMethod> getMetaMethods();

    /**
     * Obtain an immutable of all the meta methods with the given name available on this meta class.
     * If argument types is not null any method will match. If argumentTypes are given
     * it will be used to match by number and type. A null element is possible
     * and will match all types, still causing the number of parameters to match
     * against to raise. 
     *
     * @see groovy.lang.MetaMethod
     * @param argumentTypes An array containing the argument types or null.
     * @param name The name of the method of interest
     * @return A list of MetaMethod instances
     */
    Collection<? extends MetaMethod> getMetaMethods(String name, Class... argumentTypes);

    /**
     * <p>Returns an object satisfying Groovy truth if the implementing MetaClass responds to
     * a method with the given name and arguments types.
     *
     * @param name The name of the method of interest
     * @param argTypes The argument types to match against
     * @return A List of MetaMethods matching the argument types which will be empty if no matching methods exist
     */
    Collection<? extends MetaMethod> respondsTo(String name, Object... argTypes);

    /**
     * Retrieves that Java Class that the attached Meta behaviors apply to
     *
     * @return The java.lang.Class instance
     */
    Class<?> getTheClass();
}
