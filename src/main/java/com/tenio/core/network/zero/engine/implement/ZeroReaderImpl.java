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

package com.tenio.core.network.zero.engine.implement;

import com.tenio.common.data.DataType;
import com.tenio.common.utility.OsUtility;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.ServiceRuntimeException;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.engine.ZeroReader;
import com.tenio.core.network.zero.engine.listener.ZeroReaderListener;
import com.tenio.core.network.zero.engine.listener.ZeroWriterListener;
import com.tenio.core.network.zero.engine.reader.ReaderHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The implementation for the reader engine.
 *
 * @see ZeroReader
 */
public final class ZeroReaderImpl extends AbstractZeroEngine
    implements ZeroReader, ZeroReaderListener {

  private static final AtomicInteger INDEXER = new AtomicInteger(0);

  private volatile List<ReaderHandler> readerHandlers;
  private DataType dataType;
  private String serverAddress;
  private SocketConfiguration udpChannelConfiguration;
  private ZeroWriterListener zeroWriterListener;
  private NetworkReaderStatistic networkReaderStatistic;

  private ZeroReaderImpl(EventManager eventManager) {
    super(eventManager);
    setName("reader");
  }

  /**
   * Creates a new instance of reader engine.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link ZeroReader}
   */
  public static ZeroReader newInstance(EventManager eventManager) {
    return new ZeroReaderImpl(eventManager);
  }

  private ReaderHandler getReaderHandler() {
    int index = Math.floorMod(INDEXER.getAndIncrement(), getThreadPoolSize());
    return readerHandlers.get(index);
  }

  @Override
  public void acceptClientSocketChannel(SocketChannel socketChannel,
                                        Consumer<SelectionKey> onKeyRegistered) {
    getReaderHandler().registerClientSocketChannel(socketChannel, onKeyRegistered);
  }

  @Override
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  @Override
  public void setUdpChannelConfiguration(SocketConfiguration udpChannelConfiguration) {
    this.udpChannelConfiguration = udpChannelConfiguration;
  }

  @Override
  public void setZeroWriterListener(ZeroWriterListener zeroWriterListener) {
    this.zeroWriterListener = zeroWriterListener;
  }

  @Override
  public NetworkReaderStatistic getNetworkReaderStatistic() {
    return networkReaderStatistic;
  }

  @Override
  public void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic) {
    this.networkReaderStatistic = networkReaderStatistic;
  }

  @Override
  public void onInitialized() {
    readerHandlers = new ArrayList<>(getThreadPoolSize());
  }

  @Override
  public void onStarted() {
    // do nothing
  }

  @Override
  public void onRunning() {
    // Default read buffer is DIRECT
    ByteBuffer readerBuffer = ByteBuffer.allocateDirect(getMaxBufferSize());

    try {
      var readerHandler = new ReaderHandler(dataType, readerBuffer, zeroWriterListener,
          getSessionManager(), getNetworkReaderStatistic(), getSocketIoHandler(),
          getDatagramIoHandler());
      readerHandlers.add(readerHandler);

      if (udpChannelConfiguration != null) {
        var datagramChannel = openDatagramChannel(serverAddress, udpChannelConfiguration.port());
        readerHandler.registerDatagramChannel(datagramChannel);
      }

      while (!Thread.currentThread().isInterrupted()) {
        if (isActivated()) {
          try {
            readerHandler.running();
          } catch (Throwable cause) {
            if (isErrorEnabled()) {
              error(cause);
            }
          }
        }
      }
    } catch (IOException exception) {
      if (isErrorEnabled()) {
        error(exception);
      }
    }
  }

  @Override
  public void onShutdown() {
    try {
      Thread.sleep(500L);
      for (var readerHandler : readerHandlers) {
        readerHandler.shutdown();
      }
    } catch (IOException | InterruptedException exception) {
      if (isErrorEnabled()) {
        error(exception, "Exception while closing the selector");
      }
    }
  }

  @Override
  public void onDestroyed() {
    // do nothing
  }

  private DatagramChannel openDatagramChannel(String serverAddress, int port)
      throws ServiceRuntimeException {
    try {
      var datagramChannel = DatagramChannel.open();
      datagramChannel.configureBlocking(false);
      if (OsUtility.getOperatingSystemType() == OsUtility.OsType.WINDOWS) {
        datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      } else {
        datagramChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
      }
      datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
      datagramChannel.bind(new InetSocketAddress(serverAddress, port));
      // udp datagram is a connectionless protocol, we don't need to create
      // bi-direction connection, that why it's not necessary to register it to
      // acceptable selector. Just leave it to the reader selector later
      if (isInfoEnabled()) {
        info("UDP CHANNEL", buildgen("Opened at address: ", serverAddress, ", port: ",
            port));
      }
      return datagramChannel;
    } catch (IOException exception) {
      throw new ServiceRuntimeException(exception.getMessage());
    }
  }
}
