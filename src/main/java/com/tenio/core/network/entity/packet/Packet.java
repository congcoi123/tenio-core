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

package com.tenio.core.network.entity.packet;

import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Packet interface represents a network packet in the game server's communication system.
 * It provides a standardized way to handle data transmission between the server and clients,
 * supporting different transport protocols and priority levels.
 * 
 * <p>Core features:
 * <ul>
 *   <li>Binary data encapsulation</li>
 *   <li>Transport protocol specification</li>
 *   <li>Priority-based handling</li>
 *   <li>Encryption support</li>
 *   <li>Data integrity verification</li>
 * </ul>
 * 
 * <p>Packet lifecycle:
 * <ol>
 *   <li>Creation with data payload</li>
 *   <li>Transport type assignment</li>
 *   <li>Priority level setting</li>
 *   <li>Optional encryption</li>
 *   <li>Transmission handling</li>
 *   <li>Cleanup</li>
 * </ol>
 * 
 * <p>Performance considerations:
 * <ul>
 *   <li>Efficient memory usage for data storage</li>
 *   <li>Minimal overhead in packet creation</li>
 *   <li>Optimized data serialization</li>
 *   <li>Fast priority queue handling</li>
 *   <li>Efficient encryption/decryption</li>
 * </ul>
 * 
 * <p>Implementation guidelines:
 * <ul>
 *   <li>Use direct byte buffers for large packets</li>
 *   <li>Implement pooling for frequent packet creation</li>
 *   <li>Handle transport-specific requirements</li>
 *   <li>Ensure thread-safe operations</li>
 *   <li>Implement proper cleanup mechanisms</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * // Create a new packet
 * Packet packet = PacketImpl.newInstance();
 * 
 * // Set packet data and properties
 * packet.setData(data);
 * packet.setTransportType(TransportType.TCP);
 * packet.setPriority(ResponsePriority.HIGH);
 * 
 * // Optional encryption
 * if (requiresEncryption) {
 *     packet.setEncrypted(true);
 * }
 * }</pre>
 * 
 * @see TransportType
 * @see ResponsePriority
 * @since 0.5.0
 */
public interface Packet {

  /**
   * IDs generator.
   */
  AtomicLong ID_COUNTER = new AtomicLong(1L);

  /**
   * Retrieves the unique ID of packet.
   *
   * @return the unique {@code long} value ID of packet
   */
  long getId();

  /**
   * Retrieves data of binaries conveyed by the packet.
   *
   * @return data of {@code byte} array conveyed by the packet
   */
  byte[] getData();

  /**
   * Puts data of binaries into the packet.
   *
   * @param binary data of {@code byte} array conveyed by the packet
   */
  void setData(byte[] binary);

  /**
   * Retrieves the transportation type of packet.
   *
   * @return the {@link TransportType} using by the packet
   */
  TransportType getTransportType();

  /**
   * Sets transportation type of the packet.
   *
   * @param transportType the {@link TransportType} using by the packet
   */
  void setTransportType(TransportType transportType);

  /**
   * Retrieves the priority of packet.
   *
   * @return the {@link ResponsePriority} set for the packet
   */
  ResponsePriority getPriority();

  /**
   * Sets priority of the packet.
   *
   * @param priority the {@link ResponsePriority} set for the packet
   */
  void setPriority(ResponsePriority priority);

  /**
   * Determines whether the packet data is encrypted.
   *
   * @return {@code true} if the packet data is encrypted, otherwise returns {@code false}
   */
  boolean isEncrypted();

  /**
   * Marks the packet data is encrypted or not.
   *
   * @param encrypted is set to {@code true} if the packet data is encrypted, otherwise
   *                  {@code false}
   */
  void setEncrypted(boolean encrypted);

  /**
   * Retrieves a collection of sessions which play roles as recipients.
   *
   * @return an unmodifiable collection of {@link Session}s and this can be empty
   * @see Collection
   */
  Collection<Session> getRecipients();

  /**
   * Sets a collection of sessions which play roles as recipients.
   *
   * @param recipients a collection of {@link Session}s and this can be empty
   * @see Collection
   */
  void setRecipients(Collection<Session> recipients);

  /**
   * Retrieves the creation time of packet in milliseconds.
   *
   * @return the creation time in milliseconds ({@code long} value)
   */
  long getCreatedTime();

  /**
   * Retrieves the real size of packet's data in bytes.
   *
   * @return the real packet's data size in {@code integer} value of bytes
   */
  int getOriginalSize();

  /**
   * Determines whether the packet's transportation type is TCP.
   *
   * @return {@code true} if the packet's transportation type is TCP, otherwise
   * {@code false}
   */
  boolean isTcp();

  /**
   * Determines whether the packet's transportation type is UDP.
   *
   * @return {@code true} if the packet's transportation type is UDP, otherwise
   * {@code false}
   */
  boolean isUdp();

  /**
   * Determines whether the packet's transportation type is WebSocket.
   *
   * @return {@code true} if the packet's transportation type is WebSocket, otherwise
   * {@code false}
   */
  boolean isWebSocket();

  /**
   * Retrieves the rest of sending binaries from the packet's data.
   *
   * @return the rest of sending {@code byte} array from the packet's data
   */
  byte[] getFragmentBuffer();

  /**
   * Updates the rest of sending binaries from the packet's data. The data may not be sent at
   * once, but it is split to some parts according to the sending process.
   *
   * @param binary the rest of sending {@code byte} array from the packet's data
   */
  void setFragmentBuffer(byte[] binary);

  /**
   * Determines whether the packet's data is fragmented.
   *
   * @return {@code true} if there is fragmented binaries data waiting to send (In case, the
   * packet's data could not be sent at once), otherwise returns {@code false}
   */
  boolean isFragmented();

  /**
   * Determines whether the packet is the last one or not. In case this is the last sent packet,
   * it will close the connection.
   *
   * @return {@code true} if the packet is the last sent one, otherwise returns {@code false}
   * @since 0.5.0
   */
  boolean isMarkedAsLast();

  /**
   * Marks this packet as the last one. After sending it, the session will be closed.
   *
   * @param markedAsLast determines if the packet is the last one or not
   * @since 0.5.0
   */
  void setMarkedAsLast(boolean markedAsLast);

  /**
   * Retrieves the Packet's clone instance.
   *
   * @return the {@link Packet}'s clone instance
   */
  Packet clone();
}
