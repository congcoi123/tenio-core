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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For NotDefinedSubscribersException")
class NotDefinedSubscribersExceptionTest {

  @Test
  @DisplayName("Constructor should create exception with interfaces message")
  void constructorShouldCreateExceptionWithInterfacesMessage() {
    // Create test interfaces
    interface TestEventInterface1 {}
    interface TestEventInterface2 {}
    
    // Create the exception with the interfaces
    var exception = new NotDefinedSubscribersException(
        TestEventInterface1.class, TestEventInterface2.class);
    
    // Verify the exception and its message
    assertNotNull(exception);
    assertEquals(
        String.format("Need to implement interfaces: %s, %s", 
            TestEventInterface1.class.getName(), TestEventInterface2.class.getName()), 
        exception.getMessage());
  }
  
  @Test
  @DisplayName("Constructor should create exception with single interface message")
  void constructorShouldCreateExceptionWithSingleInterfaceMessage() {
    // Create test interface
    interface TestEventInterface {}
    
    // Create the exception with the interface
    var exception = new NotDefinedSubscribersException(TestEventInterface.class);
    
    // Verify the exception and its message
    assertNotNull(exception);
    assertEquals(
        String.format("Need to implement interfaces: %s", 
            TestEventInterface.class.getName()), 
        exception.getMessage());
  }
} 