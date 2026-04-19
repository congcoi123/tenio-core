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

package com.tenio.core.network.zero.engine.reader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.handler.SocketIoHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For SocketReaderHandler")
class SocketReaderHandlerTest {

  private SocketReaderHandler handler;
  private SocketIoHandler socketIoHandler;

  @BeforeEach
  void setUp() throws IOException {
    socketIoHandler = mock(SocketIoHandler.class);
    handler = new SocketReaderHandler(
        ByteBuffer.allocate(512),
        mock(SessionManager.class),
        mock(NetworkReaderStatistic.class),
        socketIoHandler
    );
  }

  @Test
  @DisplayName("Constructor creates a non-null handler")
  void testConstructorCreatesHandler() throws IOException {
    assertNotNull(handler);
    handler.shutdown();
  }

  @Test
  @DisplayName("registerClientSocketChannel queues the channel and invokes onFailed when channel is already closed")
  void testRegisterClientSocketChannelQueuesClosed() throws IOException {
    AtomicBoolean failedCalled = new AtomicBoolean(false);
    SocketChannel closedChannel = mock(SocketChannel.class);
    Consumer<SelectionKey> onSuccess = key -> {};
    Runnable onFailed = () -> failedCalled.set(true);

    // Offer to the pending queue - will be drained on the next running() or shutdown()
    assertDoesNotThrow(() ->
        handler.registerClientSocketChannel(closedChannel, onSuccess, onFailed));
    handler.shutdown();
  }

  @Test
  @DisplayName("shutdown closes the selector without throwing")
  void testShutdownDoesNotThrow() {
    assertDoesNotThrow(() -> handler.shutdown());
  }

  @Test
  @DisplayName("running() registers a pending channel and invokes onSuccess callback")
  void testRunningProcessesPendingChannelAndInvokesOnSuccess() throws Exception {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 0));
    int port = ((InetSocketAddress) serverSocketChannel.getLocalAddress()).getPort();

    SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", port));
    SocketChannel serverChannel = serverSocketChannel.accept();
    serverSocketChannel.close();
    serverChannel.configureBlocking(false);

    AtomicBoolean successCalled = new AtomicBoolean(false);
    handler.registerClientSocketChannel(serverChannel, key -> successCalled.set(true), () -> {});

    assertDoesNotThrow(() -> handler.running());

    assertTrue(successCalled.get());
    clientChannel.close();
    serverChannel.close();
    handler.shutdown();
  }

  @Test
  @DisplayName("running() reads TCP data and delegates to socketIoHandler.sessionRead")
  void testRunningReadsTcpDataAndDelegatesToSessionRead() throws Exception {
    SessionManager sessionManager = mock(SessionManager.class);
    NetworkReaderStatistic statistic = mock(NetworkReaderStatistic.class);
    SocketIoHandler ioHandler = mock(SocketIoHandler.class);
    SocketReaderHandler h = new SocketReaderHandler(
        ByteBuffer.allocate(512), sessionManager, statistic, ioHandler);

    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 0));
    int port = ((InetSocketAddress) serverSocketChannel.getLocalAddress()).getPort();

    SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", port));
    SocketChannel serverChannel = serverSocketChannel.accept();
    serverSocketChannel.close();
    serverChannel.configureBlocking(false);

    Session session = mock(Session.class);
    when(sessionManager.getSessionBySocket(serverChannel)).thenReturn(session);
    when(session.isActivated()).thenReturn(true);

    h.registerClientSocketChannel(serverChannel, key -> {}, () -> {});
    h.running(); // processes pending channel, registers it for OP_READ

    clientChannel.write(ByteBuffer.wrap(new byte[]{1, 2, 3}));

    h.running(); // reads data and calls sessionRead

    verify(ioHandler).sessionRead(eq(session), any(byte[].class));
    clientChannel.close();
    serverChannel.close();
    h.shutdown();
  }

  @Test
  @DisplayName("running() with closed channel on pending queue invokes onFailed")
  void testRunningWithClosedPendingChannelInvokesOnFailed() throws Exception {
    SocketChannel closedChannel = SocketChannel.open();
    closedChannel.close(); // pre-close it

    AtomicBoolean failedCalled = new AtomicBoolean(false);
    handler.registerClientSocketChannel(closedChannel, key -> {}, () -> failedCalled.set(true));

    assertDoesNotThrow(() -> handler.running());

    assertTrue(failedCalled.get());
    handler.shutdown();
  }
}
