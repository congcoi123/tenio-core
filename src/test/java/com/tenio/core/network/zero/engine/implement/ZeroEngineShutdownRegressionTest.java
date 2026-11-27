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
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.zero.engine.manager.SessionTicketsQueueManager;
import com.tenio.core.network.zero.engine.reader.DatagramReaderHandler;
import com.tenio.core.network.zero.engine.reader.SocketReaderHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serial;
import java.lang.reflect.Field;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Selector;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZeroEngineShutdownRegressionTest {

    private LoggerContext context;
    private LoggerConfig previous;
    private ErrorAppender errors;

    private static Field field(Class<?> type, String name) throws Exception {
        var result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static ExecutorService executor(AbstractZeroEngine engine) throws Exception {
        return (ExecutorService) field(AbstractZeroEngine.class, "executorService").get(engine);
    }

    @BeforeEach
    void captureReaderErrors() {
        context = (LoggerContext) LogManager.getContext(false);
        var config = context.getConfiguration();
        previous = config.getLoggers().get(ZeroReaderImpl.class.getName());
        errors = new ErrorAppender();
        errors.start();
        var capture = new LoggerConfig(ZeroReaderImpl.class.getName(), Level.ERROR, false);
        capture.addAppender(errors, Level.ERROR, null);
        config.removeLogger(ZeroReaderImpl.class.getName());
        config.addLogger(ZeroReaderImpl.class.getName(), capture);
        context.updateLoggers();
    }

    @AfterEach
    void restoreLogging() {
        var config = context.getConfiguration();
        config.removeLogger(ZeroReaderImpl.class.getName());
        if (previous != null) {
            config.addLogger(ZeroReaderImpl.class.getName(), previous);
        }
        context.updateLoggers();
        errors.stop();
    }

    @Test
    void shutdownInterruptsAWriterBlockedInTakeBeforeTheTenSecondTimeout() throws Exception {
        var writer = (ZeroWriterImpl) ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.setThreadPoolSize(1);
        writer.initialize();
        var executor = executor(writer);
        var taking = new CountDownLatch(1);
        var exited = new CountDownLatch(1);
        var queue = new LinkedBlockingQueue<Session>() {

            @Serial
            private static final long serialVersionUID = 1185963079315252929L;

            @Override
            public Session take() throws InterruptedException {
                taking.countDown();
                try {
                    return super.take();
                } finally {
                    exited.countDown();
                }
            }
        };
        var queues = mock(SessionTicketsQueueManager.class);
        when(queues.getQueueByIndex(0)).thenReturn(queue);
        field(ZeroWriterImpl.class, "sessionTicketsQueueManager").set(writer, queues);
        writer.activate();
        writer.start();
        try {
            assertTrue(taking.await(5, TimeUnit.SECONDS), "Writer did not reach the blocking take");
            assertTimeoutPreemptively(Duration.ofSeconds(3), writer::shutdown);
            assertTrue(exited.await(1, TimeUnit.SECONDS));
            assertTrue(executor.isTerminated());
            assertTimeoutPreemptively(Duration.ofSeconds(1), writer::shutdown);
        } finally {
            writer.shutdown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void shutdownPreservesTheCallingThreadsInterruptStatus() throws Exception {
        var writer = (ZeroWriterImpl) ZeroWriterImpl.newInstance(mock(EventManager.class));
        writer.initialize();
        var executor = executor(writer);
        try {
            Thread.currentThread().interrupt();
            writer.shutdown();
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void selectorClosedDuringShutdownDoesNotLogAnError(boolean datagram) throws Exception {
        try (var fixture = new ReaderFixture(datagram, Failure.SHUTDOWN_SELECTOR)) {
            fixture.start();
            assertTrue(fixture.reading.await(5, TimeUnit.SECONDS));
            assertTimeoutPreemptively(Duration.ofSeconds(3), fixture.reader::shutdown);
            assertTrue(fixture.executor.isTerminated());
            assertTrue(errors.events.isEmpty(), "Expected selector closure was logged as an error");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unexpectedSelectorClosureStillLogsAnError(boolean datagram) throws Exception {
        try (var fixture = new ReaderFixture(datagram, Failure.UNEXPECTED_SELECTOR)) {
            fixture.start();
            assertTrue(errors.logged.await(5, TimeUnit.SECONDS), "Unexpected selector closure was hidden");
            fixture.reader.shutdown();
            assertEquals(1, errors.events.size());
            assertTrue(errors.events.getFirst().getMessage().getFormattedMessage().contains("ClosedSelectorException"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unrelatedFailuresAreNotHiddenDuringShutdown(boolean datagram) throws Exception {
        try (var fixture = new ReaderFixture(datagram, Failure.UNRELATED)) {
            fixture.start();
            assertTrue(fixture.reading.await(5, TimeUnit.SECONDS));
            fixture.reader.shutdown();
            assertEquals(1, errors.events.size());
            assertTrue(errors.events.getFirst().getMessage().getFormattedMessage().contains("Unrelated reader failure"));
        }
    }

    private enum Failure {
        SHUTDOWN_SELECTOR, UNEXPECTED_SELECTOR, UNRELATED
    }

    private static final class ReaderFixture implements AutoCloseable {

        private final ZeroReaderImpl reader = (ZeroReaderImpl) ZeroReaderImpl.newInstance(mock(EventManager.class));
        private final Selector selector = Selector.open();
        private final CountDownLatch reading = new CountDownLatch(1);
        private final CompletableFuture<Void> closed = new CompletableFuture<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final ExecutorService executor;
        private final Runnable worker;

        private ReaderFixture(boolean datagram, Failure failure) throws Exception {
            reader.initialize();
            executor = executor(reader);
            Runnable shutdown = () -> {
                try {
                    selector.close();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                } finally {
                    closed.complete(null);
                }
            };
            org.mockito.stubbing.Answer<Void> run = invocation -> {
                int count = calls.incrementAndGet();
                if (failure == Failure.UNEXPECTED_SELECTOR) {
                    if (count == 1) {
                        selector.close();
                        throw new ClosedSelectorException();
                    }
                    closed.join();
                    return null;
                }
                reading.countDown();
                if (failure == Failure.UNRELATED) {
                    closed.join();
                    throw new IllegalStateException("Unrelated reader failure");
                }
                selector.select();
                // Ensure shutdown has finished closing before reproducing selectedKeys().
                closed.join();
                selector.selectedKeys();
                return null;
            };
            if (datagram) {
                var handler = mock(DatagramReaderHandler.class);
                doAnswer(run).when(handler).running();
                doAnswer(invocation -> {
                    shutdown.run();
                    return null;
                }).when(handler).shutdown();
                field(ZeroReaderImpl.class, "datagramReaderHandlers").set(reader, List.of(handler));
                var method = ZeroReaderImpl.class.getDeclaredMethod("runDatagramReaderHandler", DatagramReaderHandler.class);
                method.setAccessible(true);
                worker = () -> {
                    try {
                        method.invoke(reader, handler);
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError(exception);
                    }
                };
            } else {
                var handler = mock(SocketReaderHandler.class);
                doAnswer(run).when(handler).running();
                doAnswer(invocation -> {
                    shutdown.run();
                    return null;
                }).when(handler).shutdown();
                field(ZeroReaderImpl.class, "socketReaderHandlers").set(reader, List.of(handler));
                worker = reader;
            }
        }

        private void start() {
            reader.activate();
            executor.execute(worker);
        }

        @Override
        public void close() throws Exception {
            reader.shutdown();
            closed.complete(null);
            selector.close();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static final class ErrorAppender extends AbstractAppender {

        private final List<LogEvent> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch logged = new CountDownLatch(1);

        private ErrorAppender() {
            super("ZeroEngineShutdownRegressionTest", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
            logged.countDown();
        }
    }
}
