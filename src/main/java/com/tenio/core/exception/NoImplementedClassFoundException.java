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

import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.bootstrap.annotation.Autowired;

/**
 * Exception thrown when the dependency injection system cannot find an implementation for an interface
 * or abstract class that is required for injection.
 * 
 * <p>This exception typically occurs when:
 * <ul>
 * <li>An {@code @Autowired} field requires an implementation that doesn't exist</li>
 * <li>No class annotated with {@code @Component} implements the required interface</li>
 * <li>The implementation class exists but is not in the component scan path</li>
 * <li>The implementation class is missing the required annotation</li>
 * </ul>
 * 
 * <p>To resolve this exception:
 * <ul>
 * <li>Ensure the implementation class is properly annotated with {@code @Component}</li>
 * <li>Verify the implementation class is in a package that is component scanned</li>
 * <li>Check that the implementation class properly implements the required interface</li>
 * <li>Consider using {@code @Qualifier} if multiple implementations exist</li>
 * </ul>
 * 
 * <p>Example scenario:
 * <pre>
 * // Interface
 * public interface UserService {
 *     void createUser(String username);
 * }
 * 
 * // Missing @Component annotation!
 * public class UserServiceImpl implements UserService {
 *     public void createUser(String username) { ... }
 * }
 * 
 * // This will throw NoImplementedClassFoundException
 * public class UserController {
 *     &#64;Autowired
 *     private UserService userService;  // No implementation found!
 * }
 * </pre>
 * 
 * @see Component
 * @see Autowired
 */
public final class NoImplementedClassFoundException extends RuntimeException {

  private static final long serialVersionUID = -2836756456705984458L;

  /**
   * Creates a new NoImplementedClassFoundException with details about the missing implementation.
   * The exception message includes the interface or abstract class that lacks an implementation
   * to help identify what needs to be fixed.
   *
   * @param clazz the interface or abstract class that has no implementation in the container
   * 
   * @see Component
   * @see Autowired
   */
  public NoImplementedClassFoundException(Class<?> clazz) {
    super(String.format("Unable to find any implementation for the class: %s", clazz.getName()));
  }
}
