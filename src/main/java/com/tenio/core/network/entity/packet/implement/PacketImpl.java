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

package com.tenio.core.network.entity.packet.implement;

import com.tenio.common.utility.TimeUtility;
import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The implementation for packet.
 *
 * @see Packet
 */
public final class PacketImpl implements Packet, Comparable<Packet>, Cloneable {

  private static final AtomicLong ID_COUNTER = new AtomicLong();

  private final long id;
  private final long createdTime;
  private byte[] data;
  private ResponsePriority priority;
  private boolean encrypted;
  private TransportType transportType;
  private int originalSize;
  private Collection<Session> recipients;
  private byte[] fragmentBuffer;

  private PacketImpl() {
    id = ID_COUNTER.getAndIncrement();
    createdTime = TimeUtility.currentTimeMillis();
    transportType = TransportType.UNKNOWN;
    priority = ResponsePriority.NORMAL;
    encrypted = false;
  }

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
    return Objects.nonNull(fragmentBuffer);
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof Packet)) {
      return false;
    } else {
      var packet = (Packet) object;
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
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public int compareTo(Packet packet2) {
    var packet1 = this;
    return Integer.compare(packet1.getPriority().getValue(), packet2.getPriority().getValue()) != 0
        ? Integer.compare(packet1.getPriority().getValue(), packet2.getPriority().getValue())
        : Long.compare(packet2.getId(), packet1.getId());
  }

  @Override
  public String toString() {
    return String.format(
        "{ id: %d, createdTime: %d, transportType: %s, priority: %s, encrypted: %b }", id,
        createdTime, transportType.toString(), priority.toString(), encrypted);
  }

  @Override
  public Packet clone() {
    var packet = PacketImpl.newInstance();
    packet.setData(data);
    packet.setFragmentBuffer(fragmentBuffer);
    packet.setPriority(priority);
    packet.setEncrypted(encrypted);
    packet.setRecipients(recipients);
    packet.setTransportType(transportType);
    return packet;
  }
}
