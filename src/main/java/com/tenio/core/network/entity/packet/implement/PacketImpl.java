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

package com.tenio.core.network.entity.packet.implement;

import com.tenio.common.utility.TimeUtility;
import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;

/**
 * The implementation of the {@link Packet} interface. This class represents a network packet
 * that can be transmitted between server and clients. It supports various transport types,
 * priorities, and can handle both encrypted and unencrypted data. The packet can be fragmented
 * for large data transmission and supports multiple recipients.
 * 
 * <p>Key features:
 * <ul>
 * <li>Unique packet identification</li>
 * <li>Binary data handling</li>
 * <li>Transport type specification (TCP, UDP, WebSocket)</li>
 * <li>Priority-based handling</li>
 * <li>Encryption support</li>
 * <li>Multiple recipient support</li>
 * <li>Packet fragmentation for large data</li>
 * </ul>
 * 
 * @see Packet
 * @see TransportType
 * @see ResponsePriority
 * @see Session
 */
public final class PacketImpl implements Packet, Comparable<Packet>, Cloneable {

  private final long id;
  private final long createdTime;
  private byte[] data;
  private ResponsePriority priority;
  private boolean encrypted;
  private TransportType transportType;
  private int originalSize;
  private Collection<Session> recipients;
  private byte[] fragmentBuffer;
  private boolean last;

  private PacketImpl() {
    id = ID_COUNTER.getAndIncrement();
    createdTime = TimeUtility.currentTimeMillis();
    transportType = TransportType.UNKNOWN;
    priority = ResponsePriority.NORMAL;
  }

  /**
   * Creates a new instance of packet with default settings:
   * <ul>
   * <li>Unique ID generated from atomic counter</li>
   * <li>Creation timestamp set to current time</li>
   * <li>Transport type set to UNKNOWN</li>
   * <li>Priority set to NORMAL</li>
   * </ul>
   *
   * @return a new instance of {@link Packet}
   */
  public static Packet newInstance() {
    return new PacketImpl();
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public byte[] getData() {
    return data;
  }

  @Override
  public void setData(byte[] binary) {
    data = binary;
    originalSize = binary.length;
  }

  @Override
  public TransportType getTransportType() {
    return transportType;
  }

  @Override
  public void setTransportType(TransportType type) {
    transportType = type;
  }

  @Override
  public ResponsePriority getPriority() {
    return priority;
  }

  @Override
  public void setPriority(ResponsePriority priority) {
    this.priority = priority;
  }

  @Override
  public boolean isEncrypted() {
    return encrypted;
  }

  @Override
  public void setEncrypted(boolean encrypted) {
    this.encrypted = encrypted;
  }

  @Override
  public Collection<Session> getRecipients() {
    return recipients;
  }

  @Override
  public void setRecipients(Collection<Session> recipients) {
    this.recipients = recipients;
  }

  @Override
  public long getCreatedTime() {
    return createdTime;
  }

  @Override
  public int getOriginalSize() {
    return originalSize;
  }

  @Override
  public boolean isTcp() {
    return transportType == TransportType.TCP;
  }

  @Override
  public boolean isUdp() {
    return transportType == TransportType.UDP;
  }

  @Override
  public boolean isWebSocket() {
    return transportType == TransportType.WEB_SOCKET;
  }

  @Override
  public byte[] getFragmentBuffer() {
    return fragmentBuffer;
  }

  @Override
  public void setFragmentBuffer(byte[] binary) {
    fragmentBuffer = binary;
  }

  @Override
  public boolean isFragmented() {
    return fragmentBuffer != null && fragmentBuffer.length > 0;
  }

  @Override
  public boolean isMarkedAsLast() {
    return last;
  }

  @Override
  public void setMarkedAsLast(boolean markedAsLast) {
    last = markedAsLast;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Packet packet)) {
      return false;
    } else {
      return getId() == packet.getId();
    }
  }

  /**
   * It is generally necessary to override the <b>hashCode</b> method whenever
   * equals method is overridden, so as to maintain the general contract for the
   * hashCode method, which states that equal objects must have equal hash codes.
   *
   * @see <a href="https://imgur.com/x6rEAZE">Formula</a>
   */
  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public int compareTo(Packet packet) {
    return Long.compare(getId(), packet.getId());
  }

  @Override
  public String toString() {
    return "Packet{" +
        "id=" + id +
        ", createdTime=" + createdTime +
        ", data=" + (data == null ? "null" : data.length + " bytes") +
        ", priority=" + priority +
        ", encrypted=" + encrypted +
        ", transportType=" + transportType +
        ", originalSize=" + originalSize +
        ", recipients=" + (recipients == null ? "null" : recipients.size() + " recipients") +
        ", fragmentBuffer=" + (fragmentBuffer == null ? "null" : fragmentBuffer.length + " bytes") +
        ", last=" + last +
        '}';
  }

  @Override
  public Packet clone() {
    try {
      return (Packet) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
