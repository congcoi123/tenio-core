package com.tenio.core.benchmark;

import com.tenio.common.data.DataCollection;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.system.AbstractSystemCommandHandler;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.entity.Player;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Simplified benchmark tests for command management operations.
 * This class measures the performance of command registration, retrieval, and execution
 * in the CommandManager components without relying on server components.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SimplifiedCommandManagerBenchmarkTest {

    private SystemCommandManager systemCommandManager;
    private ClientCommandManager clientCommandManager;
    private AbstractSystemCommandHandler systemCommandHandler;
    private AbstractClientCommandHandler<Player> clientCommandHandler;
    private HashMap<String, Object> mockData;

    @Setup
    public void setup() {
        systemCommandManager = new SystemCommandManager();
        clientCommandManager = new ClientCommandManager();
        
        // Create a simple system command handler
        systemCommandHandler = new AbstractSystemCommandHandler() {
            @Override
            public void execute(List<String> args) {
                // Do nothing for benchmark
            }
        };
        
        // Create a simple client command handler
        clientCommandHandler = new AbstractClientCommandHandler<Player>() {
            @Override
            public void execute(Player player, DataCollection message) {
                // Do nothing for benchmark
            }
        };
        
        // Create mock data
        mockData = new HashMap<>();
        mockData.put("action", "test");
    }

    @Benchmark
    public void benchmarkSystemCommandRegistration(Blackhole blackhole) {
        String commandLabel = "test";
        systemCommandManager.registerCommand(commandLabel, systemCommandHandler);
        blackhole.consume(systemCommandManager);
        systemCommandManager.unregisterCommand(commandLabel);
    }

    @Benchmark
    public void benchmarkSystemCommandRetrieval(Blackhole blackhole) {
        String commandLabel = "test";
        systemCommandManager.registerCommand(commandLabel, systemCommandHandler);
        AbstractSystemCommandHandler handler = systemCommandManager.getHandler(commandLabel);
        blackhole.consume(handler);
        systemCommandManager.unregisterCommand(commandLabel);
    }

    @Benchmark
    public void benchmarkSystemCommandExecution(Blackhole blackhole) {
        String commandLabel = "test";
        systemCommandManager.registerCommand(commandLabel, systemCommandHandler);
        systemCommandManager.invoke("test arg1 arg2");
        blackhole.consume(systemCommandManager);
        systemCommandManager.unregisterCommand(commandLabel);
    }

    @Benchmark
    public void benchmarkClientCommandRegistration(Blackhole blackhole) {
        Short commandCode = 1;
        clientCommandManager.registerCommand(commandCode, clientCommandHandler);
        blackhole.consume(clientCommandManager);
        clientCommandManager.unregisterCommand(commandCode);
    }

    @Benchmark
    public void benchmarkClientCommandRetrieval(Blackhole blackhole) {
        Short commandCode = 1;
        clientCommandManager.registerCommand(commandCode, clientCommandHandler);
        AbstractClientCommandHandler<Player> handler = clientCommandManager.getHandler(commandCode);
        blackhole.consume(handler);
        clientCommandManager.unregisterCommand(commandCode);
    }

    @Benchmark
    public void benchmarkCommandManagerClearing(Blackhole blackhole) {
        // Register multiple commands
        for (int i = 0; i < 10; i++) {
            systemCommandManager.registerCommand("test" + i, systemCommandHandler);
            clientCommandManager.registerCommand((short) i, clientCommandHandler);
        }
        
        // Clear all commands
        systemCommandManager.clear();
        clientCommandManager.clear();
        
        blackhole.consume(systemCommandManager);
        blackhole.consume(clientCommandManager);
    }
} 