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

package com.tenio.core.network.zero;

import com.tenio.core.network.define.data.SocketConfig;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.zero.codec.encoder.BinaryPacketEncoder;
import com.tenio.core.service.Service;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * All APIs designed for working with sockets (TCP/UDP).
 */
public interface ZeroSocketService extends Service {

  /**
   * Sets size of {@link ByteBuffer} using for an acceptor worker to read/write binaries data
   * from/down.
   *
   * @param bufferSize the size of {@link ByteBuffer} ({@code integer} value) for
   *                   reading/writing binaries data
   */
  void setAcceptorBufferSize(int bufferSize);

  /**
   * Sets the number of acceptor workers for the socket (TCP) which are using to accept new coming
   * clients.
   *
   * @param workerSize the number of acceptor workers for the socket ({@code integer} value)
   */
  void setAcceptorWorkerSize(int workerSize);

  /**
   * Sets size of {@link ByteBuffer} using for a reader worker to read/write binaries data
   * from/down.
   *
   * @param bufferSize the size of {@link ByteBuffer} ({@code integer} value) for
   *                   reading/writing binaries data
   */
  void setReaderBufferSize(int bufferSize);

  /**
   * Sets the number of reader workers for the socket (TCP) which are using to read coming packets
   * from clients side.
   *
   * @param workerSize the number of reader workers for the socket ({@code integer} value)
   */
  void setReaderWorkerSize(int workerSize);

  /**
   * Sets size of {@link ByteBuffer} using for a writer worker to read/write binaries data
   * from/down.
   *
   * @param bufferSize the size of {@link ByteBuffer} ({@code integer} value) for
   *                   reading/writing binaries data
   */
  void setWriterBufferSize(int bufferSize);

  /**
   * Sets the number of writer workers for the socket (TCP) which are using to send packets to
   * clients side.
   *
   * @param workerSize the number of writer workers for the socket ({@code integer} value)
   */
  void setWriterWorkerSize(int workerSize);

  /**
   * Sets an instance for the connection filter.
   *
   * @param connectionFilter an instance of {@link ConnectionFilter}
   */
  void setConnectionFilter(ConnectionFilter connectionFilter);

  /**
   * Set a session manager.
   *
   * @param sessionManager the {@link SessionManager}
   */
  void setSessionManager(SessionManager sessionManager);

  /**
   * Sets a network reader statistic instance which takes responsibility recording the
   * receiving data from clients.
   *
   * @param networkReaderStatistic a {@link NetworkReaderStatistic} instance
   */
  void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic);

  /**
   * Sets a network writer statistic instance which takes responsibility recording the
   * sending data from the network.
   *
   * @param networkWriterStatistic a {@link NetworkWriterStatistic} instance
   */
  void setNetworkWriterStatistic(NetworkWriterStatistic networkWriterStatistic);

  /**
   * Declares a list of socket configurations for the network.
   *
   * @param socketConfigs a list of {@link SocketConfig}
   * @see List
   */
  void setSocketConfigs(List<SocketConfig> socketConfigs);

  /**
   * Sets an instance of packet encoder to encode packets for sending to clients side via the
   * socket (TCP).
   *
   * @param packetEncoder an instance of {@link BinaryPacketEncoder}
   */
  void setPacketEncoder(BinaryPacketEncoder packetEncoder);

  /**
   * Sets an instance of packet decoder to decode packets sent from clients side via the socket
   * (TCP).
   *
   * @param packetDecoder an instance of {@link BinaryPacketDecoder}
   */
  void setPacketDecoder(BinaryPacketDecoder packetDecoder);

  /**
   * Writes down (binaries) data to socket/channel in order to send them to client side.
   *
   * @param packet an instance of {@link Packet} using to carry conveying information
   */
  void write(Packet packet);
}
