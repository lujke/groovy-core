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
import java.util.Collections;
import java.util.Iterator;

public class EmptySet<T> implements PSet<T> {

    private final static EmptySet EMPTY = new EmptySet();

    private EmptySet(){}
    
    public static <T> PSet<T> create() {
        return EMPTY;
    }
    
    @Override
    public Iterator<T> iterator() {
        return Collections.EMPTY_SET.iterator();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
    
    @Override
    public PSet<T> minus(PSet<T> other, MethodHandle compare) {
        return this;
    }
    
    @Override
    public PSet<T> append(PSet<T> other) {
        return other;
    }

    @Override
    public PSet<T> minus(T element) {
        return this;
    }

    @Override
    public PSet<T> plus(PSet<T> other, MethodHandle compare) {
        return other;
    }

}
