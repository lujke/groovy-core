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

import java.util.List;

public class SetCreator {
    public static <T> PSet<T> create(List<T> l) {
        if (l.isEmpty()) return EmptySet.create();
        if (l.size()==1) {
            return new SingleElementSet(l.get(0));
        } else {
            Object[] array = l.toArray();
            return new FlatSet(array);
        }
    }
    public static <T> PSet<T> create(T... l) {
        if (l.length==0) return EmptySet.create();
        if (l.length==1) return new SingleElementSet<T>(l[0]);
        return new FlatSet(l);
    }
}
