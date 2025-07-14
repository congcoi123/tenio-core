/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

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

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.engine.acceptor.AcceptorHandler;
import com.tenio.core.network.zero.engine.listener.ZeroWriterListener;
import com.tenio.core.network.zero.handler.SocketIoHandler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Handles read/write events on socket channels using a {@link Selector}.
 *
 * <p>This class is part of the NIO event-driven loop that processes IO on
 * accepted TCP connections.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Register socket channels for read/write events</li>
 *   <li>Dispatch readable/writable keys to appropriate handlers</li>
 *   <li>Manage selection loop and wakeup mechanisms</li>
 *   <li>Notify {@link SocketIoHandler} for channel lifecycle events</li>
 * </ul>
 *
 * <p>Each reader thread runs in a loop, polling its selector and reacting
 * to channel readiness, ensuring non-blocking high-performance IO handling.
 *
 * @see AcceptorHandler
 * @see SocketIoHandler
 * @since 0.6.5
 */

public final class SocketReaderHandler extends SystemLogger {

  private final Selector readableSelector;
  private final ByteBuffer readerBuffer;
  private final Queue<Pair<SelectableChannel, Consumer<SelectionKey>>> pendingClientChannels;
  private final ZeroWriterListener zeroWriterListener;
  private final SessionManager sessionManager;
  private final NetworkReaderStatistic networkReaderStatistic;
  private final SocketIoHandler socketIoHandler;

  /**
   * Constructor.
   *
   * @param readerBuffer           instance of {@link ByteBuffer}
   * @param zeroWriterListener     instance of {@link ZeroWriterListener}
   * @param sessionManager         instance of {@link SessionManager}
   * @param networkReaderStatistic instance of {@link NetworkReaderStatistic}
   * @param socketIoHandler        instance of {@link SocketIoHandler}
   * @throws IOException whenever any IO exception thrown
   */
  public SocketReaderHandler(ByteBuffer readerBuffer,
                             ZeroWriterListener zeroWriterListener,
                             SessionManager sessionManager,
                             NetworkReaderStatistic networkReaderStatistic,
                             SocketIoHandler socketIoHandler) throws IOException {
    this.readerBuffer = readerBuffer;
    this.zeroWriterListener = zeroWriterListener;
    this.sessionManager = sessionManager;
    this.networkReaderStatistic = networkReaderStatistic;
    this.socketIoHandler = socketIoHandler;

    readableSelector = Selector.open();
    pendingClientChannels = new ConcurrentLinkedQueue<>();
  }

  /**
   * Registers a client socket to the reader selector.
   *
   * @param channel         {@link SocketChannel} a client channel
   * @param onKeyRegistered when its {@link SelectionKey} is ready
   */
  public void registerClientSocketChannel(SocketChannel channel,
                                          Consumer<SelectionKey> onKeyRegistered) {
    pendingClientChannels.offer(Pair.of(channel, onKeyRegistered));
    wakeup();
  }

  /**
   * Shutdown processing.
   *
   * @throws IOException whenever IO exceptions thrown
   */
  public void shutdown() throws IOException {
    wakeup();
    readableSelector.close();
  }

  /**
   * Processing. This should be run in a loop.
   */
  public void running() {
    try {
      // register channels to selector
      if (!pendingClientChannels.isEmpty()) {
        // readable selector was registered by OP_READ interested only socket channels,
        // but in some cases, we can receive "can writable" signal from those sockets
        Pair<SelectableChannel, Consumer<SelectionKey>> callbackableChannel;
        while ((callbackableChannel = pendingClientChannels.poll()) != null) {
          SelectableChannel channel = callbackableChannel.getKey();
          Consumer<SelectionKey> callback = callbackableChannel.getValue();
          var socketChannel = (SocketChannel) channel;
          SelectionKey selectionKey =
              socketChannel.register(readableSelector, SelectionKey.OP_READ);
          callback.accept(selectionKey);
        }
      }

      // blocks until at least one channel is ready for the events you registered for
      int countReadyKeys = readableSelector.select();

      if (countReadyKeys == 0) {
        return;
      }

      var readyKeys = readableSelector.selectedKeys();
      var keyIterator = readyKeys.iterator();

      while (keyIterator.hasNext()) {
        SelectionKey selectionKey = keyIterator.next();
        // once a key is proceeded, it should be removed from the process to prevent
        // duplicating manipulation
        keyIterator.remove();

        if (selectionKey.isValid()) {
          var selectableChannel = selectionKey.channel();
          var socketChannel = (SocketChannel) selectableChannel;
          readTcpData(socketChannel, selectionKey, readerBuffer);
        }
      }
    } catch (ClosedSelectorException exception1) {
      if (isErrorEnabled()) {
        error(exception1, "Selector is closed: ", exception1.getMessage());
      }
    } catch (CancelledKeyException exception2) {
      if (isErrorEnabled()) {
        error(exception2, "Cancelled key: ", exception2.getMessage());
      }
    } catch (IOException exception3) {
      if (isErrorEnabled()) {
        error(exception3, "I/O reading/selection error: ", exception3.getMessage());
      }
    } catch (Exception exception4) {
      if (isErrorEnabled()) {
        error(exception4, "Generic reading/selection error: ", exception4.getMessage());
      }
    }
  }

  private void readTcpData(SocketChannel socketChannel, SelectionKey selectionKey,
                           ByteBuffer readerBuffer) {
    // retrieves session by its socket channel
    var session = sessionManager.getSessionBySocket(socketChannel);

    if (session == null) {
      if (isDebugEnabled()) {
        debug("READ TCP CHANNEL", "Reader handle a null session with the socket channel: ",
            socketChannel.toString());
      }
      return;
    }

    if (!session.isActivated()) {
      if (isDebugEnabled()) {
        debug("READ TCP CHANNEL", "Session is inactivated: ", session.toString());
      }
      return;
    }

    // when a socket channel is writable, should make it the highest priority
    // manipulation
    if (selectionKey.isValid() && selectionKey.isWritable()) {
      // should continually put this session for sending all left packets first
      zeroWriterListener.continueWriteInterestOp(session);
      // now we should set it back to interest in OP_READ
      selectionKey.interestOps(SelectionKey.OP_READ);
    }

    if (selectionKey.isValid() && selectionKey.isReadable()) {
      // prepares the buffer first
      readerBuffer.clear();
      // reads data from socket and write them to buffer
      int byteCount = 0;
      try {
        // this isOpen() && isConnected() method can only work if the server side decides to close
        // the socket. There is no way to know if the connection is closed on the client side
        byteCount = socketChannel.read(readerBuffer);
        if (byteCount == -1) {
          // no left data is available, should close the connection
          socketIoHandler.channelInactive(socketChannel, selectionKey,
              ConnectionDisconnectMode.LOST_IN_READ);
          return;
        }
      } catch (IOException exception) {
        if (isErrorEnabled()) {
          error(exception, "An exception was occurred on channel: ", socketChannel.toString());
        }
        socketIoHandler.sessionException(session, exception);
      }
      if (byteCount > 0) {
        // update statistic data
        session.addReadBytes(byteCount);
        networkReaderStatistic.updateReadBytes(byteCount);
        // ready to read data from buffer
        readerBuffer.flip();
        // reads data from buffer and transfers them to the next process
        byte[] binary = new byte[readerBuffer.limit()];
        readerBuffer.get(binary);

        socketIoHandler.sessionRead(session, binary);
      }
    }
  }

  /**
   * Wakeup the reader selector.
   */
  private void wakeup() {
    readableSelector.wakeup();
  }
}
