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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.policy.DefaultPacketQueuePolicy;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import com.tenio.core.exception.PacketQueueFullException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the PacketQueueImpl class.
 */
@DisplayName("Unit Test Cases For PacketQueueImpl")
class PacketQueueImplTest {

    private PacketQueueImpl queue;
    private PacketQueuePolicy policy;
    private static final int MAX_SIZE = 100;

    @BeforeEach
    void setUp() {
        queue = PacketQueueImpl.newInstance();
        policy = new DefaultPacketQueuePolicy();
        queue.configureMaxSize(MAX_SIZE);
        queue.configurePacketQueuePolicy(policy);
  }

  @Test
    @DisplayName("New instance should create empty queue")
    void newInstanceShouldCreateEmptyQueue() {
        assertAll("newInstanceShouldCreateEmptyQueue",
            () -> assertNotNull(queue),
            () -> assertTrue(queue.isEmpty()),
            () -> assertEquals(0, queue.getSize()),
            () -> assertFalse(queue.isFull()),
            () -> assertEquals(0.0f, queue.getPercentageUsed())
        );
  }

  @Test
    @DisplayName("Configure max size should update size limit")
    void configureMaxSizeShouldUpdateSizeLimit() {
        int newMaxSize = 50;
        queue.configureMaxSize(newMaxSize);
        
        Packet packet = PacketImpl.newInstance();
        for (int i = 0; i < newMaxSize; i++) {
            queue.put(packet);
        }
        
        assertAll("configureMaxSizeShouldUpdateSizeLimit",
            () -> assertTrue(queue.isFull()),
            () -> assertEquals(100.0f, queue.getPercentageUsed()),
            () -> assertThrows(PacketQueueFullException.class, () -> queue.put(packet))
        );
    }

    @Test
    @DisplayName("Configure packet queue policy should set policy")
    void configurePacketQueuePolicyShouldSetPolicy() {
        PacketQueuePolicy policy = mock(PacketQueuePolicy.class);
        Packet packet = PacketImpl.newInstance();
        
        queue.configurePacketQueuePolicy(policy);
        queue.put(packet);
        
        verify(policy).applyPolicy(queue, packet);
    }

    @Test
    @DisplayName("Put should add packet to queue")
    void putShouldAddPacketToQueue() {
        Packet packet = PacketImpl.newInstance();
        packet.setPriority(ResponsePriority.NORMAL);
        
        queue.put(packet);
        
        assertAll("putShouldAddPacketToQueue",
            () -> assertFalse(queue.isEmpty()),
            () -> assertEquals(1, queue.getSize()),
            () -> assertEquals(packet, queue.peek())
        );
    }

    @Test
    @DisplayName("Take should remove and return packet")
    void takeShouldRemoveAndReturnPacket() {
        Packet packet = PacketImpl.newInstance();
        queue.put(packet);
        
        Packet takenPacket = queue.take();
        
        assertAll("takeShouldRemoveAndReturnPacket",
            () -> assertEquals(packet, takenPacket),
            () -> assertTrue(queue.isEmpty()),
            () -> assertEquals(0, queue.getSize())
        );
    }

    @Test
    @DisplayName("Take from empty queue should return null")
    void takeFromEmptyQueueShouldReturnNull() {
        assertNull(queue.take());
    }

    @Test
    @DisplayName("Peek should return but not remove packet")
    void peekShouldReturnButNotRemovePacket() {
        Packet packet = PacketImpl.newInstance();
        queue.put(packet);
        
        Packet peekedPacket = queue.peek();
        
        assertAll("peekShouldReturnButNotRemovePacket",
            () -> assertEquals(packet, peekedPacket),
            () -> assertFalse(queue.isEmpty()),
            () -> assertEquals(1, queue.getSize())
        );
    }

    @Test
    @DisplayName("Peek from empty queue should return null")
    void peekFromEmptyQueueShouldReturnNull() {
        assertNull(queue.peek());
    }

    @Test
    @DisplayName("Clear should remove all packets")
    void clearShouldRemoveAllPackets() {
        Packet packet1 = PacketImpl.newInstance();
        Packet packet2 = PacketImpl.newInstance();
        
        queue.put(packet1);
        queue.put(packet2);
        queue.clear();
        
        assertAll("clearShouldRemoveAllPackets",
            () -> assertTrue(queue.isEmpty()),
            () -> assertEquals(0, queue.getSize()),
            () -> assertEquals(0.0f, queue.getPercentageUsed())
        );
    }

