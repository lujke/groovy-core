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

public final class MultiSet<T> implements PSet<T> {
    private final PSet<T> object;
    private final MultiSet<T> next;

    protected MultiSet(PSet<T> object, MultiSet<T> next) {
        this.object = object;
        this.next = next;
    }

    @Override
    public PSet<T> append(PSet<T> other) {
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
    
    @Override
    public PSet<T> minus(T element) {
        LinkedList<PSet> stack = new LinkedList();
        PSet changed = null;
        MultiSet current = this; 
        for (;current!=null; current = current.next) {
            PSet old = current.object;
            stack.addFirst(old);
            changed = old.minus(element);
            if (old!=changed) {
                stack.removeFirst();
                break;
            } else {
                changed = null;
            }
        }
        if (changed==null) return this;
        current = current.next;
        if (!changed.isEmpty()) current = new MultiSet<T>(changed,current);
        while (!stack.isEmpty()) {
            current = new MultiSet<T>(stack.removeFirst(),current);
        }
        if (current==null) return EmptySet.create();
        if (current.next==null) return current.object;
        return current;
    }

    @Override
    public PSet<T> plus(PSet<T> other, MethodHandle compare) {
        LinkedList<MultiSet> stack = new LinkedList();
        for (MultiSet current = this; current!=null; current = current.next) stack.add(current);
        MultiSet lastUnchanged = null;
        PSet changed = null;
        
        // first find out what we can reuse
        while (!stack.isEmpty()) {
            lastUnchanged = stack.removeFirst();
            PSet maybeChanged = lastUnchanged.object.minus(other, compare);
            if (maybeChanged==lastUnchanged.object) continue;
            lastUnchanged = lastUnchanged.next;
            changed = maybeChanged;
        }
        // if changed was never set, there is no change, thus return new set with this.
        if (changed==null) return append(other);
        // now make the new set
        MultiSet ms = new MultiSet(other,null);
        ms = new MultiSet(lastUnchanged,ms);
        if (!changed.isEmpty()) {
            ms = new MultiSet(changed, ms);
        }
        while (!stack.isEmpty()) {
            MultiSet old = stack.removeFirst();
            ms = new MultiSet(old.object,ms);
        }
        return ms;
    }
    
    @Override
    public String toString() {
        return object.toString()+"+"+next.toString();
    }
}
