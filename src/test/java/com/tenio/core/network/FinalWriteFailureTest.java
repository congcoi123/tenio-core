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

package com.tenio.core.network;

import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.codec.encoder.BinaryPacketEncoder;
import com.tenio.core.network.entity.outbound.implement.ResponseImpl;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.implement.OutboundQueueImpl;
import com.tenio.core.network.entity.outbound.packet.implement.PacketImpl;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.netty.NettyWebSocketImpl;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.engine.implement.ZeroWriterImpl;
import com.tenio.core.network.zero.engine.writer.WriterHandler;
import com.tenio.core.network.zero.engine.writer.implement.SocketWriterHandler;
import com.tenio.core.server.Server;
import com.tenio.core.server.ServerImpl;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinalWriteFailureTest {

    private Session session() {
        var session = mock(Session.class);
        when(session.isActivated()).thenReturn(true);
        return session;
    }

    private Packet packet(Session session, boolean finalPacket) {
        var packet = PacketImpl.newInstance();
        packet.setRecipients(List.of(session));
        packet.setMarkedAsLast(finalPacket);
        packet.setData(new byte[]{1, 2});
        return packet;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectedFinalPacketsCloseAtPacketAndByteBounds(boolean byteBound) throws Exception {
        var session = session();
        var queue = OutboundQueueImpl.newInstance();
        queue.configureMaxSize(1);
        queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
        if (byteBound) {
            queue.configureDeliveryByteLimit(1);
        } else {
            queue.put(PacketImpl.newInstance());
        }
        when(session.fetchOutboundQueue()).thenReturn(queue);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        var statistics = mock(NetworkWriterStatistic.class);
        writer.setNetworkWriterStatistic(statistics);
        writer.enqueuePacket(packet(session, true));
        verify(session).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
        verify(session).addDroppedPackets(1);
        verify(statistics).updateWrittenDroppedPacketsByFull(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unexpectedAdmissionFailureClosesOnlyFinalWritesAndStillPropagates(boolean finalPacket) throws Exception {
        var session = session();
        var queue = mock(OutboundQueue.class);
        var failure = new IllegalStateException("queue failure");
        when(queue.put(any())).thenThrow(failure);
        when(session.fetchOutboundQueue()).thenReturn(queue);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> writer.enqueuePacket(packet(session, finalPacket))));
        verify(session, finalPacket ? org.mockito.Mockito.times(1) : never())
                .close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
    }

    @Test
    void admittedFinalPacketWaitsForTheWriter() throws Exception {
        var session = session();
        var queue = mock(OutboundQueue.class);
        when(queue.put(any())).thenReturn(OutboundQueue.Admission.ENQUEUED);
        when(session.fetchOutboundQueue()).thenReturn(queue);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.setThreadPoolSize(1);
        writer.onInitialized();
        writer.enqueuePacket(packet(session, true));
        verify(session, never()).close(any(), any());
        writer.onShutdown();
    }

    @Test
    void tcpEncodingFailureClosesTheFinalSessionAndRemainsVisibleToTheWriter() throws Exception {
        var session = session();
        var queue = mock(OutboundQueue.class);
        var packet = packet(session, true);
        packet.setTransportType(com.tenio.core.network.define.TransportType.TCP);
        when(session.fetchOutboundQueue()).thenReturn(queue);
        when(queue.peek()).thenReturn(packet);
        var socketWriter = mock(WriterHandler.class);
        var failure = new IllegalArgumentException("encoding failure");
        doThrow(failure).when(socketWriter).send(queue, session, packet);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        var method = ZeroWriterImpl.class.getDeclaredMethod("processSessionQueue", Session.class, WriterHandler.class, WriterHandler.class);
        method.setAccessible(true);
        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> method.invoke(writer, session, socketWriter, mock(WriterHandler.class)));
        assertSame(failure, thrown.getCause());
        verify(session).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
    }

    @Test
    void emptyEncodedFinalPacketStillClosesTheTcpSession() throws Exception {
        var session = session();
        var channel = mock(SocketChannel.class);
        when(channel.isOpen()).thenReturn(true);
        when(channel.isConnected()).thenReturn(true);
        when(session.fetchSocketChannel()).thenReturn(channel);
        var packet = packet(session, true);
        packet.setData(new byte[0]);
        var encoder = mock(BinaryPacketEncoder.class);
        when(encoder.encode(packet)).thenReturn(packet);
        var handler = SocketWriterHandler.newInstance();
        handler.setPacketEncoder(encoder);
        var queue = mock(OutboundQueue.class);
        handler.send(queue, session, packet);
        verify(queue).take();
        verify(queue).clear();
        verify(session).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
    }

    @Test
    void responseFailureClosesAllMarkedRecipientsAndPreservesTheOriginalFailure() throws Exception {
        var tcp = session();
        var ws = session();
        when(tcp.isTcp()).thenReturn(true);
        when(ws.isWebSocket()).thenReturn(true);
        var response = ResponseImpl.newInstance().setRecipientSessions(List.of(tcp, ws));
        var server = mock(Server.class);
        var failure = new IllegalArgumentException("serialization failure");
        var closeFailure = new IOException("close failure");
        doThrow(failure).when(server).write(response, true);
        doThrow(closeFailure).when(tcp).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
        try (var access = mockStatic(ServerImpl.class)) {
            access.when(ServerImpl::getInstance).thenReturn(server);
            assertSame(failure, assertThrows(IllegalArgumentException.class, response::writeThenClose));
        }
        verify(tcp).markCloseAfterPendingWrite();
        verify(ws).markCloseAfterPendingWrite();
        verify(tcp).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
        verify(ws).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
        assertEquals(List.of(closeFailure), List.of(failure.getSuppressed()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void webSocketBackpressureClosesOnlyFinalWrites(boolean finalPacket) throws Exception {
        var session = session();
        when(session.fetchWebSocketChannel()).thenReturn(mock(Channel.class));
        var webSocket = NettyWebSocketImpl.newInstance(mock(EventManager.class));
        var statistics = mock(NetworkWriterStatistic.class);
        webSocket.setNetworkWriterStatistic(statistics);
        webSocket.write(packet(session, finalPacket));
        verify(statistics).updateWrittenDroppedPacketsByFull(1);
        verify(session, finalPacket ? org.mockito.Mockito.times(1) : never())
                .close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void synchronousWebSocketFailuresCloseOnlyFinalWritesAndStillPropagate(boolean finalPacket) throws Exception {
        for (boolean encodingFailure : new boolean[]{false, true}) {
            var session = session();
            var channel = mock(Channel.class);
            when(channel.isWritable()).thenReturn(true);
            when(session.fetchWebSocketChannel()).thenReturn(channel);
            var encoder = mock(BinaryPacketEncoder.class);
            var failure = new IllegalArgumentException("WebSocket write failure");
            if (encodingFailure) {
                when(encoder.encode(any())).thenThrow(failure);
            } else {
                when(encoder.encode(any())).thenReturn(packet(session, finalPacket));
                doAnswer(call -> {
                    ReferenceCountUtil.release(call.getArgument(0));
                    throw failure;
                }).when(channel).writeAndFlush(any());
            }
            var webSocket = NettyWebSocketImpl.newInstance(mock(EventManager.class));
            webSocket.setNetworkWriterStatistic(mock(NetworkWriterStatistic.class));
            webSocket.setPacketEncoder(encoder);
            assertSame(failure, assertThrows(IllegalArgumentException.class,
                    () -> webSocket.write(packet(session, finalPacket))));
            verify(session, finalPacket ? org.mockito.Mockito.times(1) : never())
                    .close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void finalWebSocketWriteClosesAfterEitherFutureOutcome(boolean success) throws Exception {
        var session = session();
        var channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(true);
        when(session.fetchWebSocketChannel()).thenReturn(channel);
        var promise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        doAnswer(call -> {
            ReferenceCountUtil.release(call.getArgument(0));
            return promise;
        }).when(channel).writeAndFlush(any());
        var encoder = mock(BinaryPacketEncoder.class);
        when(encoder.encode(any())).thenReturn(packet(session, true));
        var webSocket = NettyWebSocketImpl.newInstance(mock(EventManager.class));
        webSocket.setPacketEncoder(encoder);
        webSocket.setNetworkWriterStatistic(mock(NetworkWriterStatistic.class));
        webSocket.write(packet(session, true));
        verify(session, never()).close(any(), any());
        if (success) {
            promise.setSuccess();
        } else {
            promise.setFailure(new IOException("asynchronous failure"));
        }
        verify(session).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
    }
}
