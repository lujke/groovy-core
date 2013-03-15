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

import org.codehaus.groovy.runtime.ExceptionUtils;

public final class SingleElementSet<T> implements PSet<T> {

    private final T element;
    SingleElementSet(T t) {
        this.element = t;
    }
    
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean done = false;
            @Override
            public boolean hasNext() {
                return !done;
            }
            @Override
            public T next() {
                return element;
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
        if (other instanceof SingleElementSet) {
            SingleElementSet<T> s2 = (SingleElementSet<T>) other;
            return new FlatSet<T>((T[]) new Object[]{element, s2.element});
        } 
        return new MultiSet(this, new MultiSet<>(other,null));
    }

    @Override
    public PSet<T> minus(PSet<T> other, MethodHandle compare) {
        if (other==this) return this;
        for (T t : other) { 
            try {
                boolean compareResult = (boolean) (compare.invoke(element, t));
                if (compareResult) return EmptySet.create();
            } catch (Throwable e) {
                ExceptionUtils.sneakyThrow(e);
            }
        }
        return this;
    }

    @Override
    public PSet<T> minus(T element) {
        if (this.element==element) return EmptySet.create();
        return this;
    }

    @Override
    public PSet<T> plus(PSet<T> other, MethodHandle compare) {
        return this.minus(other,compare).append(other);
    }

/*
    private boolean contains(Iterable<T> other, MethodHandle compare) {
        for (T t : other) {
            boolean b = false;
            try {
                b = (Boolean) compare.invokeWithArguments(element, t);
            } catch (Throwable e) {
                ExceptionUtils.sneakyThrow(e);
            }
            if (b) return true;
        }
        return false;
    }*/
}
