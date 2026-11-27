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

package com.tenio.core.network.entity.session.implement;

import com.tenio.common.data.DataCollection;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.InboundQueueFullException;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.implement.OutboundQueueImpl;
import com.tenio.core.network.entity.outbound.packet.implement.PacketImpl;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.netty.NettyWebSocketImpl;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.engine.ZeroWriter;
import com.tenio.core.network.zero.engine.implement.ZeroWriterImpl;
import com.tenio.core.network.zero.engine.writer.WriterHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SlowQueueWarningLoggingTest {

    private final List<LoggerConfig> previous = new ArrayList<>();
    private final List<LoggerConfig> captures = new ArrayList<>();
    private final List<String> names = List.of(SessionImpl.class.getName(), ZeroWriterImpl.class.getName(), NettyWebSocketImpl.class.getName());
    private LoggerContext context;
    private RecordingAppender appender;

    @BeforeEach
    void captureWarnings() {
        context = (LoggerContext) LogManager.getContext(false);
        var configuration = context.getConfiguration();
        appender = new RecordingAppender();
        appender.start();
        for (String name : names) {
            previous.add(configuration.getLoggers().get(name));
            var capture = new LoggerConfig(name, Level.WARN, false);
            capture.addAppender(appender, Level.WARN, null);
            captures.add(capture);
            configuration.removeLogger(name);
            configuration.addLogger(name, capture);
        }
        context.updateLoggers();
    }

    @AfterEach
    void restoreLogging() {
        var configuration = context.getConfiguration();
        for (int i = 0; i < names.size(); i++) {
            configuration.removeLogger(names.get(i));
            if (previous.get(i) != null) {
                configuration.addLogger(names.get(i), previous.get(i));
            }
        }
        context.updateLoggers();
        appender.stop();
    }

    @Test
    void inboundWarnsAtThresholdThenSuppressesUntilReminder() throws Exception {
        var session = spy(SessionImpl.newInstance());
        session.configureSlowConsumingInboundQueueWarningThreshold(1);
        var time = installGate(session, "inboundQueueWarning", 1);
        var message = mock(DataCollection.class);
        session.enqueueInbound(message);
        assertEquals(0, warnings("Inbound"));
        session.enqueueInbound(message);
        assertEquals(1, warnings("Inbound"));
        doThrow(new AssertionError("Suppressed warnings must not format the session"))
                .when(session)
                .toString();
        for (int i = 0; i < 50; i++) {
            session.enqueueInbound(message);
        }
        assertEquals(1, warnings("Inbound"));
        doCallRealMethod().when(session).toString();
        time.addAndGet(SlowQueueWarning.REMINDER_INTERVAL_NANOS);
        session.enqueueInbound(message);
        assertEquals(2, warnings("Inbound"));
    }

    @Test
    void inboundRecoveryRearmsAfterTheRealConsumerDrainsTheQueue() throws Exception {
        var session = SessionImpl.newInstance();
        var manager = mock(SessionManager.class);
        session.configureSessionManager(manager);
        session.configureSlowConsumingInboundQueueWarningThreshold(1);
        installGate(session, "inboundQueueWarning", 1);
        var drained = new java.util.concurrent.CountDownLatch(2);
        var release = new java.util.concurrent.CompletableFuture<Void>();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call -> {
            drained.countDown();
            if (calls.incrementAndGet() == 2) {
                release.join();
            }
            return null;
        })
                .when(manager)
                .emitEvent(
                        eq(ServerEvent.SESSION_READ_MESSAGE),
                        eq(session),
                        any(DataCollection.class));
        session.enqueueInbound(mock(DataCollection.class));
        session.enqueueInbound(mock(DataCollection.class));
        assertEquals(1, warnings("Inbound"));
        try {
            session.activate();
            assertTrue(drained.await(5, java.util.concurrent.TimeUnit.SECONDS));
            session.enqueueInbound(mock(DataCollection.class));
            session.enqueueInbound(mock(DataCollection.class));
            assertEquals(2, warnings("Inbound"));
        } finally {
            release.complete(null);
            session.close();
        }
    }

    @Test
    void inboundWarningSuppressionPreservesCapacityRejections() {
        var session = SessionImpl.newInstance();
        session.configureSlowConsumingInboundQueueWarningThreshold(1);
        session.configureMaxInboundQueueSize(1);
        var message = mock(DataCollection.class);
        session.enqueueInbound(message);
        assertThrows(InboundQueueFullException.class, () -> session.enqueueInbound(message));
        assertThrows(InboundQueueFullException.class, () -> session.enqueueInbound(message));
        assertEquals(1, warnings("Inbound"));
    }

    @Test
    void outboundWarningsAreRateLimitedWithoutConsumingDiagnosticReads() throws Exception {
        var session = SessionImpl.newInstance();
        var queue = mock(OutboundQueue.class);
        session.configureOutboundQueue(queue);
        session.configureSlowConsumingOutboundQueueWarningThreshold(2);
        var time = installGate(session, "outboundQueueWarning", 2);
        when(queue.getSnapshotSize()).thenReturn(2);
        assertEquals(2, session.getRemainingSlowConsumingOutboundQueue());
        assertEquals(2, session.getRemainingSlowConsumingOutboundQueue());
        assertEquals(2, session.pollSlowConsumingOutboundQueueWarning());
        assertEquals(0, session.pollSlowConsumingOutboundQueueWarning());
        time.addAndGet(SlowQueueWarning.REMINDER_INTERVAL_NANOS);
        assertEquals(2, session.pollSlowConsumingOutboundQueueWarning());
        when(queue.getSnapshotSize()).thenReturn(1);
        assertEquals(0, session.pollSlowConsumingOutboundQueueWarning());
        when(queue.getSnapshotSize()).thenReturn(2);
        assertEquals(2, session.pollSlowConsumingOutboundQueueWarning());
    }

    @Test
    void nonPositiveThresholdsAllocateNoGatesAndDoNotReadOutboundQueue() throws Exception {
        var session = SessionImpl.newInstance();
        var queue = mock(OutboundQueue.class);
        session.configureOutboundQueue(queue);
        for (int threshold : new int[]{0, -1}) {
            session.configureSlowConsumingInboundQueueWarningThreshold(threshold);
            session.configureSlowConsumingOutboundQueueWarningThreshold(threshold);
            assertNull(field(session, "inboundQueueWarning"));
            assertNull(field(session, "outboundQueueWarning"));
            session.enqueueInbound(mock(DataCollection.class));
            assertEquals(0, session.getRemainingSlowConsumingOutboundQueue());
            assertEquals(0, session.pollSlowConsumingOutboundQueueWarning());
        }
        verifyNoInteractions(queue);
        assertEquals(0, warnings("Inbound"));
    }

    @Test
    void disablingInboundWarnLoggingSkipsClockAndDoesNotConsumeWarning() throws Exception {
        var session = SessionImpl.newInstance();
        session.configureSlowConsumingInboundQueueWarningThreshold(1);
        var reads = new AtomicLong();
        setField(session, "inboundQueueWarning", new SlowQueueWarning(1, reads::incrementAndGet));
        captures.getFirst().setLevel(Level.ERROR);
        context.updateLoggers();
        session.enqueueInbound(mock(DataCollection.class));
        session.enqueueInbound(mock(DataCollection.class));
        assertEquals(0, reads.get());
        captures.getFirst().setLevel(Level.WARN);
        context.updateLoggers();
        session.enqueueInbound(mock(DataCollection.class));
        assertEquals(1, reads.get());
        assertEquals(1, warnings("Inbound"));
    }

    @Test
    void writerUsesTheWarningGateAndSkipsItWhenWarnIsDisabled() throws Exception {
        var session = spy(SessionImpl.newInstance());
        when(session.isActivated()).thenReturn(true);
        var queue = mock(OutboundQueue.class);
        when(queue.getSnapshotSize()).thenReturn(1);
        session.configureOutboundQueue(queue);
        session.configureSlowConsumingOutboundQueueWarningThreshold(1);
        var time = installGate(session, "outboundQueueWarning", 1);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.setNetworkWriterStatistic(mock(NetworkWriterStatistic.class));
        writer.setThreadPoolSize(1);
        writer.onInitialized();
        var packet = PacketImpl.newInstance();
        packet.setRecipients(List.of(session));
        enqueue(writer, session, packet);
        enqueue(writer, session, packet);
        assertEquals(1, warnings("Outbound"));
        time.addAndGet(SlowQueueWarning.REMINDER_INTERVAL_NANOS);
        enqueue(writer, session, packet);
        assertEquals(2, warnings("Outbound"));
        verify(session, times(3)).pollSlowConsumingOutboundQueueWarning();
        captures.get(1).setLevel(Level.ERROR);
        context.updateLoggers();
        enqueue(writer, session, packet);
        verify(session, times(3)).pollSlowConsumingOutboundQueueWarning();
        verify(queue, times(4)).put(packet);
    }

    @Test
    void sessionsDoNotSuppressEachOthersInboundWarnings() {
        for (int i = 0; i < 2; i++) {
            var session = SessionImpl.newInstance();
            session.configureSlowConsumingInboundQueueWarningThreshold(1);
            session.enqueueInbound(mock(DataCollection.class));
            session.enqueueInbound(mock(DataCollection.class));
        }
        assertEquals(2, warnings("Inbound"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fullQueuesLogBoundedWarningsWithoutStackTracesAndKeepAllDropCounts(boolean bestEffort) throws Exception {
        var session = spy(SessionImpl.newInstance());
        when(session.isActivated()).thenReturn(true);
        session.enableInboundQueueMonitoring();
        session.configureMaxInboundQueueSize(1);
        session.configureSlowConsumingInboundQueueWarningThreshold(0);
        session.configureSlowConsumingOutboundQueueWarningThreshold(0);
        var time = new AtomicLong();
        setField(session, "fullQueueWarnings", new SessionImpl.FullQueueWarnings(time::get));
        var queue = OutboundQueueImpl.newInstance();
        queue.configureMaxSize(1);
        queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
        queue.enableMonitoring(null, session.getId(), "TCP");
        queue.put(PacketImpl.newInstance());
        session.configureOutboundQueue(queue);
        var stats = mock(NetworkWriterStatistic.class);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.setNetworkWriterStatistic(stats);
        var packet = PacketImpl.newInstance();
        if (bestEffort) {
            packet.setDeliveryOptions(DeliveryOptions.bestEffort(Duration.ZERO));
        }
        session.enqueueInbound(mock(DataCollection.class));
        for (int i = 0; i < 5; i++) {
            assertThrows(InboundQueueFullException.class, () -> session.enqueueInbound(mock(DataCollection.class)));
            enqueue(writer, session, packet);
        }
        assertEquals(1, fullWarnings("Inbound"));
        assertEquals(1, fullWarnings("Outbound"));
        assertEquals(5, session.inboundQueueStatistics().fullRejections());
        assertEquals(5, session.getDroppedPackets());
        assertEquals(5, queue.statistics().fullDrops());
        verify(stats, times(5)).updateWrittenDroppedPacketsByFull(1);
        time.addAndGet(SlowQueueWarning.REMINDER_INTERVAL_NANOS);
        assertThrows(InboundQueueFullException.class, () -> session.enqueueInbound(mock(DataCollection.class)));
        enqueue(writer, session, packet);
        assertEquals(2, fullWarnings("Inbound"));
        assertEquals(2, fullWarnings("Outbound"));
        assertEquals(6, session.inboundQueueStatistics().fullRejections());
        assertEquals(6, session.getDroppedPackets());
        assertTrue(appender.events.stream().noneMatch(event -> event.getLevel() == Level.ERROR));
        for (var event : appender.events) {
            assertNull(event.getThrown());
            assertFalse(event.getMessage().getFormattedMessage().contains("Exception"));
            assertFalse(event.getMessage().getFormattedMessage().contains("\tat "));
        }
    }

    @Test
    void warnDisabledCapacityRejectionsDoNotAllocateWarningState() throws Exception {
        for (var capture : captures) {
            capture.setLevel(Level.ERROR);
        }
        context.updateLoggers();
        var session = spy(SessionImpl.newInstance());
        when(session.isActivated()).thenReturn(true);
        session.configureMaxInboundQueueSize(1);
        session.enqueueInbound(mock(DataCollection.class));
        assertThrows(InboundQueueFullException.class, () -> session.enqueueInbound(mock(DataCollection.class)));
        var queue = mock(OutboundQueue.class);
        Mockito.doThrow(new com.tenio.core.exception.OutboundQueueFullException(1)).when(queue).put(any(Packet.class));
        session.configureOutboundQueue(queue);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.setNetworkWriterStatistic(mock(NetworkWriterStatistic.class));
        enqueue(writer, session, PacketImpl.newInstance());
        assertNull(field(session, "fullQueueWarnings"));
        assertEquals(0, appender.events.size());
        captures.getFirst().setLevel(Level.WARN);
        context.updateLoggers();
        assertThrows(InboundQueueFullException.class, () -> session.enqueueInbound(mock(DataCollection.class)));
        Assertions.assertNotNull(field(session, "fullQueueWarnings"));
        assertEquals(1, fullWarnings("Inbound"));
    }

    @Test
    void udpPolicyRejectionsAreSilentAndRemainPolicyDrops() {
        var session = spy(SessionImpl.newInstance());
        when(session.isActivated()).thenReturn(true);
        var queue = OutboundQueueImpl.newInstance();
        queue.configureMaxSize(4);
        queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
        queue.enableMonitoring(null, session.getId(), "TCP");
        for (int i = 0; i < 3; i++) {
            queue.put(PacketImpl.newInstance());
        }
        session.configureOutboundQueue(queue);
        var stats = mock(NetworkWriterStatistic.class);
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.setNetworkWriterStatistic(stats);
        var udp = PacketImpl.newInstance();
        udp.setTransportType(TransportType.UDP);
        for (int i = 0; i < 10; i++) {
            enqueue(writer, session, udp);
        }
        assertEquals(0, appender.events.size());
        assertEquals(10, session.getDroppedPackets());
        assertEquals(10, queue.statistics().policyDrops());
        assertEquals(0, queue.statistics().fullDrops());
        verify(stats, times(10)).updateWrittenDroppedPacketsByPolicy(1);
    }

    @Test
    void webSocketCapacityDropsUseTheSameLazyRateLimitedWarning() throws Exception {
        var session = spy(SessionImpl.newInstance());
        when(session.isActivated()).thenReturn(true);
        var channel = mock(io.netty.channel.Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 1234));
        session.configureWebSocketChannel(channel);
        var stats = mock(NetworkWriterStatistic.class);
        var webSocket = NettyWebSocketImpl.newInstance(mock(EventManager.class));
        webSocket.setNetworkWriterStatistic(stats);
        var packet = PacketImpl.newInstance();
        packet.setRecipients(List.of(session));
        captures.get(2).setLevel(Level.ERROR);
        context.updateLoggers();
        for (int i = 0; i < 10; i++) {
            webSocket.write(packet);
        }
        assertNull(field(session, "fullQueueWarnings"));
        assertEquals(0, appender.events.size());
        captures.get(2).setLevel(Level.WARN);
        context.updateLoggers();
        for (int i = 0; i < 10; i++) {
            webSocket.write(packet);
        }
        assertEquals(1, fullWarnings("Outbound"));
        assertEquals(20, session.getDroppedPackets());
        verify(stats, times(20)).updateWrittenDroppedPacketsByFull(1);
        verify(channel, Mockito.never()).writeAndFlush(any());
    }

    @Test
    void unexpectedWriterFailuresStillLogTheirStackTracesEveryTime() throws Exception {
        var writer = ZeroWriterImpl.newInstance(mock(EventManager.class));
        @SuppressWarnings("unchecked")
        var tickets = (BlockingQueue<Session>) mock(BlockingQueue.class);
        when(tickets.take()).thenThrow(new IllegalStateException("unexpected-writer-failure"));
        var method = ZeroWriterImpl.class.getDeclaredMethod("writing", BlockingQueue.class, WriterHandler.class, WriterHandler.class);
        method.setAccessible(true);
        for (int i = 0; i < 2; i++) {
            method.invoke(writer, tickets, mock(WriterHandler.class), mock(WriterHandler.class));
        }
        assertEquals(2, appender.events.stream().filter(event -> event.getLevel() == Level.ERROR
                && event.getMessage().getFormattedMessage().contains("unexpected-writer-failure")
                && event.getMessage().getFormattedMessage().contains("java.lang.IllegalStateException")
                && event.getMessage().getFormattedMessage().contains("\tat ")).count()
        );
    }

    private long fullWarnings(String direction) {
        return appender.events.stream().filter(event -> event.getLevel() == Level.WARN
                && event.getMessage().getFormattedMessage().contains("[" + direction + " Queue Full]")).count();
    }

    private void enqueue(ZeroWriter writer, Session session, Packet packet) {
        packet.setRecipients(List.of(session));
        writer.enqueuePacket(packet);
    }

    private long warnings(String direction) {
        return appender.events.stream().filter(event ->
                        event.getLevel() == Level.WARN
                                && event.getMessage().getFormattedMessage().contains("Slow Consuming " + direction))
                .count();
    }

    private AtomicLong installGate(Session session, String name, int threshold) throws Exception {
        var time = new AtomicLong();
        setField(session, name, new SlowQueueWarning(threshold, time::get));
        return time;
    }

    private Object field(Session session, String name) throws Exception {
        var field = SessionImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(session);
    }

    private void setField(Session session, String name, Object value) throws Exception {
        var field = SessionImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(session, value);
    }

    private static final class RecordingAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<>();

        private RecordingAppender() {
            super(
                    "SlowQueueWarningLoggingTest",
                    null,
                    PatternLayout.createDefaultLayout(),
                    false,
                    Property.EMPTY_ARRAY
            );
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
