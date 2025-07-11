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

package com.tenio.core.network.zero.engine.manager;

import com.tenio.common.utility.OsUtility;
import com.tenio.core.configuration.constant.CoreConstant;
import com.tenio.core.exception.EmptyDatagramChannelsException;
import com.tenio.core.manager.Manager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class takes responsibility to provide an available UDP channel port, KCP convey id, ... when required.
 */
public class DatagramChannelManager implements Manager {

  private final AtomicInteger udpConveyIdGenerator;
  private final AtomicInteger kcpConveyIdGenerator;
  private final List<DatagramChannel> channels;
  private final AtomicInteger channelIndexer;
  private volatile int channelCacheSize;
  private volatile int udpPort;
  private volatile int kcpPort;

  private DatagramChannelManager() {
    udpPort = CoreConstant.NULL_PORT_VALUE;
    kcpPort = CoreConstant.NULL_PORT_VALUE;
    udpConveyIdGenerator = new AtomicInteger(0);
    kcpConveyIdGenerator = new AtomicInteger(0);
    channels = new ArrayList<>();
    channelIndexer = new AtomicInteger(0);
  }

  /**
   * Creates a new UDP channel manager instance.
   *
   * @return a new instance of {@link DatagramChannelManager}
   */
  public static DatagramChannelManager newInstance() {
    return new DatagramChannelManager();
  }

  /**
   * Configures UDP channels in cache.
   *
   * @param serverAddress the server IP address
   * @param udpPort       the UDP port
   * @param cacheSize     the number of elements in cache
   * @throws IOException whenever there is any issue while opening a new UDP channel
   */
  public void configureUdpChannelCache(String serverAddress, int udpPort, int cacheSize)
      throws IOException {
    this.udpPort = udpPort;
    channelCacheSize = cacheSize;

    for (int index = 0; index < channelCacheSize; index++) {
      var datagramChannel = DatagramChannel.open();
      datagramChannel.configureBlocking(false);
      if (OsUtility.getOperatingSystemType() == OsUtility.OsType.WINDOWS) {
        datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
      } else {
        datagramChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
      }
      datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
      datagramChannel.bind(new InetSocketAddress(serverAddress, udpPort));
      channels.add(datagramChannel);
    }
  }

  /**
   * Configures KCP port.
   *
   * @param kcpPort KCP port
   */
  public void configureKcpPort(int kcpPort) {
    this.kcpPort = kcpPort;
  }

  /**
   * Retrieves the current available datagram channel from cache, applies the "round-robin"
   * algorithm.
   *
   * @return an {@link DatagramChannel} instance
   */
  public DatagramChannel getChannel() {
    if (channelCacheSize == 0) {
      throw new EmptyDatagramChannelsException();
    }
    int index = Math.floorMod(channelIndexer.getAndIncrement(), channelCacheSize);
    return channels.get(index);
  }

  /**
   * Retrieves the current available UDP Convey Id.
   *
   * @return an {@code integer} value of a UDP Convey Id
   * @since 0.6.0
   */
  public int getCurrentUdpConveyId() {
    return udpConveyIdGenerator.getAndIncrement();
  }

  /**
   * Retrieves the current available KCP Convey Id.
   *
   * @return an {@code integer} value of a KCP Convey Id
   * @since 0.6.0
   */
  public int getCurrentKcpConveyId() {
    return kcpConveyIdGenerator.getAndIncrement();
  }

  /**
   * Retrieves UDP port.
   *
   * @return the UDP port
   */
  public int getUdpPort() {
    return udpPort;
  }

  /**
   * Retrieves KCP port.
   *
   * @return the KCP port
   */
  public int getKcpPort() {
    return kcpPort;
  }
}
