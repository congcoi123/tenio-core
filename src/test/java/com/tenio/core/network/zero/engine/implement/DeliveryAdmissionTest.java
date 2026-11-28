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

package com.tenio.core.network.zero.engine.implement;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.implement.OutboundQueueImpl;
import com.tenio.core.network.entity.outbound.packet.implement.PacketImpl;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryAdmissionTest {

    @Test
    void bestEffortCapacityDropsReachSessionAndNetworkTotals() throws Exception {
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        var stats = mock(NetworkWriterStatistic.class);
        writer.setNetworkWriterStatistic(stats);
        var queue = OutboundQueueImpl.newInstance();
        queue.configureMaxSize(1);
        queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
        var first = PacketImpl.newInstance();
        first.setData(new byte[]{1});
        queue.put(first);
        var session = mock(Session.class);
        when(session.isActivated()).thenReturn(true);
        when(session.fetchOutboundQueue()).thenReturn(queue);
        var dropped = PacketImpl.newInstance();
        dropped.setData(new byte[]{2});
        dropped.setDeliveryOptions(DeliveryOptions.bestEffort(Duration.ZERO));
        var method = ZeroWriterImpl.class.getDeclaredMethod("enqueuePacket", Session.class, Packet.class);
        method.setAccessible(true);
        method.invoke(writer, session, dropped);
        assertEquals(1, queue.getSize());
        verify(session).addDroppedPackets(1);
        verify(stats).updateWrittenDroppedPacketsByFull(1);
    }

    @Test
    void udpCongestionRejectionsRemainPolicyDrops() throws Exception {
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        var stats = mock(NetworkWriterStatistic.class);
        writer.setNetworkWriterStatistic(stats);
        var queue = OutboundQueueImpl.newInstance();
        queue.configureMaxSize(10);
        queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
        for (int i = 0; i < 9; i++) {
            queue.put(PacketImpl.newInstance());
        }
        var session = mock(Session.class);
        when(session.isActivated()).thenReturn(true);
        when(session.fetchOutboundQueue()).thenReturn(queue);
        var dropped = PacketImpl.newInstance();
        dropped.setTransportType(TransportType.UDP);
        dropped.setDeliveryOptions(DeliveryOptions.bestEffort(Duration.ZERO));
        var method = ZeroWriterImpl.class.getDeclaredMethod("enqueuePacket", Session.class, Packet.class);
        method.setAccessible(true);
        method.invoke(writer, session, dropped);
        assertEquals(9, queue.getSize());
        verify(session).addDroppedPackets(1);
        verify(stats).updateWrittenDroppedPacketsByPolicy(1);
        verify(stats, never()).updateWrittenDroppedPacketsByFull(anyInt());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void udpWithOrWithoutImportantModeRemainsASilentPolicyDrop(boolean explicitOptions) throws Exception {
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        var stats = mock(NetworkWriterStatistic.class);
        writer.setNetworkWriterStatistic(stats);
        var queue = OutboundQueueImpl.newInstance();
        queue.configureMaxSize(10);
        queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
        queue.enableMonitoring(null, 1, "TCP");
        for (int i = 0; i < 9; i++) {
            queue.put(PacketImpl.newInstance());
        }
        var session = mock(Session.class);
        when(session.isActivated()).thenReturn(true);
        when(session.fetchOutboundQueue()).thenReturn(queue);
        var udp = PacketImpl.newInstance();
        udp.setTransportType(TransportType.UDP);
        if (explicitOptions) {
            udp.setDeliveryOptions(DeliveryOptions.important());
        }
        var method = ZeroWriterImpl.class.getDeclaredMethod("enqueuePacket", Session.class, Packet.class);
        method.setAccessible(true);
        method.invoke(writer, session, udp);
        assertEquals(9, queue.getSize());
        assertEquals(1, queue.statistics().policyDrops());
        assertEquals(0, queue.statistics().fullDrops());
        verify(session).addDroppedPackets(1);
        verify(stats).updateWrittenDroppedPacketsByPolicy(1);
        verify(stats, never()).updateWrittenDroppedPacketsByFull(anyInt());
    }
}
