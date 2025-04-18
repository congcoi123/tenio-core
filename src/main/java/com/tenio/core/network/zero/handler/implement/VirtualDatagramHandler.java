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
import com.tenio.common.data.msgpack.element.MsgPackMap;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.configuration.constant.CoreConstant;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.handler.DatagramIoHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A virtual thread-based implementation of datagram handler.
 */
public final class VirtualDatagramHandler extends AbstractIoHandler implements DatagramIoHandler {
  private final EventManager eventManager;
  private final ExecutorService virtualThreadExecutor;
  private final AtomicBoolean isRunning;
  private DataType dataType;
  private NetworkReaderStatistic networkReaderStatistic;
  private SessionManager sessionManager;
  private Selector selector;

  private VirtualDatagramHandler(EventManager eventManager) {
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
   * Creates a new instance of the virtual datagram handler.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link VirtualDatagramHandler}
   */
  public static VirtualDatagramHandler newInstance(EventManager eventManager) {
    return new VirtualDatagramHandler(eventManager);
  }

  @Override
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic) {
    this.networkReaderStatistic = networkReaderStatistic;
  }

  @Override
  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  public void startDatagramChannel(DatagramChannel datagramChannel) {
    try {
        datagramChannel.register(selector, SelectionKey.OP_READ);
        startVirtualThread(datagramChannel);
    } catch (IOException e) {
        handleException(datagramChannel, e);
    }
  }

  private void startVirtualThread(DatagramChannel datagramChannel) {
    virtualThreadExecutor.submit(() -> {
      try {
        while (isRunning.get() && datagramChannel.isOpen()) {
          ByteBuffer buffer = ByteBuffer.allocate(1024);
          SocketAddress remoteAddress = datagramChannel.receive(buffer);
          
          if (remoteAddress != null) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            processData(datagramChannel, remoteAddress, data);
          }
        }
      } catch (IOException e) {
        handleException(datagramChannel, e);
      } finally {
        try {
          datagramChannel.close();
        } catch (IOException e) {
          // Ignore close exception
        }
      }
    });
  }

  @Override
  public void channelRead(DatagramChannel datagramChannel, SocketAddress remoteAddress,
                         DataCollection message) {
    eventManager.emit(ServerEvent.DATAGRAM_CHANNEL_READ_MESSAGE_FIRST_TIME, datagramChannel,
        remoteAddress, message);
  }

  @Override
  public void sessionRead(Session session, DataCollection message) {
    eventManager.emit(ServerEvent.SESSION_READ_MESSAGE, session, message);
  }

  @Override
  public void channelException(DatagramChannel datagramChannel, Exception exception) {
    // do nothing, the exception was already logged
  }

  @Override
  public void sessionException(Session session, Exception exception) {
    eventManager.emit(ServerEvent.SESSION_OCCURRED_EXCEPTION, session, exception);
  }

  private void processData(DatagramChannel datagramChannel, SocketAddress remoteAddress, byte[] data) {
    var message = DataUtility.binaryToCollection(dataType, data);
    var udpConvey = Session.EMPTY_DATAGRAM_CONVEY_ID;
    
    if (message instanceof ZeroMap zeroMap) {
      if (zeroMap.containsKey(CoreConstant.DEFAULT_KEY_UDP_CONVEY_ID)) {
        udpConvey = zeroMap.getInteger(CoreConstant.DEFAULT_KEY_UDP_CONVEY_ID);
      }
    } else if (message instanceof MsgPackMap msgPackMap) {
      if (msgPackMap.contains(CoreConstant.DEFAULT_KEY_UDP_CONVEY_ID)) {
        udpConvey = msgPackMap.getInteger(CoreConstant.DEFAULT_KEY_UDP_CONVEY_ID);
      }
    }

    Session session = sessionManager.getSessionByDatagram(udpConvey);
    if (session == null) {
      channelRead(datagramChannel, remoteAddress, message);
    } else {
      if (session.isActivated()) {
        session.setDatagramRemoteSocketAddress(remoteAddress);
        session.addReadBytes(data.length);
        networkReaderStatistic.updateReadBytes(data.length);
        networkReaderStatistic.updateReadPackets(1);
        sessionRead(session, message);
      }
    }
  }

  private void handleException(DatagramChannel datagramChannel, Exception exception) {
    channelException(datagramChannel, exception);
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