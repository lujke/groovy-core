/*
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
package groovy.mop.internal;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.*;
import java.util.*;

import groovy.mop.*;
import groovy.mop.internal.pcollection.PSet;

import org.codehaus.groovy.runtime.ExceptionUtils;

import static groovy.mop.internal.MetaClassHelper.*;

/**
 * This is the implementation of a meta class for Groovy according to
 * MetaObjectProtocol. This class is implemented in the style of an 
 * persistent collection.
 *   
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 * @see MetaClass
 */
public class DefaultMetaClass {
    private final static Lookup LOOKUP = MethodHandles.lookup();
    //TODO: set correct value
    private final static MethodHandle METAMETHOD_ISASSIGNABLE = null;//SignatureHelper#canBeCalledWithTypes
    
    private final Class<?> theClass;
    private final DefaultRealm domain;
    private final DefaultMetaClass[] parents;
    private MetaIndex<MetaProperty> properties = null;
    private MetaIndex<DefaultMetaMethod> methods = null;

    public DefaultMetaClass(DefaultRealm domain, Class<?> theClass) {
        this.theClass = theClass;
        this.domain = domain;
        parents = createParents(this.domain, theClass);
    }

    private static DefaultMetaClass[] createParents(DefaultRealm domain, Class<?> theClass) {
        DefaultMetaClass superClass = domain.getMetaClassInternal(theClass.getSuperclass());
        Class[] interfaces = theClass.getInterfaces();
        DefaultMetaClass[] mint = new DefaultMetaClass[interfaces.length+1];
        mint[interfaces.length] = superClass;
        for (int i=0; i<interfaces.length; i++) {
            mint[i] = domain.getMetaClassInternal(interfaces[i]);
        }
        return mint;
    }
    
    // -------------------------------------------------------------------
    //          Methods for interfacing with MetaClassHandle
    // -------------------------------------------------------------------

    public PSet<MetaProperty> getProperties(Class view, String name) {
        if (properties==null) computeProperties();
        return properties.get(name);
    }

    public PSet<DefaultMetaMethod> getMethods(Class view, String name) {
        if (methods==null) computeMethods();
        return methods.get(name);
    }
    
    public Class<?> getTheClass() {
        return theClass;
    }
    
    // -------------------------------------------------------------------

    private void computeProperties() {
        // TODO Auto-generated method stub
    }

    private void computeMethods() {
        // TODO Auto-generated method stub
        
    }

