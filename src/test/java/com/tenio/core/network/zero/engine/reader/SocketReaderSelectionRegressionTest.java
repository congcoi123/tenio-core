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

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.handler.SocketIoHandler;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SocketReaderSelectionRegressionTest {
  @Test
  void drainsAlreadyReadableBytesWithoutNeedingAnotherClientWrite() throws Exception {
    var sessions = mock(SessionManager.class);
    var io = mock(SocketIoHandler.class);
    // Force one TCP write to require several reader iterations without relying on OS timing.
    var reader =
        new SocketReaderHandler(
            ByteBuffer.allocate(1), sessions, mock(NetworkReaderStatistic.class), io);
    try (var listener = ServerSocketChannel.open()) {
      listener.bind(new InetSocketAddress("127.0.0.1", 0));
      try (var client = SocketChannel.open(listener.getLocalAddress());
          var server = listener.accept()) {
        server.configureBlocking(false);
        var session = mock(Session.class);
        when(sessions.getSessionBySocket(server)).thenReturn(session);
        when(session.isActivated()).thenReturn(true);
        reader.registerClientSocketChannel(server, key -> {}, () -> {});
        reader.running();
        client.write(ByteBuffer.wrap(new byte[] {1, 2, 3, 4}));
        assertTimeoutPreemptively(
            Duration.ofSeconds(2),
            () -> {
              for (int i = 0; i < 4; i++) {
                reader.running();
              }
            });
        verify(io, timeout(1000).times(4)).sessionRead(eq(session), any(byte[].class));
      }
    } finally {
      reader.shutdown();
    }
  }
}
