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

import com.tenio.core.exception.PacketQueueFullException;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The implementation for packet queue.
 *
 * @see PacketQueue
 */
public final class PacketQueueImpl implements PacketQueue {

  private final TreeSet<Packet> queue;
  private final AtomicInteger size;
  // Non thread-safe, one-time setup
  private PacketQueuePolicy packetQueuePolicy;
  private int maxSize;

  /**
   * Constructor.
   *
   * @see PacketImpl#compareTo(Packet)
   */
  private PacketQueueImpl() {
    // Provide a comparator
    queue = new TreeSet<>();
    size = new AtomicInteger(0);
  }

  /**
   * Creates a new instance of a packet queue.
   *
   * @return a new instance of {@link PacketQueue}
   */
  public static PacketQueueImpl newInstance() {
    return new PacketQueueImpl();
  }

  @Override
  public Packet peek() {
    synchronized (queue) {
      if (!isEmpty()) {
        return queue.last();
      }
    }
    return null;
  }

  @Override
  public Packet take() {
    synchronized (queue) {
      if (!isEmpty()) {
        Packet packet = queue.pollLast();
        size.set(queue.size());
        return packet;
      }
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return size.get() == 0;
  }

  @Override
  public boolean isFull() {
    return size.get() >= maxSize;
  }

  @Override
  public int getSize() {
    return size.get();
  }

  @Override
  public int getMaxSize() {
    return maxSize;
  }

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  @Override
  public void setPacketQueuePolicy(PacketQueuePolicy packetQueuePolicy) {
    this.packetQueuePolicy = packetQueuePolicy;
  }

  @Override
  public float getPercentageUsed() {
    return getMaxSize() == 0 ? 0.0f : ((float) size.get() * 100) / getMaxSize();
  }

  @Override
  public void put(Packet packet) {
    if (isFull()) {
      throw new PacketQueueFullException(queue.size());
    }
    packetQueuePolicy.applyPolicy(this, packet);
    synchronized (queue) {
      queue.add(packet);
      size.set(queue.size());
    }
  }

  @Override
  public void clear() {
    synchronized (queue) {
      queue.clear();
      size.set(0);
    }
  }

  @Override
  public String toString() {
    return "PacketQueue{" +
        "queue=" + queue +
        ", packetQueuePolicy=" + packetQueuePolicy +
        ", maxSize=" + maxSize +
        ", size=" + size +
        '}';
  }
}
