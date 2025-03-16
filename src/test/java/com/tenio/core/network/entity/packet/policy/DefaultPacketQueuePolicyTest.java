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

@DisplayName("Unit Test Cases For DefaultPacketQueuePolicy")
@ExtendWith(MockitoExtension.class)
public class DefaultPacketQueuePolicyTest {

    private DefaultPacketQueuePolicy policy;
    private PacketQueue queue;
    private Packet packet;
    private static final int QUEUE_SIZE = 100;

    @BeforeEach
    void setUp() {
        policy = new DefaultPacketQueuePolicy();
        queue = PacketQueueImpl.newInstance();
        ((PacketQueueImpl) queue).configureMaxSize(QUEUE_SIZE);
        packet = mock(Packet.class);
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
        assertThrows(NullPointerException.class, () -> policy.applyPolicy(null, packet));
    }

    @Test
    @DisplayName("Policy should handle regular packet")
    void testApplyPolicyWithRegularPacket() {
        when(packet.getPriority()).thenReturn(ResponsePriority.NORMAL);
        policy.applyPolicy(queue, packet);
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.getSize());
    }

    @Test
    @DisplayName("Policy should handle multiple packets with different priorities")
    void testApplyPolicyWithMultiplePackets() {
        Packet highPriorityPacket = createTestPacket(ResponsePriority.GUARANTEED_QUICKEST);
        Packet normalPriorityPacket = createTestPacket(ResponsePriority.NORMAL);
        Packet lowPriorityPacket = createTestPacket(ResponsePriority.NON_GUARANTEED);

        // Apply policies and add packets to queue
        policy.applyPolicy(queue, normalPriorityPacket);
        queue.put(normalPriorityPacket);
        
        policy.applyPolicy(queue, highPriorityPacket);
        queue.put(highPriorityPacket);
        
        // This should throw an exception since queue is not empty and priority is NON_GUARANTEED
        assertThrows(PacketQueuePolicyViolationException.class, 
                     () -> policy.applyPolicy(queue, lowPriorityPacket));

        assertEquals(2, queue.getSize());
        
        // Verify packets are in the queue
        assertNotNull(queue.take());
        assertNotNull(queue.take());
        assertTrue(queue.isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Policy should handle all priority types")
    @EnumSource(ResponsePriority.class)
    void testApplyPolicyWithAllPriorities(ResponsePriority priority) {
        // For NON_GUARANTEED priority, we need to check if the queue is empty
        if (priority == ResponsePriority.NON_GUARANTEED && !queue.isEmpty()) {
            assertThrows(PacketQueuePolicyViolationException.class, () -> {
                Packet testPacket = createTestPacket(priority);
                policy.applyPolicy(queue, testPacket);
            });
        } else {
            Packet testPacket = createTestPacket(priority);
            policy.applyPolicy(queue, testPacket);
            queue.put(testPacket);
            assertEquals(1, queue.getSize());
            assertEquals(priority, queue.take().getPriority());
        }
    }

  @Test
    @DisplayName("Policy should handle packets with same priority")
    void testApplyPolicyWithSamePriority() {
        Packet packet1 = createTestPacket(ResponsePriority.NORMAL);
        Packet packet2 = createTestPacket(ResponsePriority.NORMAL);

        policy.applyPolicy(queue, packet1);
        policy.applyPolicy(queue, packet2);

        assertEquals(2, queue.getSize());
        assertNotNull(queue.take());
        assertNotNull(queue.take());
        assertTrue(queue.isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Policy should handle all transport types")
    @EnumSource(TransportType.class)
    void testApplyPolicyWithAllTransportTypes(TransportType transportType) {
        Packet packet = createTestPacketWithTransport(ResponsePriority.NORMAL, transportType);
        policy.applyPolicy(queue, packet);
        assertEquals(1, queue.getSize());
        assertEquals(transportType, queue.take().getTransportType());
  }

  @Test
    @DisplayName("Policy should handle full queue")
    void testApplyPolicyWithFullQueue() {
        // Fill the queue
        for (int i = 0; i < QUEUE_SIZE; i++) {
            policy.applyPolicy(queue, createTestPacket(ResponsePriority.NORMAL));
        }
        
        // Try to add one more packet
        Packet extraPacket = createTestPacket(ResponsePriority.NORMAL);
        assertThrows(PacketQueueFullException.class, () -> policy.applyPolicy(queue, extraPacket));
    }

    @Test
    @DisplayName("Policy should handle packets with null data")
    void testApplyPolicyWithNullData() {
        Packet packet = PacketImpl.newInstance();
        packet.setPriority(ResponsePriority.NORMAL);
        packet.setTransportType(TransportType.TCP);
        // Not setting data
        
        policy.applyPolicy(queue, packet);
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
        assertEquals(1, queue.getSize());
        assertEquals(0, queue.take().getData().length);
    }

    @Test
    @DisplayName("Apply policy should allow packet when queue is empty")
    void applyPolicyShouldAllowPacketWhenQueueIsEmpty() {
        when(queue.isEmpty()).thenReturn(true);
        when(packet.getPriority()).thenReturn(ResponsePriority.NORMAL);

        assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
    }

    @Test
    @DisplayName("Apply policy should allow packet with GUARANTEED_QUICKEST priority")
    void applyPolicyShouldAllowPacketWithGuaranteedQuickestPriority() {
        when(queue.isEmpty()).thenReturn(false);
        when(packet.getPriority()).thenReturn(ResponsePriority.GUARANTEED_QUICKEST);

        assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
    }

    @Test
    @DisplayName("Apply policy should allow packet with GUARANTEED priority")
    void applyPolicyShouldAllowPacketWithGuaranteedPriority() {
        when(queue.isEmpty()).thenReturn(false);
        when(packet.getPriority()).thenReturn(ResponsePriority.GUARANTEED);

        assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
    }

    @Test
    @DisplayName("Apply policy should allow packet with NORMAL priority")
    void applyPolicyShouldAllowPacketWithNormalPriority() {
        when(queue.isEmpty()).thenReturn(false);
        when(packet.getPriority()).thenReturn(ResponsePriority.NORMAL);

        assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
    }

    @Test
    @DisplayName("Apply policy should throw exception for NON_GUARANTEED priority when queue is not empty")
    void applyPolicyShouldThrowExceptionForNonGuaranteedPriorityWhenQueueIsNotEmpty() {
        when(queue.isEmpty()).thenReturn(false);
        when(packet.getPriority()).thenReturn(ResponsePriority.NON_GUARANTEED);

        assertThrows(PacketQueuePolicyViolationException.class, () -> policy.applyPolicy(queue, packet));
    }

    @Test
    @DisplayName("Apply policy should allow NON_GUARANTEED priority when queue is empty")
    void applyPolicyShouldAllowNonGuaranteedPriorityWhenQueueIsEmpty() {
        when(queue.isEmpty()).thenReturn(true);
        when(packet.getPriority()).thenReturn(ResponsePriority.NON_GUARANTEED);

        assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
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
