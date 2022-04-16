/*
The MIT License

Copyright (c) 2016-2021 kong <congcoi123@gmail.com>

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

package com.tenio.core.network;

import com.tenio.core.network.define.data.PathConfig;
import com.tenio.core.network.define.data.SocketConfig;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.zero.codec.encoder.BinaryPacketEncoder;
import com.tenio.core.service.Service;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * All designed APIs for network service.
 */
public interface NetworkService extends Service {

/**
* Assigns a port number for HTTP service.
*
* @param port the <code>integer</code> number for HTTP port
*/
  void setHttpPort(int port);

/**
* Declares a collection of path configurations for HTTP service.
*
* @param pathConfigs a collection of {@link PathConfig}
* @see Collection
*/
  void setHttpPathConfigs(List<PathConfig> pathConfigs);

/**
* Sets an implementation class for connection filter.
*
* @param clazz an implementation class of {@link ConnectionFilter}
* @param maxConnectionsPerIp an <code>integer</code> value, the maximum number of connections allowed in an IP address
* @throws InstantiationException
* @throws IllegalAccessException
* @throws IllegalArgumentException
* @throws InvocationTargetException
* @throws NoSuchMethodException
* @throws SecurityException
* @see DefaultConnectionFilter
*/
  void setConnectionFilterClass(Class<? extends ConnectionFilter> clazz, int maxConnectionsPerIp)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException,
      NoSuchMethodException, SecurityException;

/**
* Sets the number of consumer workers for websocket.
*
* @param workerSize the <code>integer</code> number of consumer workers for websocket
*/
  void setWebSocketConsumerWorkers(int workerSize);

/**
* Sets the number of producer workers for websocket.
*
* @param workerSize the <code>integer</code> number of producer workers for websocket
*/
  void setWebSocketProducerWorkers(int workerSize);

/**
* Sets the size of byte buffer using for websocket channel to write data to  buffer
*
* @param bufferSize the <code>integer</code> size of byte buffer for writing data
*/
  void setWebSocketSenderBufferSize(int bufferSize);

/**
* Sets the size of byte buffer using for websocket channel to read data from buffer
*
* @param bufferSize the <code>integer</code> size of byte buffer for reading data
*/
  void setWebSocketReceiverBufferSize(int bufferSize);

/**
* Determines whether the websocket is able to use SSL.
*
* @param usingSsl sets to <code>true</code> in case of using SSL, otherwise <code>false</code>
*/
  void setWebSocketUsingSsl(boolean usingSsl);

/**
* Sets the number of acceptor workers for socket (TCP) which are using to accept new comming clients.
*
* @param workerSize the <code>integer</code> number of acceptor workers for socket
*/
  void set
  void setSocketAcceptorWorkers(int workerSize);

/**
* Sets the number of reader workers for socket (TCP) which are using to read comming packets from clients.
*
* @param workerSize the <code>integer</code> number of reader workers for socket
*/
  void setSocketReaderWorkers(int workerSize);

/**
* Sets the number of writer workers for socket (TCP) which are using to send packets to clients.
*
* @param workerSize the <code>integer</code> number of writer workers for socket
*/
  void setSocketWriterWorkers(int workerSize);

/**
* Sets the size of byte buffer using for a acceptor worker to read/write data from/to buffer
*
* @param bufferSize the <code>integer</code> size of byte buffer for reading/writing data
*/
  void setSocketAcceptorBufferSize(int bufferSize);

/**
* Sets the size of byte buffer using for a reader worker to read/write data from/to buffer
*
* @param bufferSize the <code>integer</code> size of byte buffer for reading/writing data
*/
  void setSocketReaderBufferSize(int bufferSize);

/**
* Sets the size of byte buffer using for a writer worker to read/write data from/to buffer
*
* @param bufferSize the <code>integer</code> size of byte buffer for reading/writing data
*/
  void setSocketWriterBufferSize(int bufferSize);

/**
* Declares a list of socket configurations for the server.
*
* @param socketConfigs a collection of {@link SocketConfig}
* @see Collection
*/
  void setSocketConfigs(List<SocketConfig> socketConfigs);

/**
* Sets a packet queue policy class.
*
* @param clazz the implemetation class of {@link PacketQueuePolicy} used to apply rules for the packet queue
* @throws InstantiationException
* @throws IllegalAccessException
* @throws IllegalArgumentException
* @throws InvocationTargetException
* @throws NoSuchMethodException
* @throws SecurityException
* @see PacketQueue
* @see DefaultPacketQueuePolicy
*/
  void setPacketQueuePolicy(Class<? extends PacketQueuePolicy> clazz)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException,
      NoSuchMethodException, SecurityException;

/**
* Sets the packet queue size.
*
* @param queueSize the <code>integer</code> value of new size for the packet queue
* @see PacketQueue
* @see PacketQueuePolicy
*/
  void setPacketQueueSize(int queueSize);

/**
* Sets an instance of packet encoder to encode packets for the socket (TCP)
*
* @param packetEncoder an instance of {@link BinaryPacketEncoder}
*/
  void setPacketEncoder(BinaryPacketEncoder packetEncoder);

/**
* Sets an instance of packet decoder to decode packets for the socket (TCP)
*
* @param packetDecoder an instance of {@link BinaryPacketDecoder}
*/
  void setPacketDecoder(BinaryPacketDecoder packetDecoder);

/**
* Retrieves a network reader statistic instance which takes responsibility recording the receiving data from clients.
*
* @return a {@link NetworkReaderStatistic} instance
*/
  NetworkReaderStatistic getNetworkReaderStatistic();

/**
* Retrieves a network writer statistic instance which takes responsibility recording the sending data from the server.
*
* @return a {@link NetworkWriterStatistic} instance
*/
  NetworkWriterStatistic getNetworkWriterStatistic();

/**
* Writes down data to socket/channel to send them to client sides.
*
* @param response an instance of {@link Response} using to carry conveying information
*/
  void write(Response response);
}