    /*
    @Override
    public Collection<? extends MetaProperty> getMetaProperties() {
        if (properties==null) computeProperties();
        return properties.getPublicAndPrivate();
    }

    @Override
    public MetaProperty getMetaProperty(String name) {
        if (properties==null) computeProperties();
        return properties.getSingle(name);
    }

    @Override
    public Collection<? extends MetaMethod> getMetaMethods() {
        if (methods==null) computeMethods();
        return methods.getPublicAndPrivate();
    }
    
    private List<DefaultMetaMethod> getMetaMethods(String name) {
        if (methods==null) computeMethods();
        return methods.getList(name);
    }

    @Override
    public List<? extends MetaMethod> getMetaMethods(String name, Class... argumentTypes) {
        List<DefaultMetaMethod> list = getMetaMethods(name);
        SerialRemoveList<DefaultMetaMethod> res = new SerialRemoveList<>(list);
        res = res.minus(MethodHandles.insertArguments(METAMETHOD_ISASSIGNABLE,0, (Object[]) argumentTypes));
        return res;
    }

    @Override
    public List<? extends MetaMethod> respondsTo(String name, Object... arguments) {
        Class[] types = convertToTypeArray(arguments);
        return getMetaMethods(name, types);
    }

    public Class<?> getTheClass() {
        return theClass;
    }
    
    private MetaIndex<DefaultMetaMethod> getMetaMethodIndex() {
        computeMethods();
        return methods;
    }

    private void computeMethods() {
        MetaIndex<DefaultMetaMethod> newIndex = getParentPublicMethods();
        // add public and private methods into lists
        Method[] declaredMethods = theClass.getDeclaredMethods();
        if (declaredMethods.length != 0) {
            // we to fill a map that goes by name and we want to gather all 
            // the methods before adding them, since MetaMethodView will have to 
            // handle inheritance
            // (1)  sort by name
            Arrays.sort(declaredMethods, new Comparator<Method>() {
                @Override
                public int compare(Method o1, Method o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            // (2)  for each name make list of public and private methods
            //      and add the result to our map
            String lastName = null;
            List<DefaultMetaMethod> privateMethods = null;
            List<DefaultMetaMethod> publicMethods = null;
            
            for (Method m : declaredMethods) {
                // TODO: add mop renaming
                DefaultMetaMethod dmm = new DefaultMetaMethod(theClass, m.getName(), m.getModifiers(), unreflect(m));
    
                if (lastName==null) {
                    privateMethods = new LinkedList<>();
                    publicMethods = new LinkedList<>();
                } else if (!m.getName().equals(lastName)) {
                    publicMethods = mergeMethods(publicMethods, getParentPublicMethods(lastName));
                    newIndex = newIndex.putLeaf(lastName, privateMethods, publicMethods);
                }
    
                lastName = m.getName();
                if (Modifier.isPrivate(m.getModifiers())) {
                    privateMethods.add(dmm);
                } else {
                    publicMethods.add(dmm);
                }
            }
            methods = newIndex;
        }
    }

    private static MethodHandle unreflect(Method m) {
        try {
            return LOOKUP.unreflect(m);
        } catch (IllegalAccessException e) {
            ExceptionUtils.sneakyThrow(e);
        }
        return null;
    }

    private static List<DefaultMetaMethod> mergeMethods(List<DefaultMetaMethod> listWithOverrides, List<DefaultMetaMethod> origin) {
        // name is same
        if (listWithOverrides.size()==0) return origin;
        if (origin.size()==0) return listWithOverrides;
        List<DefaultMetaMethod> skipList = new ArrayList(origin);
        for (ListIterator<DefaultMetaMethod> iter = skipList.listIterator(); iter.hasNext(); ) {
            DefaultMetaMethod oldMM = iter.next();
            for (DefaultMetaMethod newMM : listWithOverrides) {
                if (oldMM.getTarget().type().equals(newMM.getTarget().type())) iter.remove();
            }
        }
        //TODO: use persistent list here
        skipList.addAll(listWithOverrides);
        return skipList;
    }

    private MetaIndex<DefaultMetaMethod> getParentPublicMethods() {
        if (parents.length==0) return MetaIndex.EMPTY;
        return parents[0].getMetaMethodIndex().getPublic();
    }

    private List<DefaultMetaMethod> getParentPublicMethods(String name) {
        return getParentPublicMethods().getList(name);
    }

    private void setCallTarget(MOPCall call, String name, Class... types) {
        List<DefaultMetaMethod> methods = getMetaMethods(name);
        if (methods.size() == 1) {
            DefaultMetaMethod mm = methods.get(0);
            if (!SignatureHelper.canBeCalledWithTypes(mm, types)) return;
            transformHandleForTypes(call, mm, types); 
        } else {
            getCallTargetWithDistanceCalculator(call, methods, name, types);
        }
    }

    private void transformHandleForTypes(MOPCall call, DefaultMetaMethod mm, Class[] types)
    {
        // TODO Auto-generated method stub
        
    }

    private void getCallTargetWithDistanceCalculator(MOPCall call, List<DefaultMetaMethod> methods, String name, Class[] types) {
        long savedDistance = -1;
        DefaultMetaMethod ret = null;
        LinkedList<DefaultMetaMethod> errorList = null;
        for (DefaultMetaMethod mm : methods) {
            long distance = calculateParameterDistance(types, mm.getParameterClasses()); 
            if (distance==-1 || distance>savedDistance) continue;
            if (distance==0) {
                transformHandleForTypes(call, ret, types);
                return;
            }
            if (distance<savedDistance) {
                errorList = null;
                savedDistance = distance;
                ret = mm;
                continue;
            } 
            //distance==savedDistance
            if (errorList==null) {
                errorList = new LinkedList<>();
                errorList.add(ret);
            }
            errorList.add(mm);
        }
        if (errorList!=null) {
            call.errorList = errorList;
        } else {
            transformHandleForTypes(call, ret, types);
        }
    }*/

}

