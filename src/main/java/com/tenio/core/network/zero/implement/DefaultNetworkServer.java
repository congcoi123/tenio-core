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

package com.tenio.core.network.zero.implement;

import com.tenio.common.data.DataType;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.zero.handler.implement.DatagramIoHandlerImpl;
import com.tenio.core.network.zero.handler.implement.SocketIoHandlerImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of network server using traditional thread model.
 */
public final class DefaultNetworkServer implements NetworkServer {
  private final EventManager eventManager;
  private final SessionManager sessionManager;
  private final NetworkReaderStatistic networkReaderStatistic;
  private final BinaryPacketDecoder binaryPacketDecoder;
  private final SocketIoHandlerImpl socketHandler;
  private final DatagramIoHandlerImpl datagramHandler;
  private final Selector selector;
  private final AtomicBoolean isRunning;
  private ServerSocketChannel serverSocketChannel;
  private DatagramChannel datagramChannel;
  private int tcpPort;
  private int udpPort;

  private DefaultNetworkServer(EventManager eventManager, SessionManager sessionManager,
                             NetworkReaderStatistic networkReaderStatistic,
                             BinaryPacketDecoder binaryPacketDecoder) {
    this.eventManager = eventManager;
    this.sessionManager = sessionManager;
    this.networkReaderStatistic = networkReaderStatistic;
    this.binaryPacketDecoder = binaryPacketDecoder;
    this.socketHandler = SocketIoHandlerImpl.newInstance(eventManager);
    this.datagramHandler = DatagramIoHandlerImpl.newInstance(eventManager);
    this.isRunning = new AtomicBoolean(true);
    
    try {
      this.selector = Selector.open();
    } catch (IOException e) {
      throw new RuntimeException("Failed to create selector", e);
    }
  }

  /**
   * Creates a new instance of the default network server.
   *
   * @param eventManager the instance of {@link EventManager}
   * @param sessionManager the instance of {@link SessionManager}
   * @param networkReaderStatistic the instance of {@link NetworkReaderStatistic}
   * @param binaryPacketDecoder the instance of {@link BinaryPacketDecoder}
   * @return a new instance of {@link DefaultNetworkServer}
   */
  public static DefaultNetworkServer newInstance(EventManager eventManager,
                                               SessionManager sessionManager,
                                               NetworkReaderStatistic networkReaderStatistic,
                                               BinaryPacketDecoder binaryPacketDecoder) {
    return new DefaultNetworkServer(eventManager, sessionManager, networkReaderStatistic,
        binaryPacketDecoder);
  }

  @Override
  public void initialize(int tcpPort, int udpPort) throws IOException {
    this.tcpPort = tcpPort;
    this.udpPort = udpPort;

    // Initialize TCP server
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(tcpPort));
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

    // Initialize UDP server
    datagramChannel = DatagramChannel.open();
    datagramChannel.bind(new InetSocketAddress(udpPort));
    datagramChannel.configureBlocking(false);
    datagramChannel.register(selector, SelectionKey.OP_READ);

    // Configure handlers
    configureHandlers();
  }

  private void configureHandlers() {
    // Configure socket handler
    socketHandler.setDataType(DataType.ZERO);
    socketHandler.setNetworkReaderStatistic(networkReaderStatistic);
    socketHandler.setSessionManager(sessionManager);
    socketHandler.setPacketDecoder(binaryPacketDecoder);

    // Configure datagram handler
    datagramHandler.setDataType(DataType.ZERO);
    datagramHandler.setNetworkReaderStatistic(networkReaderStatistic);
    datagramHandler.setSessionManager(sessionManager);
  }

  @Override
  public void start() {
    while (isRunning.get()) {
      try {
        selector.select();
        var selectedKeys = selector.selectedKeys().iterator();

        while (selectedKeys.hasNext()) {
          var key = selectedKeys.next();
          selectedKeys.remove();

          if (!key.isValid()) {
            continue;
          }

          if (key.isAcceptable()) {
            handleAccept(key);
          } else if (key.isReadable()) {
            handleRead(key);
          }
        }
      } catch (IOException e) {
        eventManager.emit(ServerEvent.SERVER_EXCEPTION, e);
      }
    }
  }

  private void handleAccept(SelectionKey key) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel socketChannel = serverChannel.accept();
    
    if (socketChannel != null) {
      socketChannel.configureBlocking(false);
      SelectionKey newKey = socketChannel.register(selector, SelectionKey.OP_READ);
      socketHandler.channelActive(socketChannel, newKey);
    }
  }

  private void handleRead(SelectionKey key) {
    if (key.channel() instanceof SocketChannel) {
      SocketChannel socketChannel = (SocketChannel) key.channel();
      try {
        socketHandler.channelRead(socketChannel);
      } catch (IOException e) {
        socketHandler.channelException(socketChannel, e);
      }
    }
  }

  @Override
  public void shutdown() {
    isRunning.set(false);
    
    try {
      if (serverSocketChannel != null) {
        serverSocketChannel.close();
      }
      if (datagramChannel != null) {
        datagramChannel.close();
      }
      selector.close();
    } catch (IOException e) {
      eventManager.emit(ServerEvent.SERVER_EXCEPTION, e);
    }
  }
} 