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

import java.util.AbstractList;
import java.util.Iterator;

public class ListAdapter<T> extends AbstractList<T> {

    private final PSet<T> set;
    private int size = -1;

    public ListAdapter(PSet<T> set) {
        this.set = set;
    }
    
    @Override
    public int size() {
        if (size==-1) {
            int s = 0;
            for (@SuppressWarnings("unused") T t:set) s++;
            size = s;
            return s;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        for (T t : set) {
            if (t==o) return true;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @Override
    public T get(int index) {
        int i = -1;
        for (T t : set) {
            i++;
            if (i==index) return t;
        }
        throw new IndexOutOfBoundsException();
    }
}
