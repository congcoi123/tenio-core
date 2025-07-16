package com.tenio.core.network.jetty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.event.implement.EventManager;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;

class JettyHttpServiceTest {

  private JettyHttpService service;

  @BeforeEach
  void setUp() {
    EventManager eventManager = EventManager.newInstance();
    service = JettyHttpService.newInstance(eventManager);
    service.setThreadPoolSize(16); // valid max threads
    service.setPort(8080); // valid port
    service.setServletMap(Collections.singletonMap("test", mock(HttpServlet.class)));
  }

  @Test
  void testStartAndShutdown() {
    assertDoesNotThrow(() -> service.initialize());
    assertDoesNotThrow(() -> service.start());
    assertDoesNotThrow(() -> service.shutdown());
  }
} 