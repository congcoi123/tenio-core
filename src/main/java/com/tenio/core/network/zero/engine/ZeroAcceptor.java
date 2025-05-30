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

package com.tenio.core.network.zero.engine;

import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.zero.engine.listener.ZeroReaderListener;

/**
 * The engine supports working with new coming socket connections.
 */
public interface ZeroAcceptor extends ZeroEngine {

  /**
   * Sets an instance for the connection filter.
   *
   * @param connectionFilter an instance of {@link ConnectionFilter}
   */
  void setConnectionFilter(ConnectionFilter connectionFilter);

  /**
   * Sets a listener for the reader engine which is using for communication between two engines.
   *
   * @param zeroReaderListener the {@link ZeroReaderListener} instance
   * @see ZeroReader
   */
  void setZeroReaderListener(ZeroReaderListener zeroReaderListener);

  /**
   * Declares the server IP address.
   *
   * @param serverAddress the {@link String} value of server IP address
   */
  void setServerAddress(String serverAddress);

  /**
   * Declares sockets (TCP, UDP) configurations for the network.
   *
   * @param tcpSocketConfiguration an instance of {@link SocketConfiguration} for TCP
   * @param udpSocketConfiguration an instance of {@link SocketConfiguration} for UDP
   */
  void setSocketConfiguration(SocketConfiguration tcpSocketConfiguration,
                              SocketConfiguration udpSocketConfiguration);
}
