package com.tenio.core.benchmark;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.entity.session.manager.SessionManagerImpl;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.security.filter.DefaultConnectionFilter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Benchmark tests for session management operations.
 * This class measures the performance of session creation, retrieval, and removal
 * in the SessionManager component.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SessionManagerBenchmarkTest extends BenchmarkTestBase {

    private SessionManager sessionManager;
    private SocketChannel socketChannel;
    private SelectionKey selectionKey;
    private ConnectionFilter connectionFilter;
    private static final int PACKET_QUEUE_SIZE = 1000;

    @Setup
    public void setup() {
        EventManager eventManager = EventManager.newInstance();
        sessionManager = SessionManagerImpl.newInstance(eventManager);
        socketChannel = mock(SocketChannel.class);
        selectionKey = mock(SelectionKey.class);
        connectionFilter = new DefaultConnectionFilter();
        
        // Configure the session manager
        sessionManager.configureMaxIdleTimeInSeconds(60);
        sessionManager.configurePacketQueueSize(PACKET_QUEUE_SIZE);
        sessionManager.configureConnectionFilter(connectionFilter);
    }

    @Benchmark
    public void benchmarkSessionCreation() {
        // Create a new socket session
        Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    }

    @Benchmark
    public void benchmarkSessionRetrieval() {
        // Create a session first
        Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
        
        // Retrieve the session by socket
        Session retrievedSession = sessionManager.getSessionBySocket(socketChannel);
    }

    @Benchmark
    public void benchmarkSessionRemoval() {
        // Create a session first
        Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
        
        // Remove the session
        sessionManager.removeSession(session);
    }

    @Benchmark
    public void benchmarkSessionIteration() {
        // Create multiple sessions first
        for (int i = 0; i < 10; i++) {
            SocketChannel tempSocketChannel = mock(SocketChannel.class);
            SelectionKey tempSelectionKey = mock(SelectionKey.class);
            sessionManager.createSocketSession(tempSocketChannel, tempSelectionKey);
        }
        
        // Iterate over all sessions
        sessionManager.getSessionIterator().forEachRemaining(session -> {
            // Simulate some operation on each session
            session.getId();
        });
    }

    @Benchmark
    public void benchmarkSessionListRetrieval() {
        // Create multiple sessions first
        for (int i = 0; i < 10; i++) {
            SocketChannel tempSocketChannel = mock(SocketChannel.class);
            SelectionKey tempSelectionKey = mock(SelectionKey.class);
            sessionManager.createSocketSession(tempSocketChannel, tempSelectionKey);
        }
        
        // Get the read-only sessions list
        List<Session> sessionsList = sessionManager.getReadonlySessionsList();
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(SessionManagerBenchmarkTest.class);
    }
} 