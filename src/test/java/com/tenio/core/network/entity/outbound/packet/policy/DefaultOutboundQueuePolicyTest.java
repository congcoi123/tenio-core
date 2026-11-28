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

import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.implement.PacketImpl;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy.Decision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultOutboundQueuePolicyTest {

    private final DefaultOutboundQueuePolicy policy = new DefaultOutboundQueuePolicy();

    @ParameterizedTest
    @CsvSource({
            "UDP,0,false", "UDP,74.9,false", "UDP,75,false", "UDP,89.9,false", "UDP,90,true",
            "UDP,99.9,true", "TCP,0,false", "TCP,74.9,false", "TCP,75,false", "TCP,89.9,false",
            "TCP,90,false", "TCP,99.9,false", "WEB_SOCKET,0,false", "WEB_SOCKET,74.9,false",
            "WEB_SOCKET,75,false", "WEB_SOCKET,89.9,false", "WEB_SOCKET,90,false", "WEB_SOCKET,99.9,false"
    })
    void transportDeterminesAdmissionForEveryMode(TransportType transport, float occupancy,
                                                  boolean rejected) {
        var queue = mock(OutboundQueue.class);
        when(queue.getPercentageUsed()).thenReturn(occupancy);
        for (var options : new DeliveryOptions[]{null, DeliveryOptions.important(),
                DeliveryOptions.replaceable("position", Duration.ZERO),
                DeliveryOptions.bestEffort(Duration.ZERO)}) {
            var packet = PacketImpl.newInstance();
            packet.setTransportType(transport);
            packet.setDeliveryOptions(options);
            assertEquals(rejected ? Decision.REJECT_SILENTLY : Decision.ACCEPT, policy.applyPolicy(queue, packet, false));
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransportType.class, names = {"TCP", "UDP", "WEB_SOCKET"})
    void hardCapacityRejectionsAreSilentOnlyForUdp(TransportType transport) {
        var queue = mock(OutboundQueue.class);
        for (var options : new DeliveryOptions[]{null, DeliveryOptions.important(),
                DeliveryOptions.replaceable("position", Duration.ZERO),
                DeliveryOptions.bestEffort(Duration.ZERO)}) {
            var packet = PacketImpl.newInstance();
            packet.setTransportType(transport);
            packet.setDeliveryOptions(options);
            assertEquals(
                    transport == TransportType.UDP ? Decision.REJECT_SILENTLY : Decision.ACCEPT,
                    policy.applyPolicy(queue, packet, true)
            );
        }
        verifyNoInteractions(queue);
    }

    @Test
    void uncongestedQueuesDoNotInspectPacketMetadata() {
        var queue = mock(OutboundQueue.class);
        var packet = mock(Packet.class);
        when(queue.getPercentageUsed()).thenReturn(50.0f);
        assertEquals(Decision.ACCEPT, policy.applyPolicy(queue, packet, false));
        verifyNoInteractions(packet);
    }

    @Test
    void congestedQueuesDoNotInspectDeliveryOptions() {
        var queue = mock(OutboundQueue.class);
        var packet = mock(Packet.class);
        when(queue.getPercentageUsed()).thenReturn(90.0f);
        when(packet.isUdp()).thenReturn(true);
        assertEquals(Decision.REJECT_SILENTLY, policy.applyPolicy(queue, packet, false));
        verify(packet, never()).getDeliveryOptions();
    }
}
