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

package com.tenio.core.network.entity.packet.policy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.implement.PacketImpl;
import com.tenio.core.network.entity.packet.implement.PacketQueueImpl;
import com.tenio.core.exception.PacketQueueFullException;
import com.tenio.core.exception.PacketQueuePolicyViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test Cases For DefaultPacketQueuePolicy")
class DefaultPacketQueuePolicyTest {

    private DefaultPacketQueuePolicy policy;
    private PacketQueue queue;
    private static final int QUEUE_SIZE = 100;

    @BeforeEach
    void setUp() {
        policy = new DefaultPacketQueuePolicy();
        queue = PacketQueueImpl.newInstance();
        queue.configureMaxSize(QUEUE_SIZE);
    }

    @Test
    @DisplayName("Policy should handle null packet")
    void testApplyPolicyWithNullPacket() {
        policy.applyPolicy(queue, null);
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.getSize());
    }

    @Test
    @DisplayName("Policy should handle null queue")
    void testApplyPolicyWithNullQueue() {
        Packet packet = createTestPacket(ResponsePriority.NORMAL);
        assertThrows(NullPointerException.class, () -> policy.applyPolicy(null, packet));
    }

    @Test
    @DisplayName("Policy should handle regular packet")
    void testApplyPolicyWithRegularPacket() {
        Packet packet = createTestPacket(ResponsePriority.NORMAL);
        policy.applyPolicy(queue, packet);
        queue.put(packet);
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.getSize());
        Packet takenPacket = queue.take();
        assertNotNull(takenPacket);
        assertEquals(ResponsePriority.NORMAL, takenPacket.getPriority());
    }

    @Test
    @DisplayName("Policy should handle multiple packets with different priorities")
    void testApplyPolicyWithMultiplePackets() {
        Packet highPriorityPacket = createTestPacket(ResponsePriority.GUARANTEED_QUICKEST);
        Packet normalPriorityPacket = createTestPacket(ResponsePriority.NORMAL);
        Packet lowPriorityPacket = createTestPacket(ResponsePriority.NON_GUARANTEED);

        // Add normal priority packet
        policy.applyPolicy(queue, normalPriorityPacket);
        queue.put(normalPriorityPacket);
        
        // Add high priority packet
        policy.applyPolicy(queue, highPriorityPacket);
        queue.put(highPriorityPacket);
        
        // This should throw an exception since queue is not empty and priority is NON_GUARANTEED
        assertThrows(PacketQueuePolicyViolationException.class, 
                     () -> policy.applyPolicy(queue, lowPriorityPacket));

        assertEquals(2, queue.getSize());
        
        // Verify packets are in the queue
        Packet firstPacket = queue.take();
        Packet secondPacket = queue.take();
        assertNotNull(firstPacket);
        assertNotNull(secondPacket);
        assertTrue(queue.isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Policy should handle all priority types")
    @EnumSource(ResponsePriority.class)
    void testApplyPolicyWithAllPriorities(ResponsePriority priority) {
        Packet packet = createTestPacket(priority);
        
        if (priority == ResponsePriority.NON_GUARANTEED && !queue.isEmpty()) {
            assertThrows(PacketQueuePolicyViolationException.class, () -> policy.applyPolicy(queue, packet));
        } else {
            policy.applyPolicy(queue, packet);
            queue.put(packet);
            assertEquals(1, queue.getSize());
            Packet takenPacket = queue.take();
            assertNotNull(takenPacket);
            assertEquals(priority, takenPacket.getPriority());
            assertTrue(queue.isEmpty());
        }
    }

    @ParameterizedTest
    @DisplayName("Policy should handle all transport types")
    @EnumSource(TransportType.class)
    void testApplyPolicyWithAllTransportTypes(TransportType transportType) {
        Packet packet = createTestPacketWithTransport(ResponsePriority.NORMAL, transportType);
        policy.applyPolicy(queue, packet);
        queue.put(packet);
        assertEquals(1, queue.getSize());
        Packet takenPacket = queue.take();
        assertNotNull(takenPacket);
        assertEquals(transportType, takenPacket.getTransportType());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Policy should handle packets with same priority")
    void testApplyPolicyWithSamePriority() {
        Packet packet1 = createTestPacket(ResponsePriority.NORMAL);
        Packet packet2 = createTestPacket(ResponsePriority.NORMAL);

        policy.applyPolicy(queue, packet1);
        queue.put(packet1);
        policy.applyPolicy(queue, packet2);
        queue.put(packet2);

        assertEquals(2, queue.getSize());
        assertNotNull(queue.take());
        assertNotNull(queue.take());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Policy should handle full queue")
    void testApplyPolicyWithFullQueue() {
        // Fill the queue to 90% capacity
        int fillCount = (int)(QUEUE_SIZE * 0.9);
        for (int i = 0; i < fillCount; i++) {
            Packet packet = createTestPacket(ResponsePriority.NORMAL);
            policy.applyPolicy(queue, packet);
            queue.put(packet);
        }
        
        // Try to add a NON_GUARANTEED packet
        Packet lowPriorityPacket = createTestPacket(ResponsePriority.NON_GUARANTEED);
        assertThrows(PacketQueuePolicyViolationException.class, () -> policy.applyPolicy(queue, lowPriorityPacket));
    }

    @Test
    @DisplayName("Policy should handle packets with null data")
    void testApplyPolicyWithNullData() {
        Packet packet = PacketImpl.newInstance();
        packet.setPriority(ResponsePriority.NORMAL);
        packet.setTransportType(TransportType.TCP);
        
        policy.applyPolicy(queue, packet);
        queue.put(packet);
        assertEquals(1, queue.getSize());
        assertNull(queue.take().getData());
    }

    @Test
    @DisplayName("Policy should handle packets with empty data")
    void testApplyPolicyWithEmptyData() {
        Packet packet = PacketImpl.newInstance();
        packet.setData(new byte[0]);
        packet.setPriority(ResponsePriority.NORMAL);
        packet.setTransportType(TransportType.TCP);
        
        policy.applyPolicy(queue, packet);
        queue.put(packet);
        assertEquals(1, queue.getSize());
        assertEquals(0, queue.take().getData().length);
    }

    private Packet createTestPacket(ResponsePriority priority) {
        return createTestPacketWithTransport(priority, TransportType.TCP);
    }

    private Packet createTestPacketWithTransport(ResponsePriority priority, TransportType transportType) {
        Packet packet = PacketImpl.newInstance();
        packet.setData(new byte[100]);
        packet.setPriority(priority);
        packet.setTransportType(transportType);
        return packet;
    }
}
