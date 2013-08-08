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

import java.lang.invoke.*;
import java.util.*;

import org.codehaus.groovy.GroovyBugError;

public class Hamt<K,V>  implements Iterable<V> {
    private static final MethodHandle equalEntryKeys;
    static {
        try {
            equalEntryKeys =
                    MethodHandles.lookup().
                    findStatic(Hamt.class, "equalEntryKeys", MethodType.methodType(boolean.class, Entry.class, Entry.class)).
                    asType(MethodType.methodType(boolean.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new GroovyBugError(e);
        }
    }
    
    @SuppressWarnings("unused")
    private static boolean equalEntryKeys(Entry e1, Entry e2) {
        return e1.key.equals(e2.key);
    }
    
    private static class Empty<K,V> extends Hamt<K,V> {
        public Empty(){ super(null); }
        @Override public Entry getEntry(K key) {
            return null;
        }
        @Override
        public Hamt<K,V> plus(K key, V value) {
            return new Hamt<K,V>(new Entry(key,value,hash(key)));
        }
    }
    public static abstract class Node<K> {
        public Node<K> getChild(int index) {
            throw new UnsupportedOperationException();
        }
        public boolean isLeaf() {return false;}
        public boolean isFullNode() {return false;}
        public Entry getEntry(K key){return null;}
        public abstract Node<K> replace(int hashCode, int level, Node<K> newEntry, Node<K> oldEntry);
        public abstract Node<K> plus(int hashCode, int level, Node<K> entry);
        public abstract Node<K> merge(Node<K> otherRoot, int level);
        public int getHash() {
            throw new GroovyBugError("should not reach here");
        }
    }
    public static class Entry<K,V> extends Node<K> implements Map.Entry<K, V> {
        private final K key;
        private final V value;
        private final int hash;
        
        public Entry(K key, V value, int hash) {
            this.key = key;
            this.value = value;
            this.hash = hash;
        }
        @Override
        public int getHash() {
            return hash;
        }
        @Override public boolean isLeaf() {
            return true;
        }
        @Override public K getKey() {return key;}
        @Override public V getValue() {return value;}
        @Override public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Entry<K,V> getEntry(K key) {
            if (this.key.equals(key)) return this;
            return null;
        }
        @Override
        public Node<K> replace(int hashCode, int level, Node<K> entry, Node<K> oldEntry) {
            return entry;
        }
        @Override
        public Node<K> plus(int hashCode, int level, Node<K> newEntry) {
            if (hash == hashCode) {
                // hash collision
                return new CollisionNode(SetCreator.create(newEntry,this),hash);
            }

            int index = getLevelIndex(hashCode,level);
            int bitmap = 1<<index;
            hashCode = hash;
            int otherIndex = getLevelIndex(hashCode,level);
            bitmap |= 1<<otherIndex; 
            Node<K> ret = null;
            if (index<otherIndex) {
                ret = new BitmapNode(bitmap, (Entry) newEntry, this);
            } else {
                ret = new BitmapNode(bitmap, this, (Entry) newEntry);
            }
            return ret;
        }

        @Override
        public Node<K> merge(Node<K> otherRoot, int level) {
            if (this==otherRoot) return this;
            if (Hamt.getEntry(key, otherRoot, level)!=null) return otherRoot;
            return newSubTree(key, otherRoot, this, level);
        };
        @Override public String toString() {
            return key.toString()+":"+value.toString();
        }
    }
    private static class ArrayNode extends Node {
        protected final Node[] array;
        private ArrayNode(Node... array) {
            this.array = array;
        }
        @Override
        public boolean isFullNode() {
            return true;
        }
        @Override
        public Node getChild(int index) {
            return array[index];
        }
        @Override
        public Node replace(int hashCode, int level, Node newEntry, Node oldEntry) {
            if (newEntry==oldEntry) return this;
            int index = getLevelIndex(hashCode,level);
            Node[] newArray = array.clone();
            newArray[index] = newEntry;
            return new ArrayNode(newArray);
        }
        @Override
        public Node plus(int hashCode, int level, Node entry) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public Node merge(Node otherRoot, int level) {
            if (this==otherRoot) return this;
            if (otherRoot.isLeaf()) {
                Node[] newNodes = array.clone();
                int hash = otherRoot.getHash();
                int index = getLevelIndex(hash, level);
                newNodes[index] = array[index].merge(otherRoot, level+1);
                if (newNodes[index]==array[index]) return this;
                return new ArrayNode(newNodes);
            } else if (otherRoot instanceof ArrayNode) {
                ArrayNode other = (ArrayNode) otherRoot;
                return mergeAA(array, other.array, level+1, this);
            } else {
                BitmapNode other = (BitmapNode) otherRoot;
                return mergeBB(array, Integer.MAX_VALUE, other.array, other.bitmap, level+1, null);
            }
        }
        @Override public String toString() {
            return Arrays.toString(array);
        }
    }
    private static class BitmapNode extends Node {
        private final int bitmap;
        private final Node[] array;
        private BitmapNode(int bitmap, Node... array) {
            this.array = array;
            this.bitmap = bitmap;
        }
        @Override
        public Entry getChild(int index) {
            // get a number with 1 bit set for the index in the bitmap
            // the child exists iff bitmap&mapIndex!=0
            int mapIndex = 1 << index;
            if ((bitmap & mapIndex) == 0) return null;
            // the child index is the number of bits set left of that index
            // to get those bits we use mapIndex-1 as mask (all bits set to 
            // the right)
            int childIndex = Integer.bitCount(bitmap & (mapIndex-1));
            return (Entry) array[childIndex];
        }
        @Override
        public Node replace(int hashCode, int level, Node newEntry, Node oldEntry) {
            if (newEntry==oldEntry) return this;
            Node[] nodes = array.clone();
            for (int i=0; i<nodes.length; i++) {
                if (nodes[i]==oldEntry) {
                    nodes[i] = newEntry;
                    break;
                }
            }
            return new BitmapNode(bitmap, nodes);
        }
        @Override
        public Node plus(int hashCode, int level, Node entry) {
            int index = getLevelIndex(hashCode, level);
            int mapIndex = 1 << index;
            int childIndex = Integer.bitCount(bitmap & (mapIndex-1));
            int newBitmap = bitmap | mapIndex;
            int size = array.length+1;
            Node[] nodes = new Node[size];
            for (int i=0; i<childIndex; i++) {
                nodes[i] = array[i];
            }
            nodes[childIndex] = entry;
            for (int i=childIndex+1; i<size; i++) {
                nodes[i] = array[i-1];
            }
            if (size<32) {
                return new BitmapNode(newBitmap, nodes);
            } else {
                return new ArrayNode(nodes);
            }
        }
        @Override
        public Node merge(Node otherRoot, int level) {
            if (otherRoot.isLeaf()) {
                int hash = otherRoot.getHash();
                int index = getLevelIndex(hash, level);
                int mapIndex = 1 << index;
                if ((bitmap&mapIndex)!=0)  {
                    // entry exists
                    int childIndex = Integer.bitCount(bitmap & (mapIndex-1));
                    return replace(hash, level, otherRoot, array[childIndex]);
                } else {
                    return plus(hash, level, otherRoot);
                }
            } else {
                Node[] otherNodes;
                int otherBitmap;
                if (otherRoot instanceof ArrayNode) {
                    otherBitmap = Integer.MAX_VALUE;
                    otherNodes = ((ArrayNode) otherRoot).array;
                } else {
                    BitmapNode other = (BitmapNode) otherRoot;
                    otherNodes = other.array;
                    otherBitmap = other.bitmap;
                }
                return mergeBB(array,bitmap,otherNodes,otherBitmap,level+1,otherRoot);
            }
        }
        @Override public String toString() {
            return Arrays.toString(array);
        }
    }
    private static class CollisionNode<K,V> extends Node<K> {
        private final PSet<Entry<K,V>> list;
        private final int hash;
        private CollisionNode(PSet<Entry<K,V>> list, int hash) {
            this.list = list;
            this.hash = hash;
        }
        @Override
        public boolean isLeaf() {
            return true;
        }
        @Override
        public Entry<K,V> getEntry(K key) {
            for (Entry<K,V> l : list) {
                if (l.key.equals(key)) return l;
            }
            return null;
        }
        @Override
        public int getHash() {
            return hash;
        }
        @Override
        public Node<K> replace(int hashCode, int level, Node<K> newEntry, Node<K> oldEntry) {
            PSet newSet;
            if (newEntry instanceof CollisionNode) {
                CollisionNode cn = (CollisionNode) newEntry;
                newSet = list.minus(cn.list, equalEntryKeys).append(cn.list);
            } else {
                newSet = list.minus((Entry) oldEntry).append(
                            SetCreator.create((Entry<K,V>) newEntry));
            }
            return new CollisionNode(newSet,hash);
        }
        @Override
        public Node<K> plus(int hashCode, int level, Node<K> entry) {
            return new CollisionNode(list.append(
                    SetCreator.create((Entry<K,V>) entry)),
                    hash);
        }
        @Override
        public Node<K> merge(Node<K> otherRoot, int level) {
            if (otherRoot.isLeaf()) {
                return replace(0, level, otherRoot, null);
            } else if (otherRoot instanceof ArrayNode) {
                ArrayNode an = (ArrayNode) otherRoot;
                int index = getLevelIndex(hash, level);
                Node otherNode = an.array[index];
                return an.replace(hash, level, this.merge(otherNode, level+1), otherNode);
            } else {
                BitmapNode bn = (BitmapNode) otherRoot;
                int index = getLevelIndex(hash, level);
                Node target = bn.getChild(index);
                return bn.replace(hash, level, this.merge(target, level+1), target);
            }
        }
    }
    
    public static Hamt EMPTY = new Hamt.Empty();
    
    private final Node root;
    private Hamt(Node root){
        this.root = root;
    }
    
    private static int hash(Object key) {
        return key.hashCode();
    }
    private static int getLevelIndex(int hashCode, int level) {
        return (hashCode >>> (5*level)) & 0b11111;
    }
    
    public Entry<K,V> getEntry(K key) {
        return getEntry(key, root, 0);
    }
    
    private static <K,V> Entry<K,V> getEntry(K key, Node<K> root, int level) {
        int code = hash(key) >>> 5*level;
        Node current = root;
        while (current!=null) {
            if (current.isLeaf()) return current.getEntry(key);
            int index = code & 0b11111;
            code = code >>> 5;
            current = current.getChild(index);
        }
        return null;
    }
    
    private static <K,V> Node[] getPathForChanges(Node root, int hash, K key) {
        // first we find the path to the possible leaf
        Node[] path = new Node[8]; // more than that is not possible
        int pathIndex = -1;
        Node current = root;
        while (current!=null) {
            pathIndex++;
            path[pathIndex] = current;
            if (current.isLeaf()) {
                break;
            } else {
                int childIndex = hash & 0b11111;
                hash = hash >>> 5;
                current = current.getChild(childIndex);
            }
        }
        return path;
    }
    
    private static int getLastPathEntry(Node[] nodes) {
        for (int i=nodes.length-1; i>=0; i--) {
            if (nodes[i]!=null) return i;
        }
        throw new GroovyBugError("should not reach here");
    }
    
    public Hamt<K,V> plus(K key, V value) {
        Entry<K,V> newEntry = new Entry<>(key, value, hash(key));
        return new Hamt<>(newSubTree(key,root,newEntry,0));
    }

    private static <K> Node<K> newSubTree(K key, Node<K> root, Node<K> newNode, int level) {
        int hash = hash(key);
        // first we find the path to the possible leaf
        Node[] path = getPathForChanges(root,hash,key);
        // get last index of path 
        int lastPathEntry = getLastPathEntry(path);
        
        // we build up a new trie containing all the old nodes,
        // that are not changed plus new nodes for the path elements
        Node lastNode = path[lastPathEntry];
        boolean handled = false;
        Node newRoot = null;
        if (lastPathEntry>0) {
            Node parent = path[lastPathEntry-1];
            if (!parent.isFullNode()) {
                lastPathEntry--;
                lastNode = parent;
                handled = true;
                newRoot = lastNode.merge(newNode, lastPathEntry);
            }
        } 
        if (!handled) newRoot = lastNode.plus(hash, lastPathEntry, newNode);
        lastPathEntry--;

        Node lastElement = lastNode;
        for (int i=lastPathEntry; i>=0; i--) {
            Node n = path[i]; 
            newRoot = n.replace(hash, i+level, newRoot, lastElement);
            lastElement = n;
        }
        return newRoot;
    }

    public Hamt<K,V> merge(Hamt<K,V> other) {
        if (other==EMPTY) return this;
        if (this==EMPTY) return other;
        return new Hamt<K,V>(root.merge(other.root,0));
    }

    private static Node mergeAA(Node[] n1, Node[] n2, int nextLevel, ArrayNode original) {
        Node[] newNodes = new Node[32];
        for (int i=0; i<32; i++) {
            Node n1i = n1[i];
            Node n2i = n2[i];
            newNodes[i] = n1i.merge(n2i, nextLevel);
        }
        for (int i=0; i<32; i++) {
            if (n1[i]!=n2[i]) return new ArrayNode(newNodes);
        }
        return original;
    }

    private static Node mergeBB(Node[] n1, int bitmap1, Node[] n2, int bitmap2, int nextLevel, Node orig) {
        ArrayList<Node> list = new ArrayList<Node>(32);
        int indexN1 = 0, indexN2 = 0;
        for (int i=0; i<32; i++) {
            int bitIndex = 1 << i;
            boolean n1Hit = (bitmap1 & bitIndex) != 0;
            boolean n2Hit = (bitmap2 & bitIndex) != 0;
            if (n1Hit) {
                if (n2Hit) {
                    list.add(n1[indexN1].merge(n2[indexN2], nextLevel));
                    indexN1++; indexN2++;
                } else {
                    list.add(n1[indexN1]);
                    indexN1++;
                }
            } else if (n2Hit) {
                list.add(n2[indexN2]);
                indexN2++;
            }
        }
        Node[] newNodes = (Node[]) list.toArray(new Node[0]);
        if (list.size()==32) {
            return new ArrayNode(newNodes);
        } else {
            int newMap = bitmap1|bitmap2;
            if (orig!=null && bitmap2==newMap) {
                boolean match = true;
                for (int i=0; i<n2.length; i++) {
                    if (n2[i]!=newNodes[i]) {
                        match=false;
                        break;
                    }
                }
                if (match) return orig;
            }
            return new BitmapNode(newMap, newNodes);
        }
    }

    @Override
    public java.util.Iterator<V> iterator() {
        final Iterator<Entry<K, V>> it = entryIterator();
        return new Iterator<V>() {
            @Override
            public boolean hasNext() { return it.hasNext(); }
            @Override
            public V next() { return it.next().getValue();}
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public Iterator<Entry<K, V>> entryIterator() {
        final LinkedList<Node> stack = new LinkedList();
        stack.add(root);
        return new Iterator<Entry<K,V>>() {
            private Iterator<Entry<K,V>> it = null;
            private Entry<K,V> currentEntry = null;
            @Override
            public boolean hasNext() {
                if (it == null || !it.hasNext() || currentEntry==null) {
                    while (!stack.isEmpty()) {
                        // remove current root and replace it with its childs
                        Node nextRoot = stack.removeLast();
                        if (nextRoot.isLeaf()) {
                            if (nextRoot instanceof Entry) {
                                currentEntry = (Entry) nextRoot;
                                it = null;
                                return true;
                            } else {
                                currentEntry = null;
                                CollisionNode<K, V> cn = (CollisionNode) nextRoot;
                                it = cn.list.iterator();
                                if (it.hasNext()) return true;
                            }
                        } else {
                            Node[] nodes;
                            if (nextRoot instanceof BitmapNode) {
                                BitmapNode bn = (BitmapNode) nextRoot;
                                nodes = bn.array;
                            } else {
                                ArrayNode an = (ArrayNode) nextRoot;
                                nodes = an.array;
                            }
                            for (int i=nodes.length; i>=0; i--) {
                                stack.add(nodes[i]);
                            }
                        }
                    }
                    return false;
                }
                return true;
            }
            @Override
            public Entry<K, V> next() {
                if (it!=null) return it.next();
                Entry<K, V> n = currentEntry;
                currentEntry=null;
                return n;
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    
}
