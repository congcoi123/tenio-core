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

package com.tenio.core.network.zero.codec.packet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.implement.PacketImpl;
import com.tenio.core.network.entity.packet.implement.PacketQueueImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For PacketQueue")
class PacketQueueTest {

  private PacketQueue packetQueue;

  @BeforeEach
  void setUp() {
    packetQueue = PacketQueueImpl.newInstance();
  }

  @Test
  @DisplayName("New instance should create empty queue")
  void newInstanceShouldCreateEmptyQueue() {
    assertAll("newInstanceShouldCreateEmptyQueue",
        () -> assertNotNull(packetQueue),
        () -> assertTrue(packetQueue.isEmpty()),
        () -> assertEquals(0, packetQueue.getSize())
    );
  }

  @Test
  @DisplayName("Clear should remove all packets")
  void clearShouldRemoveAllPackets() {
    // Add some packets
    Packet packet1 = PacketImpl.newInstance();
    packet1.setData(new byte[] {1, 2, 3});
    Packet packet2 = PacketImpl.newInstance();
    packet2.setData(new byte[] {4, 5, 6});
    
    packetQueue.put(packet1);
    packetQueue.put(packet2);
    
    // Clear the queue
    packetQueue.clear();
    
    assertAll("clearShouldRemoveAllPackets",
        () -> assertTrue(packetQueue.isEmpty()),
        () -> assertEquals(0, packetQueue.getSize())
    );
  }

  @Test
  @DisplayName("Put should increase size")
  void putShouldIncreaseSize() {
    Packet packet = PacketImpl.newInstance();
    packet.setData(new byte[] {1, 2, 3});
    
    packetQueue.put(packet);
    
    assertAll("putShouldIncreaseSize",
        () -> assertFalse(packetQueue.isEmpty()),
        () -> assertEquals(1, packetQueue.getSize())
    );
  }

  @Test
  @DisplayName("Take should return and remove first packet")
  void takeShouldReturnAndRemoveFirstPacket() {
    Packet packet1 = PacketImpl.newInstance();
    packet1.setData(new byte[] {1, 2, 3});
    Packet packet2 = PacketImpl.newInstance();
    packet2.setData(new byte[] {4, 5, 6});
    
    packetQueue.put(packet1);
    packetQueue.put(packet2);
    
    Packet takenPacket = packetQueue.take();
    
    assertAll("takeShouldReturnAndRemoveFirstPacket",
        () -> assertEquals(packet1, takenPacket),
        () -> assertEquals(1, packetQueue.getSize())
    );
  }

  @Test
  @DisplayName("Peek should return but not remove first packet")
  void peekShouldReturnButNotRemoveFirstPacket() {
    Packet packet1 = PacketImpl.newInstance();
    packet1.setData(new byte[] {1, 2, 3});
    Packet packet2 = PacketImpl.newInstance();
    packet2.setData(new byte[] {4, 5, 6});
    
    packetQueue.put(packet1);
    packetQueue.put(packet2);
    
    Packet peekedPacket = packetQueue.peek();
    
    assertAll("peekShouldReturnButNotRemoveFirstPacket",
        () -> assertEquals(packet1, peekedPacket),
        () -> assertEquals(2, packetQueue.getSize())
    );
  }
} 