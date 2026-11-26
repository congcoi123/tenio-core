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

package com.tenio.core.network.entity.outbound.packet.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.exception.OutboundQueueFullException;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeliveryPolicyTest {
  static OutboundQueueImpl queue(int size) {
    var q = OutboundQueueImpl.newInstance();
    q.configureMaxSize(size);
    q.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
    q.enableMonitoring(null, 1, "TCP");
    return q;
  }

  static Packet packet(int bytes, DeliveryOptions options) {
    var packet = PacketImpl.newInstance();
    packet.setData(new byte[bytes]);
    packet.setDeliveryOptions(options);
    return packet;
  }

  @Test
  void replaceableUpdateCanReplaceAtCapacityButImportantOrderIsKept() {
    var q = queue(2);
    var event = packet(2, DeliveryOptions.important());
    var old = packet(3, DeliveryOptions.replaceable("position:1", Duration.ZERO));
    var newest = packet(5, DeliveryOptions.replaceable("position:1", Duration.ZERO));
    q.put(event);
    q.put(old);
    q.put(newest);
    assertEquals(2, q.getSize());
    assertSame(event, q.take());
    assertSame(newest, q.take());
    assertEquals(1, q.deliveryStatistics().replaced());
    assertEquals(0, q.deliveryStatistics().queuedBytes());
  }

  @Test
  void writerClaimedAndFinalPacketsCannotBeReplaced() {
    var q = queue(1);
    var first = packet(3, DeliveryOptions.replaceable("position:1", Duration.ZERO));
    q.put(first);
    assertSame(first, q.peek());
    assertThrows(
        OutboundQueueFullException.class,
        () -> q.put(packet(4, DeliveryOptions.replaceable("position:1", Duration.ZERO))));
    assertSame(first, q.take());
    first = packet(3, DeliveryOptions.replaceable("position:1", Duration.ZERO));
    first.setMarkedAsLast(true);
    q.put(first);
    assertThrows(
        OutboundQueueFullException.class,
        () -> q.put(packet(4, DeliveryOptions.replaceable("position:1", Duration.ZERO))));
  }

  @Test
  void expiryCannotRemoveClaimedOrFragmentedPackets() {
    var q = queue(2);
    Packet claimed = spy(packet(2, DeliveryOptions.bestEffort(Duration.ZERO)));
    q.put(claimed);
    q.peek();
    doReturn(1L).when(claimed).getDeliveryDeadlineNanos();
    assertSame(claimed, q.peek());
    assertSame(claimed, q.take());
    var fragmented = spy(packet(2, DeliveryOptions.bestEffort(Duration.ZERO)));
    fragmented.setFragmentBuffer(new byte[] {1});
    q.put(fragmented);
    doReturn(1L).when(fragmented).getDeliveryDeadlineNanos();
    assertSame(fragmented, q.peek());
  }

  @Test
  void removesExpiredUnclaimedPacketsAndCountsTheReason() {
    var q = queue(2);
    Packet expiring = spy(packet(2, DeliveryOptions.bestEffort(Duration.ZERO)));
    q.put(expiring);
    doReturn(1L).when(expiring).getDeliveryDeadlineNanos();
    assertNull(q.peek());
    assertEquals(0, q.getSize());
    assertEquals(1, q.deliveryStatistics().expired());
  }

  @Test
  void byteBoundsAndBestEffortAdmissionAreIndependentOfPacketCapacity() {
    var q = queue(10);
    q.configureDeliveryByteLimit(5);
    q.put(packet(4, null));
    q.put(packet(2, DeliveryOptions.bestEffort(Duration.ZERO)));
    assertEquals(1, q.getSize());
    assertEquals(1, q.deliveryStatistics().bestEffortDrops());
    assertThrows(
        OutboundQueueFullException.class, () -> q.put(packet(2, DeliveryOptions.important())));
    q.take();
    assertEquals(0, q.deliveryStatistics().queuedBytes());
  }

  @Test
  void failedReplacementLeavesOriginalPacketInItsOriginalPosition() {
    var q = queue(3);
    var first = packet(2, null);
    var old = packet(2, DeliveryOptions.replaceable("state", Duration.ZERO));
    q.put(first);
    q.put(old);
    q.configureOutboundQueuePolicy(
        (queue, packet) -> {
          throw new IllegalArgumentException("rejected");
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> q.put(packet(3, DeliveryOptions.replaceable("state", Duration.ZERO))));
    assertEquals(4, q.deliveryStatistics().queuedBytes());
    assertSame(first, q.take());
    assertSame(old, q.take());
  }

  @Test
  void copiesPreservePolicyAndDeadlineWithoutExtendingLifetime() {
    var original = packet(4, DeliveryOptions.bestEffort(Duration.ofSeconds(1)));
    var copy = original.deepCopy();
    assertEquals(original.getDeliveryOptions(), copy.getDeliveryOptions());
    assertEquals(original.getDeliveryDeadlineNanos(), copy.getDeliveryDeadlineNanos());
  }

  @Test
  void legacyAndDisabledDiagnosticsRemainUnallocatedOrUnavailable() {
    var q = queue(3);
    q.enableMonitoring(false, null, 1, "TCP");
    q.put(packet(2, null));
    assertEquals(-1, q.deliveryStatistics().queuedBytes());
    q.put(packet(2, DeliveryOptions.bestEffort(Duration.ZERO)));
    assertEquals(-1, q.deliveryStatistics().expired());
    assertEquals(-1, q.statistics().fullDrops());
  }

  @Test
  void repeatedPacketReferencesKeepTheByteAccountAccurate() {
    var q = queue(5);
    q.configureDeliveryByteLimit(100);
    var p = packet(4, null);
    q.put(p);
    q.put(p);
    assertEquals(8, q.deliveryStatistics().queuedBytes());
    q.take();
    assertEquals(4, q.deliveryStatistics().queuedBytes());
    q.take();
    assertEquals(0, q.deliveryStatistics().queuedBytes());
  }

  @Test
  void importantTcpRetainsFifoAndHardLimitsAtNinetyPercent() {
    var q = queue(10);
    var original = new java.util.ArrayList<Packet>();
    for (int i = 0; i < 9; i++) {
      var entry = packet(1, null);
      original.add(entry);
      q.put(entry);
    }
    var important = packet(1, DeliveryOptions.important());
    assertEquals(com.tenio.core.network.entity.outbound.packet.OutboundQueue.Admission.ENQUEUED,
        q.putWithDelivery(important));
    assertThrows(OutboundQueueFullException.class, () -> q.put(packet(1, DeliveryOptions.important())));
    for (var entry : original) {
      assertSame(entry, q.take());
    }
    assertSame(important, q.take());
  }

  @Test
  void customPolicyRejectionRestoresTheOriginalOrderAndByteAccounting() {
    var q = queue(10);
    var original = new java.util.ArrayList<Packet>();
    for (int i = 0; i < 9; i++) {
      var entry = packet(1, i == 1 ? DeliveryOptions.replaceable("state", Duration.ZERO) : null);
      original.add(entry);
      q.put(entry);
    }
    q.configureOutboundQueuePolicy((queue, packet) -> {
      throw new com.tenio.core.exception.OutboundQueuePolicyViolationException(
          packet, queue.getPercentageUsed());
    });
    var replacement = packet(2, DeliveryOptions.replaceable("state", Duration.ZERO));
    assertThrows(com.tenio.core.exception.OutboundQueuePolicyViolationException.class,
        () -> q.putWithDelivery(replacement));
    assertEquals(9, q.deliveryStatistics().queuedBytes());
    assertEquals(0, q.deliveryStatistics().replaced());
    assertEquals(1, q.statistics().policyDrops());
    for (var entry : original) {
      assertSame(entry, q.take());
    }
  }

  @Test
  void noDeliveryModeBypassesTheUdpCongestionCutoff() {
    var q = queue(100);
    for (int i = 0; i < 75; i++) {
      var tcp = packet(1, null);
      tcp.setTransportType(com.tenio.core.network.define.TransportType.TCP);
      q.put(tcp);
    }
    for (var options : new DeliveryOptions[] {null, DeliveryOptions.important(),
        DeliveryOptions.replaceable("udp-state", Duration.ZERO),
        DeliveryOptions.bestEffort(Duration.ZERO)}) {
      var udp = packet(1, options);
      udp.setTransportType(com.tenio.core.network.define.TransportType.UDP);
      assertThrows(com.tenio.core.exception.OutboundQueuePolicyViolationException.class,
          () -> q.putWithDelivery(udp));
    }
    assertEquals(75, q.getSize());
    assertEquals(4, q.statistics().policyDrops());
  }

  @Test
  void udpLeavesReservedHeadroomForTcpWithoutEvictingOrReorderingQueuedPackets() {
    var q = queue(100);
    var original = new java.util.ArrayList<Packet>();
    for (int i = 0; i < 75; i++) {
      var entry = packet(1, null);
      entry.setTransportType(com.tenio.core.network.define.TransportType.UDP);
      original.add(entry);
      q.put(entry);
    }
    var udp = packet(1, null);
    udp.setTransportType(com.tenio.core.network.define.TransportType.UDP);
    assertThrows(com.tenio.core.exception.OutboundQueuePolicyViolationException.class, () -> q.put(udp));
    assertEquals(75, q.getSize());
    assertEquals(1, q.statistics().policyDrops());
    for (int i = 75; i < 100; i++) {
      var entry = packet(1, null);
      entry.setTransportType(com.tenio.core.network.define.TransportType.TCP);
      original.add(entry);
      q.put(entry);
    }
    assertEquals(100, q.getSize());
    assertThrows(OutboundQueueFullException.class, () -> q.put(udp));
    assertEquals(1, q.statistics().policyDrops());
    assertEquals(1, q.statistics().fullDrops());
    for (var entry : original) {
      assertSame(entry, q.take());
    }
    assertEquals(0, q.getSize());
  }

  @Test
  void rejectedUdpReplacementRestoresItsOriginalPositionAndByteAccounting() {
    var q = queue(100);
    var original = new java.util.ArrayList<Packet>();
    for (int i = 0; i < 76; i++) {
      var entry = packet(1, i == 1 ? DeliveryOptions.replaceable("position", Duration.ZERO) : null);
      entry.setTransportType(i == 1 ? com.tenio.core.network.define.TransportType.UDP
          : com.tenio.core.network.define.TransportType.TCP);
      original.add(entry);
      q.put(entry);
    }
    var replacement = packet(2, DeliveryOptions.replaceable("position", Duration.ZERO));
    replacement.setTransportType(com.tenio.core.network.define.TransportType.UDP);
    assertThrows(com.tenio.core.exception.OutboundQueuePolicyViolationException.class,
        () -> q.putWithDelivery(replacement));
    assertEquals(76, q.getSize());
    assertEquals(76, q.deliveryStatistics().queuedBytes());
    assertEquals(0, q.deliveryStatistics().replaced());
    assertEquals(1, q.statistics().policyDrops());
    for (var entry : original) {
      assertSame(entry, q.take());
    }
  }

  @Test
  void nonGrowingUdpReplacementAtTheCutoffDoesNotConsumeReservedSlots() {
    var q = queue(100);
    for (int i = 0; i < 75; i++) {
      var entry = packet(1, i == 0 ? DeliveryOptions.replaceable("position", Duration.ZERO) : null);
      entry.setTransportType(i == 0 ? com.tenio.core.network.define.TransportType.UDP
          : com.tenio.core.network.define.TransportType.TCP);
      q.put(entry);
    }
    var replacement = packet(2, DeliveryOptions.replaceable("position", Duration.ZERO));
    replacement.setTransportType(com.tenio.core.network.define.TransportType.UDP);
    q.put(replacement);
    assertEquals(75, q.getSize());
    assertEquals(1, q.deliveryStatistics().replaced());
    for (int i = 1; i < 75; i++) {
      assertTrue(q.take().isTcp());
    }
    assertSame(replacement, q.take());
  }

  @Test
  void aCustomPolicyCanChooseDifferentUdpAdmissionButCannotExceedQueueCapacity() {
    var q = queue(4);
    q.configureOutboundQueuePolicy((queue, packet) -> {});
    for (int i = 0; i < 4; i++) {
      var udp = packet(1, null);
      udp.setTransportType(com.tenio.core.network.define.TransportType.UDP);
      q.put(udp);
    }
    var extra = packet(1, null);
    extra.setTransportType(com.tenio.core.network.define.TransportType.UDP);
    assertThrows(OutboundQueueFullException.class, () -> q.put(extra));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(DeliveryOptions.Mode.class)
  void tcpWithEveryModeRemainsEligibleAtNinetyPercent(DeliveryOptions.Mode mode) {
    var q = queue(20);
    for (int i = 0; i < 18; i++) {
      var tcp = packet(1, null);
      tcp.setTransportType(com.tenio.core.network.define.TransportType.TCP);
      q.put(tcp);
    }
    var options = switch (mode) {
      case IMPORTANT -> DeliveryOptions.important();
      case REPLACEABLE -> DeliveryOptions.replaceable("tcp-state", Duration.ZERO);
      case BEST_EFFORT -> DeliveryOptions.bestEffort(Duration.ZERO);
    };
    var tcp = packet(1, options);
    tcp.setTransportType(com.tenio.core.network.define.TransportType.TCP);
    q.put(tcp);
    assertEquals(19, q.getSize());
    var finalTcp = packet(1, null);
    finalTcp.setTransportType(com.tenio.core.network.define.TransportType.TCP);
    q.put(finalTcp);
    assertEquals(20, q.getSize());
    assertThrows(OutboundQueueFullException.class, () -> q.put(packet(1, null)));
  }
}
