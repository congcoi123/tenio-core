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

import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue.Admission;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy.Decision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundQueueAdmissionTest {

    @ParameterizedTest
    @EnumSource(value = TransportType.class, names = {"TCP", "UDP", "WEB_SOCKET"})
    void aCustomPolicyCanUseEverySlotForAnyTransport(TransportType transport) {
        var queue = DeliveryPolicyTest.queue(10);
        var capacityChecks = new AtomicInteger();
        queue.configureOutboundQueuePolicy((outbound, packet, capacityExceeded) -> {
            if (capacityExceeded) {
                capacityChecks.incrementAndGet();
            }
            return Decision.ACCEPT;
        });
        for (int i = 0; i < 10; i++) {
            var packet = DeliveryPolicyTest.packet(1, null);
            packet.setTransportType(transport);
            assertEquals(Admission.ENQUEUED, queue.putWithDelivery(packet));
        }
        var rejected = DeliveryPolicyTest.packet(1, null);
        rejected.setTransportType(transport);
        assertEquals(Admission.DROPPED, queue.putWithDelivery(rejected));
        assertEquals(1, capacityChecks.get());
        assertEquals(10, queue.getSize());
        assertEquals(1, queue.statistics().fullDrops());
        assertEquals(0, queue.statistics().policyDrops());
    }

    @ParameterizedTest
    @EnumSource(Decision.class)
    void noPolicyDecisionCanBypassPacketOrByteBounds(Decision decision) {
        for (var transport : new TransportType[]{TransportType.TCP, TransportType.UDP, TransportType.WEB_SOCKET}) {
            for (boolean byteBound : new boolean[]{false, true}) {
                for (var options : new DeliveryOptions[]{null, DeliveryOptions.important(),
                        DeliveryOptions.replaceable("state", Duration.ZERO),
                        DeliveryOptions.bestEffort(Duration.ZERO)}) {
                    var queue = DeliveryPolicyTest.queue(1);
                    queue.configureOutboundQueuePolicy((outbound, packet, capacityExceeded) -> Decision.ACCEPT);
                    if (byteBound) {
                        queue.configureDeliveryByteLimit(1);
                    } else {
                        queue.put(DeliveryPolicyTest.packet(1, null));
                    }
                    var rejected = DeliveryPolicyTest.packet(2, options);
                    rejected.setTransportType(transport);
                    queue.configureOutboundQueuePolicy((outbound, packet, capacityExceeded) -> {
                        assertTrue(capacityExceeded);
                        return decision;
                    });
                    assertEquals(
                            decision == Decision.REJECT_SILENTLY ? Admission.SILENTLY_DROPPED : Admission.DROPPED,
                            queue.putWithDelivery(rejected)
                    );
                    assertEquals(byteBound ? 0 : 1, queue.getSize());
                    assertEquals(1, queue.statistics().fullDrops());
                    assertEquals(0, queue.statistics().policyDrops());
                    if (options != null) {
                        assertEquals(
                                options.mode() == DeliveryOptions.Mode.BEST_EFFORT ? 1 : 0,
                                queue.deliveryStatistics().bestEffortDrops()
                        );
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Decision.class)
    void capacityRejectionRestoresReplacementOrderAndBytes(Decision decision) {
        var queue = DeliveryPolicyTest.queue(3);
        queue.configureDeliveryByteLimit(4);
        var first = DeliveryPolicyTest.packet(2, null);
        var original = DeliveryPolicyTest.packet(2, DeliveryOptions.replaceable("state", Duration.ZERO));
        queue.put(first);
        queue.put(original);
        queue.configureOutboundQueuePolicy((outbound, packet, capacityExceeded) -> {
            assertTrue(capacityExceeded);
            assertEquals(1, outbound.getSnapshotSize());
            return decision;
        });
        var replacement = DeliveryPolicyTest.packet(3, DeliveryOptions.replaceable("state", Duration.ZERO));
        assertEquals(
                decision == Decision.REJECT_SILENTLY ? Admission.SILENTLY_DROPPED : Admission.DROPPED,
                queue.putWithDelivery(replacement)
        );
        assertEquals(2, queue.getSize());
        assertEquals(4, queue.deliveryStatistics().queuedBytes());
        assertEquals(0, queue.deliveryStatistics().replaced());
        assertEquals(1, queue.statistics().fullDrops());
        assertEquals(0, queue.statistics().policyDrops());
        assertSame(first, queue.take());
        assertSame(original, queue.take());
    }

    @Test
    void aMissingPolicyDecisionIsAnUnexpectedFailureAndPreservesTheQueuedUpdate() {
        var queue = DeliveryPolicyTest.queue(2);
        var original = DeliveryPolicyTest.packet(2, DeliveryOptions.replaceable("state", Duration.ZERO));
        queue.put(original);
        queue.configureOutboundQueuePolicy((outbound, packet, capacityExceeded) -> null);
        assertThrows(NullPointerException.class, () -> queue.putWithDelivery(DeliveryPolicyTest.packet(3, DeliveryOptions.replaceable("state", Duration.ZERO))));
        assertEquals(2, queue.deliveryStatistics().queuedBytes());
        assertEquals(0, queue.statistics().fullDrops());
        assertEquals(0, queue.statistics().policyDrops());
        assertSame(original, queue.take());
    }
}
