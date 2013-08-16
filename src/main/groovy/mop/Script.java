/*
 * Copyright 2003-2007 the original author or authors.
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
package groovy.mop;

import org.codehaus.groovy.control.CompilationFailedException;

import groovy.mop.internal.DefaultRealm;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;

/**
 * This object represents a Groovy script
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @author Guillaume Laforge
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
@MOPMethods(setMissing="setProperty",getInterceptor="getProperty",methodMissing="invokeMethod")
public abstract class Script {

    private Binding binding;

    protected Script() {
        this(new Binding());
    }

    protected Script(Binding binding) {
        this.binding = binding;
    }

    public Binding getBinding() {
        return binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    @SuppressWarnings("unused")
    private Object getProperty(String property) {
        return binding.getVariable(property);
    }

    @SuppressWarnings("unused")
    private void setProperty(String property, Object newValue) {
        binding.setVariable(property, newValue);
    }

    /**
     * Invoke a method (or closure in the binding) defined.
     *
     * @param name method to call
     * @param args arguments to pass to the method
     * @return value
     */
    @SuppressWarnings("unused")
    private MethodHandle invokeMethod(String name, Object args) {
        // if the method was not found in the current scope (the script's methods)
        // let's try to see if there's a callable object with the same name in the binding
        if (binding.hasVariable(name)) {
            Object callObject = binding.getVariable(name);
            GroovyInvoker.getMethodHandle(callObject,"call");
        }
        return null;
    }

    /**
     * The main instance method of a script which has variables in scope
     * as defined by the current {@link Binding} instance.
     */
    public abstract Object run();

    // println helper methods

    /**
     * Prints a newline to the current 'out' variable which should be a PrintWriter
     * or at least have a println() method defined on it.
     * If there is no 'out' property then print to standard out.
     */
    public void println() {
        MethodHandle getter = DefaultRealm.getRoot().
                                getMetaClassInternal(Script.class).
                                selectGetter(this,"out");
        if (getter==null) {
            GroovyInvoker.invoke(System.out, "println");
        } else {
            GroovyInvoker.invoke(getter);
        }
    }

    /**
     * Prints the value to the current 'out' variable which should be a PrintWriter
     * or at least have a print() method defined on it.
     * If there is no 'out' property then print to standard out.
     */
    public void print(Object value) {
        MethodHandle getter = DefaultRealm.getRoot().
                                getMetaClassInternal(Script.class).
                                selectGetter(this,"out");
        if (getter==null) {
            GroovyInvoker.invoke(System.out, "print");
        } else {
            GroovyInvoker.invoke(getter);
        }
    }

    /**
     * Prints the value and a newline to the current 'out' variable which should be a PrintWriter
     * or at least have a println() method defined on it.
     * If there is no 'out' property then print to standard out.
     */
    public void println(Object value) {
        MethodHandle getter = DefaultRealm.getRoot().
                                getMetaClassInternal(Script.class).
                                selectGetter(this,"out");
        if (getter==null) {
            GroovyInvoker.invoke(System.out, "println", value);
        } else {
            GroovyInvoker.invoke(getter);
        }
    }

    /**
     * Prints a formatted string using the specified format string and argument.
     *
     * @param format the format to follow
     * @param value the value to be formatted
     */
    public void printf(String format, Object value) {
        MethodHandle getter = DefaultRealm.getRoot().
                                getMetaClassInternal(Script.class).
                                selectGetter(this,"out");
        if (getter==null) {
            GroovyInvoker.invoke(System.out, "printf", format, value);
        } else {
            GroovyInvoker.invoke(getter);
        }
    }

    /**
     * Prints a formatted string using the specified format string and arguments.
     *
     * @param format the format to follow
     * @param values an array of values to be formatted
     */
    public void printf(String format, Object[] values) {
        MethodHandle getter = DefaultRealm.getRoot().
                                getMetaClassInternal(Script.class).
                                selectGetter(this,"out");
        if (getter==null) {
            GroovyInvoker.invoke(System.out, "printf", format, values);
        } else {
            GroovyInvoker.invoke(getter);
        }
    }

    /**
     * A helper method to allow the dynamic evaluation of groovy expressions using this
     * scripts binding as the variable scope
     *
     * @param expression is the Groovy script expression to evaluate
     */
    public Object evaluate(String expression) throws CompilationFailedException {
        GroovyShell shell = new GroovyShell(getClass().getClassLoader(), binding);
        return shell.evaluate(expression);
    }

    /**
     * A helper method to allow the dynamic evaluation of groovy expressions using this
     * scripts binding as the variable scope
     *
     * @param file is the Groovy script to evaluate
     */
    public Object evaluate(File file) throws CompilationFailedException, IOException {
        GroovyShell shell = new GroovyShell(getClass().getClassLoader(), binding);
        return shell.evaluate(file);
    }

    /**
     * A helper method to allow scripts to be run taking command line arguments
     */
    public void run(File file, String[] arguments) throws CompilationFailedException, IOException {
        GroovyShell shell = new GroovyShell(getClass().getClassLoader(), binding);
        shell.run(file, arguments);
    }
}
