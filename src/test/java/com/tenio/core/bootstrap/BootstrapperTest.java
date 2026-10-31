/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tenio.core.bootstrap.injector.Injector;
import com.tenio.core.event.handler.EventHandler;
import com.tenio.core.exception.NoImplementedClassFoundException;
import com.tenio.core.fixture.bootstrap.BootstrapApplication;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For Bootstrapper")
class BootstrapperTest {

  private static final String EVENT_HANDLERS = "com.tenio.core.event.handler";

  private final Bootstrapper bootstrapper = Bootstrapper.newInstance();
  private final Injector injector = Injector.newInstance();

  private static class NotBootstrap {
  }

  @BeforeEach
  @AfterEach
  void resetBootstrapState() throws Exception {
    // Reset the handler, profile selection and bean registrations together.
    bootstrapper.run(NotBootstrap.class);
  }

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
  @DisplayName("run() with a class not annotated with @Bootstrap should not initialize handler")
  void runWithNonBootstrapClassShouldNotInitHandler() {
    assertDoesNotThrow(() -> bootstrapper.run(NotBootstrap.class));
    assertNull(bootstrapper.getBootstrapHandler());
    assertTrue(injector.getClassBeansMap().isEmpty());
  }

  @Test
  @DisplayName("run() should initialize a fully wired handler from an isolated application")
  void runWithBootstrapClassShouldInitHandler() {
    assertDoesNotThrow(() -> bootstrapper.run(BootstrapApplication.class, EVENT_HANDLERS));

    var handler = bootstrapper.getBootstrapHandler();
    assertNotNull(handler);
    assertAll(
        () -> assertNotNull(handler.getEventHandler()),
        () -> assertNotNull(handler.getConfigurationHandler()),
        () -> assertSame(injector.getBean(EventHandler.class), handler.getEventHandler()),
        () -> assertSame(injector.getSystemCommandManager(), handler.getSystemCommandManager()),
        () -> assertSame(injector.getClientCommandManager(), handler.getClientCommandManager()),
        () -> assertSame(injector.getServletBeansMap(), handler.getServletMap()),
        () -> assertSame(handler.getEventHandler(), handler.getBeanByClazz(EventHandler.class)),
        () -> assertEquals(Set.of(), handler.getActiveProfiles()));
  }

  @Test
  @DisplayName("getBootstrapHandler() before run() should return null")
  void getBootstrapHandlerBeforeRunShouldReturnNull() {
    assertNull(Bootstrapper.newInstance().getBootstrapHandler());
  }

  @Test
  @DisplayName("run() should propagate missing dependencies instead of exposing a partial handler")
  void runShouldPropagateMissingDependency() throws Exception {
    bootstrapper.run(BootstrapApplication.class, EVENT_HANDLERS);

    // Omitting the event package deliberately removes the required EventHandler provider.
    var exception = assertThrows(NoImplementedClassFoundException.class,
        () -> bootstrapper.run(BootstrapApplication.class));

    assertTrue(exception.getMessage().contains(EventHandler.class.getName()));
    assertNull(bootstrapper.getBootstrapHandler());
  }

  @Test
  @DisplayName("Matching profiles should start the entry point and retain an immutable selection")
  void runWithProfilesShouldSelectEntryPointAndDetachProfiles() throws Exception {
    var profiles = new HashSet<>(Set.of(" world "));
    bootstrapper.runWithProfiles(BootstrapApplication.WorldApplication.class, profiles, EVENT_HANDLERS);
    profiles.add("game");

    var handler = bootstrapper.getBootstrapHandler();
    assertNotNull(handler);
    assertNotNull(handler.getEventHandler());
    assertEquals(Set.of("world"), handler.getActiveProfiles());
    assertThrows(UnsupportedOperationException.class, () -> handler.getActiveProfiles().add("game"));
  }

  @Test
  @DisplayName("An excluded entry point should clear the prior bootstrap generation")
  void runWithExcludedProfileShouldClearPreviousBeans() throws Exception {
    bootstrapper.runWithProfiles(BootstrapApplication.WorldApplication.class, Set.of("world"), EVENT_HANDLERS);
    assertNotNull(injector.getBean(BootstrapHandler.class));
    assertNotNull(injector.getBean(EventHandler.class));

    bootstrapper.runWithProfiles(BootstrapApplication.WorldApplication.class, Set.of("game"), EVENT_HANDLERS);

    assertAll(
        () -> assertNull(bootstrapper.getBootstrapHandler()),
        () -> assertNull(injector.getBean(BootstrapHandler.class)),
        () -> assertNull(injector.getBean(EventHandler.class)),
        () -> assertTrue(injector.getClassesMap().isEmpty()),
        () -> assertTrue(injector.getClassBeansMap().isEmpty()),
        () -> assertTrue(injector.getServletBeansMap().isEmpty()));
  }

  @Test
  @DisplayName("A non-bootstrap entry point should clear the prior handler and bean registrations")
  void nonBootstrapEntryShouldClearPreviousBeans() throws Exception {
    bootstrapper.run(BootstrapApplication.class, EVENT_HANDLERS);
    assertNotNull(injector.getBean(BootstrapHandler.class));

    bootstrapper.run(NotBootstrap.class);

    assertNull(bootstrapper.getBootstrapHandler());
    assertNull(injector.getBean(BootstrapHandler.class));
    assertNull(injector.getBean(EventHandler.class));
    assertTrue(injector.getClassBeansMap().isEmpty());
  }

  @Test
  @DisplayName("Legacy run() should reset an earlier selection and construct a fresh generation")
  void runShouldResetProfilesAndCachedHandler() throws Exception {
    bootstrapper.runWithProfiles(BootstrapApplication.class, Set.of("world"), EVENT_HANDLERS);
    var previousHandler = bootstrapper.getBootstrapHandler();
    assertSame(previousHandler, injector.getBean(BootstrapHandler.class));

    bootstrapper.run(BootstrapApplication.class, EVENT_HANDLERS);

    var handler = bootstrapper.getBootstrapHandler();
    assertNotSame(previousHandler, handler);
    assertNotSame(previousHandler.getEventHandler(), handler.getEventHandler());
    assertSame(handler, injector.getBean(BootstrapHandler.class));
    assertEquals(Set.of(), handler.getActiveProfiles());
  }
}
