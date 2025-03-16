package com.tenio.core.benchmark;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests for event management operations.
 * This class measures the performance of event subscription, publishing, and handling
 * in the EventManager component.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class EventManagerBenchmarkTest extends BenchmarkTestBase {

    private EventManager eventManager;
    private static final ServerEvent TEST_EVENT = ServerEvent.SERVER_EXCEPTION;
    private static final Object[] TEST_PARAMS = new Object[] { "Test Exception" };

    @Setup
    public void setup() {
        eventManager = EventManager.newInstance();
        
        // Register a simple event handler for benchmarking
        eventManager.on(TEST_EVENT, params -> {
            // Simple operation to simulate event handling
            String message = (String) params[0];
            return message.length();
        });
        
        // Subscribe to activate the handlers
        eventManager.subscribe();
    }

    @Benchmark
    public void benchmarkEventSubscription() {
        // Create a temporary event manager for subscription benchmarking
        EventManager tempManager = EventManager.newInstance();
        
        // Register an event handler
        tempManager.on(TEST_EVENT, params -> {
            String message = (String) params[0];
            return message.length();
        });
        
        // Subscribe to activate the handler
        tempManager.subscribe();
    }

    @Benchmark
    public void benchmarkEventEmission() {
        // Emit a single event
        eventManager.emit(TEST_EVENT, TEST_PARAMS);
    }

    @Benchmark
    public void benchmarkMultipleEventEmissions() {
        // Emit multiple events in sequence
        for (int i = 0; i < 10; i++) {
            eventManager.emit(TEST_EVENT, new Object[] { "Test Exception " + i });
        }
    }

    @Benchmark
    public void benchmarkEventSubscriberCheck() {
        // Check if an event has subscribers
        boolean hasSubscriber = eventManager.hasSubscriber(TEST_EVENT);
    }

    @Benchmark
    public void benchmarkCompleteEventCycle() {
        // Create a temporary event manager for the complete cycle
        EventManager tempManager = EventManager.newInstance();
        ServerEvent tempEvent = ServerEvent.SERVER_INITIALIZATION;
        
        // Register
        tempManager.on(tempEvent, params -> {
            String name = (String) params[0];
            return name.toUpperCase();
        });
        
        // Subscribe
        tempManager.subscribe();
        
        // Emit
        Object result = tempManager.emit(tempEvent, new Object[] { "server" });
        
        // Clean up
        tempManager.clear();
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(EventManagerBenchmarkTest.class);
    }
} 