/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.entity.outbound.packet.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tenio.core.exception.OutboundQueueFullException;
import com.tenio.core.exception.OutboundQueuePolicyViolationException;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.implement.OutboundQueueImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DefaultOutboundQueuePolicyTest {

  private DefaultOutboundQueuePolicy policy;

  @BeforeEach
  void setUp() {
    policy = new DefaultOutboundQueuePolicy();
  }

  @Test
  void testApplyPolicy() {
    DefaultOutboundQueuePolicy defaultOutboundQueuePolicy = new DefaultOutboundQueuePolicy();
    OutboundQueueImpl newInstanceResult = OutboundQueueImpl.newInstance();
    assertThrows(OutboundQueueFullException.class, () -> defaultOutboundQueuePolicy.applyPolicy(newInstanceResult, mock(Packet.class)));
  }

  @Test
  void testAlmostFullQueueThrowsOutboundQueueFullException() {
    OutboundQueue queue = mock(OutboundQueue.class);
    when(queue.isSnapshotFull()).thenReturn(true);
    when(queue.getSnapshotSize()).thenReturn(5);

    assertThrows(OutboundQueueFullException.class,
        () -> policy.applyPolicy(queue, mock(Packet.class)));
  }

  @Test
  void testBetween75And90PercentWithLowPriorityThrowsPolicyViolation() {
    OutboundQueue queue = mock(OutboundQueue.class);
    Packet packet = mock(Packet.class);
    when(queue.isSnapshotFull()).thenReturn(false);
    when(queue.getPercentageUsed()).thenReturn(80.0f);
    when(packet.getDeliveryOptions()).thenReturn(DeliveryOptions.bestEffort(java.time.Duration.ZERO));

    assertThrows(OutboundQueuePolicyViolationException.class,
        () -> policy.applyPolicy(queue, packet));
  }

  @Test
  void testBetween75And90PercentWithNormalPriorityDoesNotThrow() {
    OutboundQueue queue = mock(OutboundQueue.class);
    Packet packet = mock(Packet.class);
    when(queue.isSnapshotFull()).thenReturn(false);
    when(queue.getPercentageUsed()).thenReturn(80.0f);
    when(packet.getDeliveryOptions()).thenReturn(null);

    assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
  }

  @Test
  void testAt90PercentOrAboveWithNormalPriorityThrowsPolicyViolation() {
    OutboundQueue queue = mock(OutboundQueue.class);
    Packet packet = mock(Packet.class);
    when(queue.isSnapshotFull()).thenReturn(false);
    when(queue.getPercentageUsed()).thenReturn(92.0f);
    when(packet.getDeliveryOptions()).thenReturn(null);

    assertThrows(OutboundQueuePolicyViolationException.class,
        () -> policy.applyPolicy(queue, packet));
  }

  @Test
  void testAt90PercentOrAboveWithHighPriorityDoesNotThrow() {
    OutboundQueue queue = mock(OutboundQueue.class);
    Packet packet = mock(Packet.class);
    when(queue.isSnapshotFull()).thenReturn(false);
    when(queue.getPercentageUsed()).thenReturn(92.0f);
    when(packet.getDeliveryOptions()).thenReturn(DeliveryOptions.important());

    assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
  }

  @Test
  void testBelow75PercentWithLowPriorityDoesNotThrow() {
    OutboundQueue queue = mock(OutboundQueue.class);
    Packet packet = mock(Packet.class);
    when(queue.isSnapshotFull()).thenReturn(false);
    when(queue.getPercentageUsed()).thenReturn(50.0f);
    when(packet.getDeliveryOptions()).thenReturn(DeliveryOptions.bestEffort(java.time.Duration.ZERO));

    assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
  }

  @ParameterizedTest
  @CsvSource({
      "LOW,0,false", "LOW,74.9,false", "LOW,75,true", "LOW,89.9,true", "LOW,90,true",
      "LOW,99.9,true", "NORMAL,0,false", "NORMAL,74.9,false", "NORMAL,75,false",
      "NORMAL,89.9,false", "NORMAL,90,true", "NORMAL,99.9,true", "HIGH,0,false",
      "HIGH,74.9,false", "HIGH,75,false", "HIGH,89.9,false", "HIGH,90,false", "HIGH,99.9,false"
  })
  void congestionBoundariesUseOnlyTheSelectedDeliveryPriority(DeliveryOptions.Priority priority,
                                                              float occupancy, boolean rejected) {
    var queue = mock(OutboundQueue.class);
    var packet = mock(Packet.class);
    when(queue.getPercentageUsed()).thenReturn(occupancy);
    when(packet.getDeliveryOptions()).thenReturn(
        DeliveryOptions.replaceable("state", java.time.Duration.ZERO).withPriority(priority));
    if (rejected) {
      assertThrows(OutboundQueuePolicyViolationException.class, () -> policy.applyPolicy(queue, packet));
    } else {
      assertDoesNotThrow(() -> policy.applyPolicy(queue, packet));
    }
  }

  @ParameterizedTest
  @EnumSource(DeliveryOptions.Priority.class)
  void noPriorityBypassesFullQueues(DeliveryOptions.Priority priority) {
    var queue = mock(OutboundQueue.class);
    var packet = mock(Packet.class);
    when(queue.isSnapshotFull()).thenReturn(true);
    when(packet.getDeliveryOptions()).thenReturn(DeliveryOptions.important().withPriority(priority));
    assertThrows(OutboundQueueFullException.class, () -> policy.applyPolicy(queue, packet));
  }

  @Test
  void uncongestedQueuesDoNotInspectOrAllocatePacketDeliverySettings() {
    var queue = mock(OutboundQueue.class);
    var packet = mock(Packet.class);
    when(queue.getPercentageUsed()).thenReturn(50.0f);
    policy.applyPolicy(queue, packet);
    verifyNoInteractions(packet);
  }
}
