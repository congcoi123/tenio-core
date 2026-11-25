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

package com.tenio.core.network.netty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tenio.common.data.DataCollection;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NettyWebSocketIntegrationTest {

  private NettyWebSocket service(int port) {
    var service = NettyWebSocketImpl.newInstance(mock(EventManager.class));
    service.setWebSocketConfiguration(new SocketConfiguration("websocket",
        TransportType.WEB_SOCKET, port, 0));
    service.setServerAddress("127.0.0.1");
    service.setProducerWorkerSize(1);
    service.setConsumerWorkerSize(1);
    service.setSessionManager(mock(SessionManager.class));
    service.setPacketDecoder(mock(BinaryPacketDecoder.class));
    service.setConnectionFilter(mock(ConnectionFilter.class));
    service.setNetworkReaderStatistic(mock(NetworkReaderStatistic.class));
    service.initialize();
    return service;
  }

  @SuppressWarnings("unchecked")
  private List<Channel> listeners(NettyWebSocket service) throws Exception {
    var field = NettyWebSocketImpl.class.getDeclaredField("serverWebSockets");
    field.setAccessible(true);
    return (List<Channel>) field.get(service);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testRealJdkClientFragmentationPingAndClose(boolean tls) throws Exception {
    var oldPath = System.getProperty("keystore.file.path");
    var oldPassword = System.getProperty("keystore.file.password");
    var keyPath = Path.of(getClass().getResource("/websocket/localhost-test.jks").toURI());
    System.setProperty("keystore.file.path", keyPath.toString());
    System.setProperty("keystore.file.password", "changeit");
    var service = service(0);
    var session = mock(Session.class);
    var sessions = mock(SessionManager.class);
    var decoder = mock(BinaryPacketDecoder.class);
    var message = mock(DataCollection.class);
    var delivered = new CompletableFuture<Void>();
    when(sessions.getSessionByWebSocket(any())).thenReturn(session);
    when(session.isActivated()).thenReturn(true);
    when(session.isAssociatedToPlayer(Session.AssociatedState.DONE)).thenReturn(true);
    when(decoder.decode(any(byte[].class))).thenReturn(message);
    doAnswer(invocation -> {
      delivered.complete(null);
      return null;
    }).when(session).enqueueInbound(message);
    service.setSessionManager(sessions);
    service.setPacketDecoder(decoder);
    service.setUsingSsl(tls);
    try {
      service.start();
      service.activate();
      service.activate();
      assertEquals(1, listeners(service).size());
      var address = (InetSocketAddress) listeners(service).getFirst().localAddress();
      assertEquals("127.0.0.1", address.getAddress().getHostAddress());
      var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5));
      if (tls) {
        var keys = KeyStore.getInstance("JKS");
        try (var stream = java.nio.file.Files.newInputStream(keyPath)) {
          keys.load(stream, "changeit".toCharArray());
        }
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(keys);
        var context = SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        builder.sslContext(context);
      }
      try (var client = builder.build()) {
        if (tls) {
          var wrongHost = URI.create("wss://127.0.0.1:" + address.getPort() + "/ws");
          assertThrows(java.util.concurrent.ExecutionException.class,
              () -> client.newWebSocketBuilder().buildAsync(wrongHost, new WebSocket.Listener() {})
                  .get(5, TimeUnit.SECONDS));
          try (var untrusted = HttpClient.newHttpClient()) {
            var uri = URI.create("wss://localhost:" + address.getPort() + "/ws");
            assertThrows(java.util.concurrent.ExecutionException.class,
                () -> untrusted.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {})
                    .get(5, TimeUnit.SECONDS));
          }
        }
        var pong = new CompletableFuture<ByteBuffer>();
        var close = new CompletableFuture<Integer>();
        var listener = new WebSocket.Listener() {
          @Override
          public void onOpen(WebSocket socket) {
            socket.request(1);
          }

          @Override
          public CompletionStage<?> onPong(WebSocket socket, ByteBuffer data) {
            var copy = ByteBuffer.allocate(data.remaining()).put(data).flip();
            pong.complete(copy);
            socket.request(1);
            return null;
          }

          @Override
          public CompletionStage<?> onClose(WebSocket socket, int status, String reason) {
            close.complete(status);
            return null;
          }

          @Override
          public void onError(WebSocket socket, Throwable cause) {
            delivered.completeExceptionally(cause);
            pong.completeExceptionally(cause);
            close.completeExceptionally(cause);
          }
        };
        var uri = URI.create((tls ? "wss" : "ws") + "://localhost:" + address.getPort() + "/ws");
        var socket = client.newWebSocketBuilder().buildAsync(uri, listener)
            .get(5, TimeUnit.SECONDS);
        try {
          socket.sendBinary(ByteBuffer.wrap(new byte[] {1, 2}), false).get(5, TimeUnit.SECONDS);
          socket.sendPing(ByteBuffer.wrap(new byte[] {42})).get(5, TimeUnit.SECONDS);
          assertEquals(42, pong.get(5, TimeUnit.SECONDS).get());
          assertFalse(delivered.isDone());
          socket.sendBinary(ByteBuffer.wrap(new byte[] {3, 4}), true).get(5, TimeUnit.SECONDS);
          delivered.get(5, TimeUnit.SECONDS);
          verify(decoder).decode(new byte[] {1, 2, 3, 4});
          socket.sendClose(1000, "finished").get(5, TimeUnit.SECONDS);
          assertEquals(1000, close.get(5, TimeUnit.SECONDS));
        } finally {
          socket.abort();
        }
      }
    } finally {
      assertTimeoutPreemptively(Duration.ofSeconds(5), service::shutdown);
      restore("keystore.file.path", oldPath);
      restore("keystore.file.password", oldPassword);
    }
  }

  @Test
  void testFailedBindCleansUpAndAllowsRestart() throws Exception {
    try (var occupied = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
      var service = service(occupied.getLocalPort());
      try {
        service.start();
        assertThrows(IllegalStateException.class, service::activate);
        var field = NettyWebSocketImpl.class.getDeclaredField("webSocketWorkers");
        field.setAccessible(true);
        assertTrue(((io.netty.channel.EventLoopGroup) field.get(service)).isTerminated());
        service.setWebSocketConfiguration(new SocketConfiguration("websocket",
            TransportType.WEB_SOCKET, 0, 0));
        service.start();
        service.activate();
        assertEquals(1, listeners(service).size());
      } finally {
        assertTimeoutPreemptively(Duration.ofSeconds(5), service::shutdown);
      }
    }
  }

  private void restore(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }
}
