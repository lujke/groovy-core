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

public final class MultiSet<T> implements PSet<T> {
    private final PSet<T> object;
    private final MultiSet<T> next;

    protected MultiSet(PSet<T> object, MultiSet<T> next) {
        this.object = object;
        this.next = next;
    }

    public PSet<T> plus(PSet<T> other) {
        if (other.isEmpty()) return this;
        MultiSet<T> l = null;
        if (other instanceof MultiSet) {
            l = (MultiSet<T>) other;
        } else {
             l = new MultiSet<>(other, null);
        }
        return new MultiSet<>(this,l);
    }

    private static class Iter<E> implements Iterator<E>{
        private MultiSet<E> current;
        private Iterator<E> iter;
        
        public Iter(MultiSet<E> start) {
            current = start;
            iter = start.iterator();
        }
        
        @Override
        public boolean hasNext() {
            if (iter.hasNext()) return true;
            return current.next != null;
        }

        @Override
        public E next() {
            if (iter.hasNext()) return iter.next();
            current = current.next;
            iter = current.iterator();
            return next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public Iterator<T> iterator() {
        return new Iter(this); 
    }

    @Override
    public boolean isEmpty() {
        return next==null && object.isEmpty();
    }

    @Override
    public PSet<T> minus(PSet<T> other, MethodHandle compare) {
        if (other == this) return EmptySet.create();
        if (other == this.object) {
            if (next == null) return EmptySet.create();
            return next;
        }
        FlatSet<T> flat = this.flatten();
        PSet<T> res = flat.minus(other, compare);
        if (flat==res) return this;
        return res;
    }

    private FlatSet<T> flatten() {
        // TODO Auto-generated method stub
        return null;
    }
}
