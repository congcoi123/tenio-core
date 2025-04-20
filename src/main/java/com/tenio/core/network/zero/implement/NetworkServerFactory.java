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

import com.tenio.common.configuration.Configuration;
import com.tenio.core.configuration.define.CoreConfigurationType;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.zero.handler.DatagramIoHandler;
import com.tenio.core.network.zero.handler.SocketIoHandler;
import com.tenio.core.network.zero.handler.implement.DatagramIoHandlerImpl;
import com.tenio.core.network.zero.handler.implement.SocketIoHandlerImpl;
import com.tenio.core.network.zero.handler.implement.VirtualDatagramHandler;
import com.tenio.core.network.zero.handler.implement.VirtualSocketHandler;
import java.util.ServiceLoader;

/**
 * Factory class for creating network server implementations.
 */
public final class NetworkServerFactory {

  private static final boolean ENABLE_VIRTUAL_THREADS = Boolean.parseBoolean(
      System.getProperty("enable.virtual.threads", "false"));

  private NetworkServerFactory() {
    throw new UnsupportedOperationException("This class cannot be instantiated");
  }

  /**
   * Creates a new instance of the appropriate network server based on configuration.
   *
   * @param eventManager the instance of {@link EventManager}
   * @param sessionManager the instance of {@link SessionManager}
   * @param networkReaderStatistic the instance of {@link NetworkReaderStatistic}
   * @param binaryPacketDecoder the instance of {@link BinaryPacketDecoder}
   * @param configuration the server configuration
   * @return a new instance of network server implementation
   */
  public static NetworkServer newInstance(EventManager eventManager,
                                        SessionManager sessionManager,
                                        NetworkReaderStatistic networkReaderStatistic,
                                        BinaryPacketDecoder binaryPacketDecoder,
                                        Configuration configuration) {
    boolean useVirtualThreads = ENABLE_VIRTUAL_THREADS && 
        configuration.getBoolean(CoreConfigurationType.PROP_USE_VIRTUAL_THREADS);
    
    if (useVirtualThreads) {
      return VirtualNetworkServer.newInstance(eventManager, sessionManager,
          networkReaderStatistic, binaryPacketDecoder);
    } else {
      return DefaultNetworkServer.newInstance(eventManager, sessionManager,
          networkReaderStatistic, binaryPacketDecoder);
    }
  }

  /**
   * Creates a new instance of the appropriate socket handler based on configuration.
   *
   * @param eventManager the instance of {@link EventManager}
   * @param configuration the server configuration
   * @return a new instance of socket handler implementation
   */
  public static SocketIoHandler newSocketHandler(EventManager eventManager,
                                               Configuration configuration) {
    boolean useVirtualThreads = ENABLE_VIRTUAL_THREADS && 
        configuration.getBoolean(CoreConfigurationType.PROP_USE_VIRTUAL_THREADS);
    
    if (useVirtualThreads) {
      return VirtualSocketHandler.newInstance(eventManager);
    } else {
      return SocketIoHandlerImpl.newInstance(eventManager);
    }
  }

  /**
   * Creates a new instance of the appropriate datagram handler based on configuration.
   *
   * @param eventManager the instance of {@link EventManager}
   * @param configuration the server configuration
   * @return a new instance of datagram handler implementation
   */
  public static DatagramIoHandler newDatagramHandler(EventManager eventManager,
                                                   Configuration configuration) {
    boolean useVirtualThreads = ENABLE_VIRTUAL_THREADS && 
        configuration.getBoolean(CoreConfigurationType.PROP_USE_VIRTUAL_THREADS);
    
    if (useVirtualThreads) {
      return VirtualDatagramHandler.newInstance(eventManager);
    } else {
      return DatagramIoHandlerImpl.newInstance(eventManager);
    }
  }
} 