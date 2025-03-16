package com.tenio.core.benchmark;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.msgpack.MsgPackUtility;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.system.AbstractSystemCommandHandler;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.entity.Player;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A simplified benchmark test for the command manager functionality.
 * This test doesn't depend on any server components.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class TrulySimplifiedCommandManagerBenchmarkTest {

    private static final String SYSTEM_COMMAND_NAME = "test";
    private static final short CLIENT_COMMAND_CODE = 1;

    private SystemCommandManager systemCommandManager;
    private ClientCommandManager clientCommandManager;
    private TestSystemCommandHandler testSystemCommandHandler;
    private TestClientCommandHandler testClientCommandHandler;

    @Setup
    public void setup() {
        systemCommandManager = new SystemCommandManager();
        clientCommandManager = new ClientCommandManager();
        
        testSystemCommandHandler = new TestSystemCommandHandler();
        testClientCommandHandler = new TestClientCommandHandler();
        
        // Register commands for retrieval and execution tests
        systemCommandManager.registerCommand(SYSTEM_COMMAND_NAME, testSystemCommandHandler);
        clientCommandManager.registerCommand(CLIENT_COMMAND_CODE, testClientCommandHandler);
    }

    @Benchmark
    public void benchmarkSystemCommandRetrieval(Blackhole blackhole) {
        AbstractSystemCommandHandler handler = systemCommandManager.getHandler(SYSTEM_COMMAND_NAME);
        blackhole.consume(handler);
    }

    @Benchmark
    public void benchmarkSystemCommandRegistration(Blackhole blackhole) {
        SystemCommandManager localManager = new SystemCommandManager();
        localManager.registerCommand(SYSTEM_COMMAND_NAME + "-new", new TestSystemCommandHandler());
        blackhole.consume(localManager);
    }

    @Benchmark
    public void benchmarkClientCommandRetrieval(Blackhole blackhole) {
        AbstractClientCommandHandler<Player> handler = clientCommandManager.getHandler(CLIENT_COMMAND_CODE);
        blackhole.consume(handler);
    }

    @Benchmark
    public void benchmarkClientCommandRegistration(Blackhole blackhole) {
        ClientCommandManager localManager = new ClientCommandManager();
        localManager.registerCommand((short)(CLIENT_COMMAND_CODE + 1), new TestClientCommandHandler());
        blackhole.consume(localManager);
    }

    @Benchmark
    public void benchmarkCommandManagerClearing(Blackhole blackhole) {
        SystemCommandManager localSystemManager = new SystemCommandManager();
        ClientCommandManager localClientManager = new ClientCommandManager();
        
        // Register some commands
        localSystemManager.registerCommand("test1", new TestSystemCommandHandler());
        localSystemManager.registerCommand("test2", new TestSystemCommandHandler());
        localClientManager.registerCommand((short)1, new TestClientCommandHandler());
        localClientManager.registerCommand((short)2, new TestClientCommandHandler());
        
        // Clear commands
        localSystemManager.clear();
        localClientManager.clear();
        
        blackhole.consume(localSystemManager);
        blackhole.consume(localClientManager);
    }

    // Custom command handler implementations for testing
    private static class TestSystemCommandHandler extends AbstractSystemCommandHandler {
        @Override
        public void execute(List<String> args) {
            // Do nothing for benchmark
        }
    }

    private static class TestClientCommandHandler extends AbstractClientCommandHandler<Player> {
        @Override
        public void execute(Player player, DataCollection message) {
            // Do nothing for benchmark
        }
    }
} 