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

package com.tenio.core.network.zero.handler.implement;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataType;
import com.tenio.common.data.DataUtility;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.zero.handler.SocketIoHandler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A virtual thread-based implementation of socket handler.
 */
public final class VirtualSocketHandler extends AbstractIoHandler implements SocketIoHandler {
  private final EventManager eventManager;
  private final ExecutorService virtualThreadExecutor;
  private final AtomicBoolean isRunning;
  private DataType dataType;
  private BinaryPacketDecoder binaryPacketDecoder;
  private NetworkReaderStatistic networkReaderStatistic;
  private SessionManager sessionManager;
  private Selector selector;

  private VirtualSocketHandler(EventManager eventManager) {
    super(eventManager);
    this.eventManager = eventManager;
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    this.isRunning = new AtomicBoolean(true);
    try {
      this.selector = Selector.open();
    } catch (IOException e) {
      throw new RuntimeException("Failed to create selector", e);
    }
  }

  /**
   * Creates a new instance of the virtual socket handler.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link VirtualSocketHandler}
   */
  public static VirtualSocketHandler newInstance(EventManager eventManager) {
    return new VirtualSocketHandler(eventManager);
  }

  @Override
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public void setPacketDecoder(BinaryPacketDecoder packetDecoder) {
    this.binaryPacketDecoder = packetDecoder;
  }

  @Override
  public void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic) {
    this.networkReaderStatistic = networkReaderStatistic;
  }

  @Override
  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public void channelActive(SocketChannel socketChannel, SelectionKey selectionKey) {
    var session = sessionManager.createSocketSession(socketChannel, selectionKey);
    session.activate();
    startVirtualThread(socketChannel);
  }

  private void startVirtualThread(SocketChannel socketChannel) {
    virtualThreadExecutor.submit(() -> {
      try {
        while (isRunning.get() && socketChannel.isConnected()) {
          ByteBuffer buffer = ByteBuffer.allocate(1024);
          int bytesRead = socketChannel.read(buffer);
          
          if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            processData(socketChannel, data);
          } else if (bytesRead == -1) {
            // Connection closed
            break;
          }
        }
      } catch (IOException e) {
        handleException(socketChannel, e);
      } finally {
        try {
          socketChannel.close();
        } catch (IOException e) {
          // Ignore close exception
        }
      }
    });
  }

  @Override
  public void channelInactive(SocketChannel socketChannel) {
    var session = sessionManager.getSessionBySocket(socketChannel);
    if (session != null) {
      eventManager.emit(ServerEvent.SESSION_WILL_BE_CLOSED, session);
    }
  }

  @Override
  public void channelException(SocketChannel socketChannel, Exception exception) {
    var session = sessionManager.getSessionBySocket(socketChannel);
    if (session != null) {
      eventManager.emit(ServerEvent.SESSION_OCCURRED_EXCEPTION, session, exception);
    }
  }

  @Override
  public void sessionException(Session session, Exception exception) {
    eventManager.emit(ServerEvent.SESSION_OCCURRED_EXCEPTION, session, exception);
  }

  @Override
  public void sessionRead(Session session, byte[] binary) {
    binaryPacketDecoder.decode(session, binary);
  }

  private void processData(SocketChannel socketChannel, byte[] data) {
    Session session = sessionManager.getSessionBySocket(socketChannel);
    
    if (session == null) {
      try {
        // First connection
        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        session = sessionManager.createSocketSession(socketChannel, selectionKey);
        session.activate();
        eventManager.emit(ServerEvent.SESSION_REQUEST_CONNECTION, session, 
            DataUtility.binaryToCollection(dataType, data));
      } catch (IOException e) {
        handleException(socketChannel, e);
      }
    } else {
      if (session.isActivated()) {
        if (!session.isAssociatedToPlayer(Session.AssociatedState.DOING)) {
          eventManager.emit(ServerEvent.SESSION_READ_MESSAGE, session, 
              DataUtility.binaryToCollection(dataType, data));
        }
      }
    }
  }

  private void handleException(SocketChannel socketChannel, Exception exception) {
    Session session = sessionManager.getSessionBySocket(socketChannel);
    if (session != null) {
      eventManager.emit(ServerEvent.SESSION_OCCURRED_EXCEPTION, session, exception);
    }
  }

  public void shutdown() {
    isRunning.set(false);
    virtualThreadExecutor.shutdown();
    try {
      selector.close();
    } catch (IOException e) {
      // Ignore close exception
    }
  }
} 