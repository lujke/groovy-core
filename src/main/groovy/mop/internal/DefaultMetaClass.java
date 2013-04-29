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

import groovy.mop.*;
import groovy.mop.internal.pcollection.PSet;

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
    
    // -------------  immutable values ----------
    private final Class<?> theClass;
    private final DefaultRealm realm;

    private NameVisibilityIndex<MetaProperty> properties = null;
    private NameVisibilityIndex<DefaultMetaMethod> methods = null;

    public DefaultMetaClass(DefaultRealm realm, Class<?> theClass) {
        this.theClass = theClass;
        this.realm = realm;
    }
    
    // -------------------------------------------------------------------
    //          Methods for interfacing with MetaClassHandle
    // -------------------------------------------------------------------

    public PSet<MetaProperty> getProperties(Class view, String name) {
        computeProperties();
        return properties.get(name);
    }

    public PSet<DefaultMetaMethod> getMethods(Class view, String name) {
        computeMethods();
        return methods.get(name);
    }
    
    public Class<?> getTheClass() {
        return theClass;
    }
    
    // -------------------------------------------------------------------

    public void computeProperties() {
        if (properties!=null) return;
        //properties = PropertyHelper.createIndex(theClass, realm);
    }
    
    public NameVisibilityIndex<DefaultMetaMethod> computeMethods() {
        if (methods!=null) return methods;
        methods = MethodHelper.createIndex(this);
        return methods;
    }

    public DefaultRealm getRealm() {
        return realm;
    }

    public NameVisibilityIndex<DefaultMetaMethod> getExtensions() {
        // TODO Auto-generated method stub
        return null;
    }

    public DefaultMetaMethod selectMethod(Class c, Object receiver, String name, Object... args) {
        // TODO Auto-generated method stub
        return null;
    }

    /*

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
    
    private MetaIndex<DefaultMetaMethod> getMetaMethodIndex() {
        computeMethods();
        return methods;
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

