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

package com.tenio.core.bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tenio.core.bootstrap.annotation.Bootstrap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For Bootstrapper")
class BootstrapperTest {

  private final Bootstrapper bootstrapper = Bootstrapper.newInstance();

  @Test
  @DisplayName("Throw an exception when the class's instance is attempted creating again")
  void createNewInstanceShouldThrowException() throws NoSuchMethodException {
    var constructor = Bootstrapper.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    assertThrows(InvocationTargetException.class, () -> {
      constructor.setAccessible(true);
      constructor.newInstance();
    });
  }

  @Test
  @DisplayName("Run should return true when class has @Bootstrap annotation")
  void runShouldReturnTrueWithBootstrapAnnotation() throws Exception {
    @Bootstrap
    class TestBootstrapClass {}
    
    assertTrue(bootstrapper.run(TestBootstrapClass.class));
  }

  @Test
  @DisplayName("Run should return false when class lacks @Bootstrap annotation")
  void runShouldReturnFalseWithoutBootstrapAnnotation() throws Exception {
    class TestNonBootstrapClass {}
    
    assertFalse(bootstrapper.run(TestNonBootstrapClass.class));
  }

  @Test
  @DisplayName("Get bootstrap handler should return null before run")
  void getBootstrapHandlerShouldReturnNullBeforeRun() {
    assertNull(bootstrapper.getBootstrapHandler());
  }

  @Test
  @DisplayName("Get bootstrap handler should return instance after successful run")
  void getBootstrapHandlerShouldReturnInstanceAfterRun() throws Exception {
    @Bootstrap
    class TestBootstrapClass {}
    
    bootstrapper.run(TestBootstrapClass.class);
    assertNotNull(bootstrapper.getBootstrapHandler());
  }

  @Test
  @DisplayName("Run should handle errors during initialization")
  void runShouldHandleInitializationErrors() throws Exception {
    @Bootstrap
    class TestErrorClass {
      public TestErrorClass() {
        throw new RuntimeException("Test initialization error");
      }
    }
    
    assertThrows(Exception.class, () -> bootstrapper.run(TestErrorClass.class));
  }
}
