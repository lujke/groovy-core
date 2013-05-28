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
package mop;

import static org.junit.Assert.*;

import org.junit.Test;

import groovy.mop.Closure;
import groovy.mop.GroovyInvoker;

public class GroovyInvokerTest {

    @Test
    public void testSimpleInvocation() {
        String str = "str";
        Object[] args = new Object[0];
        Object res = GroovyInvoker.invoke(str, "toString", args);
        assertEquals(str,res);
    }
    
    @Test
    public void testMOPAddedMethod() {
        GroovyInvoker.setMethod(String.class, new Closure() {
            public String call(){return "1";}
        });
        Object res = GroovyInvoker.invoke("str", "toString");
        assertEquals("1",res);
    }

}
