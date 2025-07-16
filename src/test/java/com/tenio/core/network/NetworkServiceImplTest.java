package com.tenio.core.network;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.jetty.JettyHttpService;
import com.tenio.core.network.kcp.KcpService;
import com.tenio.core.network.netty.NettyWebSocketService;
import com.tenio.core.network.zero.ZeroSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class NetworkServiceImplTest {

  private NetworkServiceImpl service;
  private JettyHttpService jettyService;
  private KcpService kcpService;
  private NettyWebSocketService nettyService;
  private ZeroSocketService zeroService;

  @BeforeEach
  void setUp() throws Exception {
    EventManager eventManager = EventManager.newInstance();
    service = (NetworkServiceImpl) NetworkServiceImpl.newInstance(eventManager);
    jettyService = mock(JettyHttpService.class);
    kcpService = mock(KcpService.class);
    nettyService = mock(NettyWebSocketService.class);
    zeroService = mock(ZeroSocketService.class);
    // Inject mocks into private final fields
    Field f1 = NetworkServiceImpl.class.getDeclaredField("httpService");
    f1.setAccessible(true); f1.set(service, jettyService);
    Field f2 = NetworkServiceImpl.class.getDeclaredField("webSocketService");
    f2.setAccessible(true); f2.set(service, nettyService);
    Field f3 = NetworkServiceImpl.class.getDeclaredField("kcpChannelService");
    f3.setAccessible(true); f3.set(service, kcpService);
    Field f4 = NetworkServiceImpl.class.getDeclaredField("socketService");
    f4.setAccessible(true); f4.set(service, zeroService);
  }

  @Test
  void testStartAndShutdown() {
    assertDoesNotThrow(() -> service.start());
    assertDoesNotThrow(() -> service.shutdown());
  }
} 