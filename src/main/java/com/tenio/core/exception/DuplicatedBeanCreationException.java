/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.exception;

import com.tenio.core.bootstrap.annotation.AutowiredQualifier;
import com.tenio.core.bootstrap.annotation.Bean;
import java.io.Serial;

/**
 * Exception thrown when attempting to create a duplicate bean in the dependency injection system.
 * This exception indicates that a bean with the same class type and qualifier name already exists
 * in the container.
 * 
 * <p>This exception can occur in several scenarios:
 * <ul>
 * <li>Multiple {@code @Bean} methods producing the same type with same qualifier</li>
 * <li>Multiple components with same type and {@code @AutowiredQualifier}</li>
 * <li>Attempting to register a bean that was already registered</li>
 * </ul>
 * 
 * <p>To resolve this exception:
 * <ul>
 * <li>Use unique qualifier names for beans of the same type</li>
 * <li>Ensure bean definitions are not duplicated across configuration classes</li>
 * <li>Check for conflicting component scanning configurations</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * // This will throw DuplicatedBeanCreationException
 * &#64;Configuration
 * public class AppConfig {
 *     &#64;Bean("userService")
 *     public UserService userService1() {
 *         return new UserServiceImpl();
 *     }
 *     
 *     &#64;Bean("userService")  // Duplicate name!
 *     public UserService userService2() {
 *         return new UserServiceImpl();
 *     }
 * }
 * </pre>
 * 
 * @see Bean
 * @see AutowiredQualifier
 * @since 0.5.0
 */
public final class DuplicatedBeanCreationException extends Exception {

  @Serial
  private static final long serialVersionUID = -2167364908295120478L;

  /**
   * Creates a new DuplicatedBeanCreationException with details about the duplicate bean.
   * The exception message includes both the bean's class type and qualifier name to help
   * identify the source of the duplication.
   *
   * @param clazz the bean's {@link Class} type that was duplicated
   * @param name  the bean's qualifier name that was duplicated
   */
  public DuplicatedBeanCreationException(Class<?> clazz, String name) {
    super(String.format("Duplicated bean creation with type: %s, and name: %s",
        clazz.getSimpleName(), name));
  }
}
