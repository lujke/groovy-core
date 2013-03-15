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
import java.util.Iterator;
import java.util.LinkedList;

import org.codehaus.groovy.runtime.ExceptionUtils;

public final class FlatSet<T> implements PSet<T> {
    private final T[] elements; 

    protected FlatSet(T[] elements) {
        this.elements = elements;
    }
    
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;
            @Override
            public boolean hasNext() {
                return index<elements.length;
            }
            @Override
            public T next() {
                index++;
                return (T) elements[index];
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public PSet<T> append(PSet<T> other) {
        if (other.isEmpty()) return this;
        MultiSet ms = null;
        if (other instanceof MultiSet) {
            ms = (MultiSet) other;
        } else {
            ms = new MultiSet<>(other, null);
        }
        return new MultiSet<T>(this, ms);
    }
    
    @Override
    public PSet<T> minus(PSet<T> other, MethodHandle compare) {
        if (other.isEmpty()) return this;
        PSet<T> res = minus_0(elements, other, compare, 0, elements.length);
        if (res==null) return this;
        return res;
    }
    
    private static <T> int find(T[] elements, PSet<T> other, MethodHandle compare, int index, int max, boolean negate) throws Throwable {
        OUTER: while (index<=max) {
            index ++;
            T element = elements[index];
            for (T t:other) {
                boolean test = (boolean) (compare.invoke(element,t));
                if (test^negate) continue OUTER;
            }
            return index;
        }
        return index;
    }
    
    protected static <T> int find_0(T[] elements, T searchElement, final int is, final int ie) {
        for (int i=is; i<ie; i++) {
            if (elements[i]==searchElement) return i;
        }
        return -1;
    }
    
    protected static <T> PSet<T> minus_0(T[] elements, PSet<T> other, MethodHandle compare, final int is, final int ie) {
        try {
            LinkedList<PSet<T>> results = new LinkedList();
            int start = is;
            while (start<ie) {
                int offset = find(elements, other, compare, start-1, ie, false);
                if (offset==ie) break;
                int end = find(elements, other, compare, offset, ie, true);
                if (offset==is && end==ie) return null;
                if (end-offset==1) {
                    results.addFirst(new SingleElementSet<T>(elements[offset]));
                    break;
                } 
                results.addFirst(new RegionSet(elements, offset, end));
                start = end;
            }
            int size = results.size();
            if (size==0) return EmptySet.create();
            if (size==1) return results.getFirst();

            MultiSet ms = null;
            for (PSet<T> p : results) ms = new MultiSet<>(p, ms);
            return ms;
        } catch (Throwable e) {
            ExceptionUtils.sneakyThrow(e);
        }
        return null;
    }

    @Override
    public PSet<T> plus(PSet<T> other, MethodHandle compare) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PSet<T> minus(T element) {
        // TODO Auto-generated method stub
        return null;
    }

}
