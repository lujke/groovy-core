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
package groovy.mop.internal.pcollection;

import java.lang.invoke.MethodHandle;

public interface PSet<T> extends Iterable<T> {

    /**
     * Returns true if the set is empty 
     * @return emptiness of the set
     */
    boolean isEmpty();
    /**
     * Appends a set at this one. If the appended set is empty
     * then this set is returned. If this set is empty the set to be
     * appended is returned. No check will be done for element 
     * duplication 
     * @param other - the set to be appended
     * @return - the combined set
     */
    PSet<T> append(PSet<T> other);
    /**
     * Adds another set to this set while not adding duplicates. If other
     * is empty, the current set will be returned. If the current set is
     * empty, the other set will be returned.
     * @param other - the set to be added
     * @param compare - the comparator
     * @return the combined set
     */
    PSet<T> plus(PSet<T> other, MethodHandle compare);
    /**
     * Removes the given element from the set by using referential identity.
     * If no element is removed the current set is returned.
     * @param element the element to remove
     * @return the new set
     */
    PSet<T> minus(T element);
    /**
     * Removes all elements of the given set from the current set using the
     * given comparator. If no elements are removed or other is empty the 
     * current set is returned. 
     * @param other
     * @param compare
     * @return
     */
    PSet<T> minus(PSet<T> other, MethodHandle compare);
}
