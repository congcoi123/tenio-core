package com.tenio.core.benchmark.manager;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.implement.SessionImpl;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.entity.session.manager.SessionManagerImpl;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.mockito.Mockito;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SessionManagerBenchmark {
    private SessionManager sessionManager;
    private Session session;
    private EventManager eventManager;
    private SocketChannel socketChannel;
    private SelectionKey selectionKey;

    @Setup(Level.Iteration)
    public void setup() {
        eventManager = EventManager.newInstance();
        sessionManager = SessionManagerImpl.newInstance(eventManager);
        session = SessionImpl.newInstance();
        socketChannel = Mockito.mock(SocketChannel.class);
        selectionKey = Mockito.mock(SelectionKey.class);
    }

    @Benchmark
    public void benchmarkCreateSession(Blackhole blackhole) {
        Session newSession = sessionManager.createSocketSession(socketChannel, selectionKey);
        blackhole.consume(newSession);
    }

    @Benchmark
    public void benchmarkRemoveSession(Blackhole blackhole) {
        Session newSession = sessionManager.createSocketSession(socketChannel, selectionKey);
        sessionManager.removeSession(newSession);
        blackhole.consume(sessionManager);
    }

    @Benchmark
    public void benchmarkGetSession(Blackhole blackhole) {
        Session newSession = sessionManager.createSocketSession(socketChannel, selectionKey);
        Session found = sessionManager.getSessionBySocket(socketChannel);
        blackhole.consume(found);
    }

    @Benchmark
    public void benchmarkContainsSession(Blackhole blackhole) {
        Session newSession = sessionManager.createSocketSession(socketChannel, selectionKey);
        int count = sessionManager.getSessionCount();
        blackhole.consume(count);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SessionManagerBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
} 