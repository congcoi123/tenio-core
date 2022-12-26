/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.zero.engine.implement;

import com.tenio.common.utility.OsUtility;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.RefusedConnectionAddressException;
import com.tenio.core.exception.ServiceRuntimeException;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.define.data.SocketConfig;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.zero.engine.ZeroAcceptor;
import com.tenio.core.network.zero.engine.listener.ZeroAcceptorListener;
import com.tenio.core.network.zero.engine.listener.ZeroReaderListener;
import com.tenio.core.server.ServerImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The implementation for acceptor engine.
 *
 * @see ZeroAcceptor
 */
public final class ZeroAcceptorImpl extends AbstractZeroEngine
    implements ZeroAcceptor, ZeroAcceptorListener {

  private final List<SocketChannel> acceptableSockets;
  private final List<SelectableChannel> boundSockets;
  private Selector acceptableSelector;
  private ConnectionFilter connectionFilter;
  private ZeroReaderListener zeroReaderListener;
  private String serverAddress;
  private int amountUdpWorkers;
  private boolean enabledKcp;
  private SocketConfig socketConfig;

  private ZeroAcceptorImpl(EventManager eventManager) {
    super(eventManager);

    acceptableSockets = new ArrayList<>();
    boundSockets = new ArrayList<>();

    setName("acceptor");
  }

  public static ZeroAcceptor newInstance(EventManager eventManager) {
    return new ZeroAcceptorImpl(eventManager);
  }

  private void initializeSockets() throws ServiceRuntimeException {
    // opens a selector to handle server socket, udp datagram and accept all incoming client socket
    try {
      acceptableSelector = Selector.open();
    } catch (IOException e) {
      throw new ServiceRuntimeException(e.getMessage());
    }

    // each socket configuration constructs a server socket or an udp datagram
    bindSocket(socketConfig);
  }

  private void bindSocket(SocketConfig socketConfig) throws ServiceRuntimeException {
    if (socketConfig.type() == TransportType.TCP) {
      bindTcpSocket(socketConfig.port());
    }
    if (amountUdpWorkers > 0) {
      bindUdpSocket();
    }
  }

  private void bindTcpSocket(int port) throws ServiceRuntimeException {
    try {
      var serverSocketChannel = ServerSocketChannel.open();
      serverSocketChannel.configureBlocking(false);
      if (OsUtility.getOperatingSystemType() == OsUtility.OsType.WINDOWS) {
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      } else {
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
      }
      serverSocketChannel.socket().bind(new InetSocketAddress(serverAddress, port));
      info("TCP SOCKET", buildgen("Started at address: ", serverAddress, ", port: ",
          serverSocketChannel.socket().getLocalPort()));
      // only server socket should interest in this key OP_ACCEPT
      serverSocketChannel.register(acceptableSelector, SelectionKey.OP_ACCEPT);
      synchronized (boundSockets) {
        boundSockets.add(serverSocketChannel);
      }
    } catch (IOException e) {
      throw new ServiceRuntimeException(e.getMessage());
    }
  }

  private void bindUdpSocket() throws ServiceRuntimeException {
    try {
      synchronized (boundSockets) {
        for (int i = 0; i < amountUdpWorkers; i++) {
          var datagramChannel = DatagramChannel.open();
          datagramChannel.configureBlocking(false);
          if (OsUtility.getOperatingSystemType() == OsUtility.OsType.WINDOWS) {
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
          } else {
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
          }
          datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
          datagramChannel.socket().bind(new InetSocketAddress(serverAddress, 0));
          // udp datagram is a connectionless protocol, we don't need to create
          // bi-direction connection, that why it's not necessary to register it to
          // acceptable selector. Just leave it to the reader selector later
          zeroReaderListener.acceptDatagramChannel(datagramChannel);
          int boundPort = datagramChannel.socket().getLocalPort();
          ServerImpl.getInstance().getUdpChannelManager().appendUdpPort(boundPort);
          info(enabledKcp ? "UDP CHANNEL (KCP)" : "UDP CHANNEL",
              buildgen("Started at address: ", serverAddress, ", port: ", boundPort));
          boundSockets.add(datagramChannel);
        }
      }
    } catch (IOException e) {
      throw new ServiceRuntimeException(e.getMessage());
    }
  }

  private void acceptableLoop() throws IOException {
    // blocks until at least one channel is ready for the events you registered for
    acceptableSelector.select();

    synchronized (acceptableSelector) {
      if (!acceptableSelector.isOpen()) {
        return;
      }
      // retrieves a set of selected keys
      var selectedKeys = acceptableSelector.selectedKeys();
      var keyIterator = selectedKeys.iterator();

      while (keyIterator.hasNext()) {
        // iterates the list of selection keys
        var selectionKey = keyIterator.next();
        // once a key is proceeded, it should be removed from the process to prevent
        // duplicating manipulation
        keyIterator.remove();

        // a client socket was accepted by a server socket
        // we only interest in this event
        if (selectionKey.isAcceptable()) {
          try {
            // get the server socket channel from the selector
            var serverChannel = (ServerSocketChannel) selectionKey.channel();
            // and accept the incoming request from client
            var clientChannel = serverChannel.accept();

            // make sure that the socket is available
            if (Objects.nonNull(clientChannel)) {
              synchronized (acceptableSockets) {
                acceptableSockets.add(clientChannel);
              }
            }

          } catch (IOException e) {
            error(e);
          }
        }
      }
    }

    // wakes up the reader selector for bellow channels handling
    zeroReaderListener.wakeup();
  }

  private void shutdownAcceptedSockets() {
    synchronized (acceptableSockets) {
      // iterates the list of client socket channels
      var socketIterator = acceptableSockets.iterator();

      while (socketIterator.hasNext()) {
        var socketChannel = socketIterator.next();
        socketIterator.remove();

        try {
          getSocketIoHandler().channelInactive(socketChannel);
          if (!socketChannel.socket().isClosed()) {
            socketChannel.socket().shutdownInput();
            socketChannel.socket().shutdownOutput();
            socketChannel.close();
          }
        } catch (IOException e) {
          getSocketIoHandler().channelException(socketChannel, e);
          error(e);
        }
      }
    }
  }

  private void shutdownBoundSockets() {
    synchronized (boundSockets) {
      // iterates the list of server socket channels, datagram channels
      var boundSocketIterator = boundSockets.iterator();

      while (boundSocketIterator.hasNext()) {
        var socketChannel = boundSocketIterator.next();
        boundSocketIterator.remove();

        try {
          socketChannel.close();
        } catch (IOException e) {
          error(e);
        }
      }
    }
  }

  private void shutdownSelector() {
    try {
      Thread.sleep(500L);
      acceptableSelector.close();
    } catch (IOException | InterruptedException e) {
      error(e);
    }
  }

  private void cleanup() {
  }

  @Override
  public void handleAcceptableChannels() {

    // we need to register all new client channel to the reader selector before
    // reading
    synchronized (acceptableSockets) {
      if (acceptableSockets.isEmpty()) {
        return;
      }

      var socketIterator = acceptableSockets.iterator();

      while (socketIterator.hasNext()) {
        var socketChannel = socketIterator.next();
        // makes sure that the loop will never check this item again
        socketIterator.remove();

        if (Objects.isNull(socketChannel)) {
          debug("ACCEPTABLE CHANNEL", "Acceptor handle a null socket channel");
        } else {
          var socket = socketChannel.socket();

          if (Objects.isNull(socket)) {
            debug("ACCEPTABLE CHANNEL", "Acceptor handle a null socket");
          } else {
            var inetAddress = socket.getInetAddress();
            if (Objects.nonNull(inetAddress)) {
              try {
                connectionFilter.validateAndAddAddress(inetAddress.getHostAddress());
                socketChannel.configureBlocking(false);
                socketChannel.socket().setTcpNoDelay(true);
                SelectionKey selectionKey = zeroReaderListener.acceptSocketChannel(socketChannel);

                getSocketIoHandler().channelActive(socketChannel, selectionKey);
              } catch (RefusedConnectionAddressException e1) {
                getSocketIoHandler().channelException(socketChannel, e1);
                error(e1, "Refused connection with address: ", e1.getMessage());

                try {
                  getSocketIoHandler().channelInactive(socketChannel);
                  socketChannel.socket().shutdownInput();
                  socketChannel.socket().shutdownOutput();
                  socketChannel.close();
                } catch (IOException e2) {
                  getSocketIoHandler().channelException(socketChannel, e2);
                  error(e2,
                      "Additional problem with refused connection. "
                          + "Was not able to shut down the channel: ",
                      e2.getMessage());
                }
              } catch (IOException e3) {
                getSocketIoHandler().channelException(socketChannel, e3);
                var logger = buildgen("Failed accepting connection: ");
                if (Objects.nonNull(socketChannel.socket())) {
                  logger.append(socketChannel.socket().getInetAddress().getHostAddress());
                }

                error(e3, logger);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void setConnectionFilter(ConnectionFilter filter) {
    connectionFilter = filter;
  }

  @Override
  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  @Override
  public void setAmountUdpWorkers(int amountUdpWorkers) {
    this.amountUdpWorkers = amountUdpWorkers;
  }

  @Override
  public void setEnabledKcp(boolean enabledKcp) {
    this.enabledKcp = enabledKcp;
  }

  @Override
  public void setSocketConfig(SocketConfig socketConfig) {
    this.socketConfig = socketConfig;
  }

  @Override
  public void setZeroReaderListener(ZeroReaderListener zeroReaderListener) {
    this.zeroReaderListener = zeroReaderListener;
  }

  @Override
  public void onInitialized() {
    initializeSockets();
  }

  @Override
  public void onStarted() {
    // do nothing
  }

  @Override
  public void onRunning() {
    while (true) {
      if (isActivated()) {
        try {
          acceptableLoop();
        } catch (IOException e) {
          error(e, e.getMessage());
        }
      }
    }
  }

  @Override
  public void onShutdown() {
    shutdownBoundSockets();
    shutdownAcceptedSockets();
    shutdownSelector();
  }

  @Override
  public void onDestroyed() {
    cleanup();
  }
}
