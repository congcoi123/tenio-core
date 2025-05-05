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
 * Exception thrown when multiple implementations of an interface or abstract class are found
 * during dependency injection, and no qualifier is specified to disambiguate which implementation
 * should be used.
 * 
 * <p>This exception typically occurs when:
 * <ul>
 * <li>Multiple classes annotated with {@code @Component} implement the same interface</li>
 * <li>An {@code @Autowired} field has multiple candidate implementations</li>
 * <li>No qualifier is specified to select a specific implementation</li>
 * <li>The bean name doesn't match any implementation</li>
 * </ul>
 * 
 * <p>To resolve this exception:
 * <ul>
 * <li>Add a qualifier to specify which implementation to inject</li>
 * <li>Use unique bean names for each implementation</li>
 * <li>Make one implementation primary</li>
 * <li>Remove duplicate implementations if they're not needed</li>
 * </ul>
 * 
 * <p>Example scenario:
 * <pre>
 * // Interface
 * public interface PaymentService {
 *     void processPayment(double amount);
 * }
 * 
 * // First implementation
 * &#64;Component
 * public class CreditCardPaymentService implements PaymentService {
 *     public void processPayment(double amount) { ... }
 * }
 * 
 * // Second implementation
 * &#64;Component
 * public class PayPalPaymentService implements PaymentService {
 *     public void processPayment(double amount) { ... }
 * }
 * 
 * // This will throw MultipleImplementedClassForInterfaceException
 * public class PaymentController {
 *     &#64;Autowired
 *     private PaymentService paymentService;  // Which implementation to use?
 * }
 * 
 * // Fix by adding qualifier
 * public class PaymentController {
 *     &#64;Autowired("creditCard")
 *     private PaymentService paymentService;  // Now uses CreditCardPaymentService
 * }
 * </pre>
 * 
 * @see Component
 * @see Autowired
 */
public final class MultipleImplementedClassForInterfaceException extends RuntimeException {

  private static final long serialVersionUID = 9186053637398483773L;

  /**
   * Creates a new MultipleImplementedClassForInterfaceException with details about the ambiguous
   * interface or abstract class that has multiple implementations.
   * 
   * <p>The exception message includes the interface or abstract class name to help identify
   * which type has conflicting implementations in the container.
   *
   * @param clazz the interface or abstract class that has multiple implementations
   * 
   * @see Component
   * @see Autowired
   */
  public MultipleImplementedClassForInterfaceException(Class<?> clazz) {
    super(String.format("Multiple implementations for the class: %s found", clazz.getName()));
  }
}