    @Test
    @DisplayName("Queue should handle packet addition correctly")
    void testPacketAddition() {
        Packet packet = createTestPacket();
        policy.applyPolicy(queue, packet);
        
        assertEquals(1, queue.getSize());
        assertFalse(queue.isEmpty());
        assertEquals(1.0f / MAX_SIZE * 100, queue.getPercentageUsed());
    }

    @Test
    @DisplayName("Queue should handle packet removal correctly")
    void testPacketRemoval() {
        Packet packet = createTestPacket();
        policy.applyPolicy(queue, packet);
        
        Packet removed = queue.take();
        assertNotNull(removed);
        assertEquals(0, queue.getSize());
        assertTrue(queue.isEmpty());
        assertEquals(0.0f, queue.getPercentageUsed());
    }

    @ParameterizedTest
    @DisplayName("Queue should handle different sizes correctly")
    @ValueSource(ints = {1, 10, 50, 100})
    void testQueueSizes(int size) {
        for (int i = 0; i < size; i++) {
            policy.applyPolicy(queue, createTestPacket());
        }
        
        assertEquals(size, queue.getSize());
        assertEquals((float) size / MAX_SIZE * 100, queue.getPercentageUsed());
  }

  @Test
    @DisplayName("Queue should handle clear operation correctly")
  void testClear() {
        for (int i = 0; i < 10; i++) {
            policy.applyPolicy(queue, createTestPacket());
        }
        
        queue.clear();
        assertEquals(0, queue.getSize());
        assertTrue(queue.isEmpty());
        assertEquals(0.0f, queue.getPercentageUsed());
    }

    @Test
    @DisplayName("Queue should handle priority-based ordering")
    void testPriorityOrdering() {
        // Add packets with different priorities
        Packet highPriority = createTestPacket(ResponsePriority.GUARANTEED_QUICKEST);
        Packet normalPriority = createTestPacket(ResponsePriority.NORMAL);
        Packet lowPriority = createTestPacket(ResponsePriority.NON_GUARANTEED);
        
        policy.applyPolicy(queue, normalPriority);
        policy.applyPolicy(queue, lowPriority);
        policy.applyPolicy(queue, highPriority);
        
        // High priority should come out first
        assertEquals(ResponsePriority.GUARANTEED_QUICKEST, queue.take().getPriority());
        assertEquals(ResponsePriority.NORMAL, queue.take().getPriority());
        assertEquals(ResponsePriority.NON_GUARANTEED, queue.take().getPriority());
    }

    @Test
    @DisplayName("Queue should handle maximum size limit")
    void testMaxSizeLimit() {
        // Fill queue to maximum
        for (int i = 0; i < MAX_SIZE; i++) {
            policy.applyPolicy(queue, createTestPacket());
        }
        
        assertTrue(queue.isFull());
        assertEquals(MAX_SIZE, queue.getSize());
        assertEquals(100.0f, queue.getPercentageUsed());
        
        // Attempting to add one more packet should throw exception
        assertThrows(PacketQueueFullException.class, () -> 
            policy.applyPolicy(queue, createTestPacket()));
    }

    @Test
    @DisplayName("Queue should handle peek operation correctly")
    void testPeek() {
        assertTrue(queue.isEmpty());
        assertNull(queue.peek());
        
        Packet packet = createTestPacket();
        policy.applyPolicy(queue, packet);
        
        Packet peeked = queue.peek();
        assertNotNull(peeked);
        assertEquals(packet, peeked);
        assertEquals(1, queue.getSize()); // Size should not change after peek
    }

    @Test
    @DisplayName("Queue should maintain order with same priority packets")
    void testSamePriorityOrder() {
        Packet packet1 = createTestPacket(ResponsePriority.NORMAL);
        Packet packet2 = createTestPacket(ResponsePriority.NORMAL);
        Packet packet3 = createTestPacket(ResponsePriority.NORMAL);
        
        policy.applyPolicy(queue, packet1);
        policy.applyPolicy(queue, packet2);
        policy.applyPolicy(queue, packet3);
        
        assertEquals(3, queue.getSize());
        assertNotNull(queue.take());
        assertNotNull(queue.take());
        assertNotNull(queue.take());
        assertTrue(queue.isEmpty());
    }

    private Packet createTestPacket() {
        return createTestPacket(ResponsePriority.NORMAL);
    }

    private Packet createTestPacket(ResponsePriority priority) {
        Packet packet = PacketImpl.newInstance();
        packet.setData(new byte[100]);
        packet.setPriority(priority);
        packet.setTransportType(TransportType.TCP);
        return packet;
  }
}
