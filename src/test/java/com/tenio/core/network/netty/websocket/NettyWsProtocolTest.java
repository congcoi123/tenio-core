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

package com.tenio.core.network.netty.websocket;

import com.tenio.common.data.DataCollection;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NettyWsProtocolTest {

    private final SessionManager sessions = mock(SessionManager.class);
    private final BinaryPacketDecoder decoder = mock(BinaryPacketDecoder.class);

    private NettyWsHandler handler() {
        return NettyWsHandler.newInstance(mock(EventManager.class), sessions,
                mock(ConnectionFilter.class), decoder, mock(NetworkReaderStatistic.class));
    }

    private EmbeddedChannel handshakeChannel() {
        var channel = new EmbeddedChannel(new HttpServerCodec(),
                new HttpObjectAggregator(NettyWsInitializer.MAX_HTTP_CONTENT_LENGTH),
                new WebSocketFrameAggregator(NettyWsInitializer.MAX_MESSAGE_LENGTH),
                NettyWsHandShake.newInstance(mock(EventManager.class), sessions,
                        mock(ConnectionFilter.class), decoder, mock(NetworkReaderStatistic.class)));
        channel.pipeline().addFirst("handshake-timeout", new ReadTimeoutHandler(10));
        return channel;
    }

    private FullHttpRequest upgrade() {
        var request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/websocket");
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.CONNECTION, "keep-alive, Upgrade");
        request.headers().set(HttpHeaderNames.UPGRADE, "websocket");
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "13");
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_KEY, "dGhlIHNhbXBsZSBub25jZQ==");
        return request;
    }

    private String response(EmbeddedChannel channel) {
        ByteBuf bytes = channel.readOutbound();
        assertNotNull(bytes);
        try {
            return bytes.toString(StandardCharsets.US_ASCII);
        } finally {
            bytes.release();
        }
    }

    @Test
    void testUpgradeReleasesRequestAndAcceptsAnyPath() {
        var channel = handshakeChannel();
        var request = upgrade();
        try {
            channel.writeInbound(request);
            assertTrue(response(channel).startsWith("HTTP/1.1 101"));
            assertEquals(0, request.refCnt());
            assertTrue(channel.isActive());
            assertNotNull(channel.pipeline().get(NettyWsHandler.class));
            assertNull(channel.pipeline().get("handshake-timeout"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testHandshakeReadTimeoutClosesIdleConnection() {
        var channel = handshakeChannel();
        try {
            channel.advanceTimeBy(11, TimeUnit.SECONDS);
            channel.runScheduledPendingTasks();
            assertFalse(channel.isActive());
            verifyNoInteractions(sessions, decoder);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testMissingKeyIsRejectedAndReleased() {
        var channel = handshakeChannel();
        var request = upgrade();
        request.headers().remove(HttpHeaderNames.SEC_WEBSOCKET_KEY);
        try {
            channel.writeInbound(request);
            assertTrue(response(channel).startsWith("HTTP/1.1 400"));
            assertFalse(channel.isActive());
            assertEquals(0, request.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testUnsupportedVersionClosesWithoutInstallingApplicationHandler() {
        var channel = handshakeChannel();
        var request = upgrade();
        request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_VERSION, "99");
        try {
            channel.writeInbound(request);
            assertTrue(response(channel).startsWith("HTTP/1.1 426"));
            assertFalse(channel.isActive());
            assertNull(channel.pipeline().get(NettyWsHandler.class));
            assertEquals(0, request.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testPingReturnsSamePayloadWithoutCreatingSession() {
        var channel = new EmbeddedChannel(handler());
        var ping = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{1, 2, 3}));
        try {
            channel.writeInbound(ping);
            PongWebSocketFrame pong = channel.readOutbound();
            assertNotNull(pong);
            assertEquals(3, pong.content().readableBytes());
            assertEquals(1, pong.content().readByte());
            assertEquals(2, pong.content().readByte());
            assertEquals(3, pong.content().readByte());
            pong.release();
            assertEquals(0, ping.refCnt());
            verifyNoInteractions(sessions, decoder);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testCloseEchoesStatusAndReleasesFrame() {
        var channel = new EmbeddedChannel(handler());
        var close = new CloseWebSocketFrame(1000, "finished");
        try {
            channel.writeInbound(close);
            CloseWebSocketFrame reply = channel.readOutbound();
            assertEquals(1000, reply.statusCode());
            assertEquals("finished", reply.reasonText());
            reply.release();
            assertEquals(0, close.refCnt());
            assertFalse(channel.isActive());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testCloseRecordsClientRequestInsteadOfConnectionLoss() throws Exception {
        var session = mock(Session.class);
        when(sessions.getSessionByWebSocket(any())).thenReturn(session);
        when(session.isActivated()).thenReturn(true, false);
        var channel = new EmbeddedChannel(handler());
        try {
            channel.writeInbound(new CloseWebSocketFrame(1000, "finished"));
            CloseWebSocketFrame reply = channel.readOutbound();
            reply.release();
            verify(session).close(ConnectionDisconnectMode.CLIENT_REQUEST, PlayerDisconnectMode.CLIENT_REQUEST);
            verify(session, never()).close(ConnectionDisconnectMode.LOST_IN_READ, PlayerDisconnectMode.CONNECTION_LOST);
            assertFalse(channel.isActive());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testTextIsRejectedWithUnsupportedDataStatus() {
        var channel = new EmbeddedChannel(handler());
        var text = new TextWebSocketFrame("not a TenIO binary packet");
        try {
            channel.writeInbound(text);
            CloseWebSocketFrame reply = channel.readOutbound();
            assertEquals(1003, reply.statusCode());
            reply.release();
            assertEquals(0, text.refCnt());
            assertFalse(channel.isActive());
            verifyNoInteractions(decoder);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testEmptyPacketIsRejectedBeforeCreatingSession() {
        var channel = new EmbeddedChannel(handler());
        try {
            channel.writeInbound(new BinaryWebSocketFrame());
            CloseWebSocketFrame reply = channel.readOutbound();
            assertEquals(1002, reply.statusCode());
            reply.release();
            assertFalse(channel.isActive());
            verifyNoInteractions(decoder);
            verify(sessions, never()).createWebSocketSession(any());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testFragmentsAreDecodedOnceWithInterleavedPing() throws Exception {
        var session = mock(Session.class);
        var message = mock(DataCollection.class);
        when(sessions.getSessionByWebSocket(any())).thenReturn(session);
        when(session.isActivated()).thenReturn(true);
        when(session.isAssociatedToPlayer(Session.AssociatedState.DONE)).thenReturn(true);
        when(decoder.decode(any(byte[].class))).thenReturn(message);
        var channel = new EmbeddedChannel(new WebSocketFrameAggregator(8), handler());
        var first = new BinaryWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{1, 2}));
        var last = new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(new byte[]{3, 4}));
        try {
            channel.writeInbound(first);
            verifyNoInteractions(decoder);
            channel.writeInbound(new PingWebSocketFrame());
            PongWebSocketFrame pong = channel.readOutbound();
            pong.release();
            channel.writeInbound(last);
            verify(decoder).decode(new byte[]{1, 2, 3, 4});
            verify(session).enqueueInbound(message);
            assertEquals(0, first.refCnt());
            assertEquals(0, last.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testOversizedFragmentedMessageNeverReachesDecoder() {
        var channel = new EmbeddedChannel(new WebSocketFrameAggregator(3), handler());
        var first = new BinaryWebSocketFrame(false, 0, Unpooled.wrappedBuffer(new byte[]{1, 2}));
        var last = new ContinuationWebSocketFrame(true, 0, Unpooled.wrappedBuffer(new byte[]{3, 4}));
        try {
            channel.writeInbound(first);
            channel.writeInbound(last);
            assertFalse(channel.isActive());
            verifyNoInteractions(decoder);
            assertEquals(0, first.refCnt());
            assertEquals(0, last.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void testFragmentedHttpUpgradeIsAggregated() {
        var channel = handshakeChannel();
        try {
            channel.writeInbound(Unpooled.copiedBuffer("GET /ws HTTP/1.1\r\nHost: localhost\r\n"
                    + "Connection: Upgrade\r\nUpgrade: websocket\r\n", StandardCharsets.US_ASCII));
            assertNull(channel.readOutbound());
            channel.writeInbound(Unpooled.copiedBuffer("Sec-WebSocket-Version: 13\r\n"
                    + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n", StandardCharsets.US_ASCII));
            assertTrue(response(channel).startsWith("HTTP/1.1 101"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
