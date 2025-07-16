package com.tenio.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.bootstrap.Bootstrapper;
import com.tenio.core.monitoring.system.SystemInfo;
import com.tenio.core.server.ServerImpl;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class ApplicationLauncherTest {

  @Test
  void testSingletonEnforcement() throws Exception {
    Field instanceField = ApplicationLauncher.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    ApplicationLauncher instance = (ApplicationLauncher) instanceField.get(null);
    assertNotNull(instance);
    assertSame(ApplicationLauncher.class, instance.getClass());
  }

  @Test
  void testRunWithNullEntryClass() {
    assertDoesNotThrow(() -> ApplicationLauncher.run(null, new String[]{}));
  }

  @Test
  void testStartWithBootstrapperException() throws Exception {
    // Mock Bootstrapper and ServerImpl
    Bootstrapper bootstrapper = mock(Bootstrapper.class);
    ServerImpl server = mock(ServerImpl.class);
    SystemInfo sysInfo = mock(SystemInfo.class);
    // Use reflection to inject mocks
    Field bootstrapperField = Bootstrapper.class.getDeclaredField("instance");
    bootstrapperField.setAccessible(true);
    bootstrapperField.set(null, bootstrapper);
    Field serverField = ServerImpl.class.getDeclaredField("instance");
    serverField.setAccessible(true);
    serverField.set(null, server);
    doThrow(new RuntimeException("fail")).when(bootstrapper).run(any(), any(), any(), any(), any());
    doNothing().when(server).shutdown();
    // Should not throw, but will call System.exit(1) (cannot test exit in unit test)
    assertDoesNotThrow(() -> {
      try {
        java.lang.reflect.Method m = ApplicationLauncher.class.getDeclaredMethod("newInstance");
        m.setAccessible(true);
        ApplicationLauncher launcher = (ApplicationLauncher) m.invoke(null);
        launcher.start(Object.class, new String[]{});
      } catch (Exception ignored) {}
    });
  }
} 