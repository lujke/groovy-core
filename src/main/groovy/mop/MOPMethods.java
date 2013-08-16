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
package groovy.mop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interface for interaction with the Groovy MOP to define which methods
 * would be used for in-class logic. The suffix <i>Interceptor</i> is used for
 * calls done before the selection process, the suffix <i>Missing</i> for after
 * a failed selection process. The prefix <i>method</i> is for method call
 * logic, <i>set</i> for property setting logic and <i>get</i> for property
 * getting logic.
 *  
 * @author <a href="mailto:blackdrag@gmx.org">Jochen "blackdrag" Theodorou</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MOPMethods {
    //TODO: explain supported signatures
    String methodMissing()      default "";
    String methodInterceptor()  default "";
    String getInterceptor()     default "";
    String setInterceptor()     default "";
    String getMissing()         default "";
    String setMissing()         default "";
    
    public static enum Return{NO_VALUE}
}
