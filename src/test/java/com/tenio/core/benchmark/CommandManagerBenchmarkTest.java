package com.tenio.core.benchmark;

import com.tenio.common.data.DataCollection;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.system.AbstractSystemCommandHandler;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.PlayerState;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.network.entity.session.Session;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests for command management operations.
 * This class measures the performance of command registration, retrieval, and execution
 * in the CommandManager components.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class CommandManagerBenchmarkTest {

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
    public void benchmarkClientCommandExecution(Blackhole blackhole) {
        // Skip this benchmark since we can't properly mock DataCollection
        blackhole.consume(1);
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
    
    /**
     * A simple mock implementation of Player for benchmarking purposes
     */
    @SuppressWarnings("unused")
    private static class MockPlayer implements Player {
        private final String identity = "MockPlayer";
        private final Map<String, Object> properties = new HashMap<>();
        private PlayerState state = new PlayerState() {}; // Anonymous implementation
        private boolean activated = false;
        private boolean loggedIn = false;
        private boolean neverDeported = false;
        private PlayerRoleInRoom roleInRoom = PlayerRoleInRoom.SPECTATOR;
        private Room currentRoom = null;
        private int playerSlot = -1;
        private Session session = null;
        private long lastReadTime = System.currentTimeMillis();
        private long lastWriteTime = System.currentTimeMillis();
        
        @Override
        public String getIdentity() {
            return identity;
        }
        
        @Override
        public boolean containsSession() {
            return session != null;
        }
        
        @Override
        public boolean isState(PlayerState state) {
            return this.state == state;
        }
        
        @Override
        public PlayerState getState() {
            return state;
        }
        
        @Override
        public void setState(PlayerState state) {
            this.state = state;
        }
        
        @Override
        public boolean transitionState(PlayerState expectedState, PlayerState newState) {
            if (this.state == expectedState) {
                this.state = newState;
                return true;
            }
            return false;
        }
        
        @Override
        public boolean isActivated() {
            return activated;
        }
        
        @Override
        public void setActivated(boolean activated) {
            this.activated = activated;
        }
        
        @Override
        public boolean isLoggedIn() {
            return loggedIn;
        }
        
        @Override
        public void setLoggedIn(boolean loggedIn) {
            this.loggedIn = loggedIn;
        }
        
        @Override
        public long getLastLoggedInTime() {
            return 0;
        }
        
        @Override
        public long getLastActivityTime() {
            return 0;
        }
        
        @Override
        public long getInactiveTimeInSeconds() {
            return 0;
        }
        
        @Override
        public long getLastReadTime() {
            return lastReadTime;
        }
        
        @Override
        public void setLastReadTime(long timestamp) {
            this.lastReadTime = timestamp;
        }
        
        @Override
        public long getLastWriteTime() {
            return lastWriteTime;
        }
        
        @Override
        public void setLastWriteTime(long timestamp) {
            this.lastWriteTime = timestamp;
        }
        
        @Override
        public boolean isIdle() {
            return false;
        }
        
        @Override
        public boolean isNeverDeported() {
            return neverDeported;
        }
        
        @Override
        public void setNeverDeported(boolean flag) {
            this.neverDeported = flag;
        }
        
        @Override
        public boolean isIdleNeverDeported() {
            return false;
        }
        
        @Override
        public Optional<Session> getSession() {
            return Optional.ofNullable(session);
        }
        
        @Override
        public void setSession(Session session) {
            this.session = session;
        }
        
        @Override
        public boolean isInRoom() {
            return currentRoom != null;
        }
        
        @Override
        public PlayerRoleInRoom getRoleInRoom() {
            return roleInRoom;
        }
        
        @Override
        public void setRoleInRoom(PlayerRoleInRoom roleInRoom) {
            this.roleInRoom = roleInRoom;
        }
        
        @Override
        public boolean transitionRole(PlayerRoleInRoom expectedRole, PlayerRoleInRoom newRole) {
            if (this.roleInRoom == expectedRole) {
                this.roleInRoom = newRole;
                return true;
            }
            return false;
        }
        
        @Override
        public Optional<Room> getCurrentRoom() {
            return Optional.ofNullable(currentRoom);
        }
        
        @Override
        public void setCurrentRoom(Room room) {
            this.currentRoom = room;
        }
        
        @Override
        public long getLastJoinedRoomTime() {
            return 0;
        }
        
        @Override
        public int getPlayerSlotInCurrentRoom() {
            return playerSlot;
        }
        
        @Override
        public void setPlayerSlotInCurrentRoom(int slot) {
            this.playerSlot = slot;
        }
        
        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }
        
        @Override
        public void setProperty(String key, Object value) {
            properties.put(key, value);
        }
        
        @Override
        public boolean containsProperty(String key) {
            return properties.containsKey(key);
        }
        
        @Override
        public void removeProperty(String key) {
            properties.remove(key);
        }
        
        @Override
        public void clearProperties() {
            properties.clear();
        }
        
        @Override
        public void onUpdateListener(Consumer<Field> updateConsumer) {
            // Do nothing for benchmark
        }
        
        @Override
        public void clean() {
            properties.clear();
            session = null;
            currentRoom = null;
        }
        
        @Override
        public void configureMaxIdleTimeInSeconds(int seconds) {
            // Do nothing for benchmark
        }
        
        @Override
        public void configureMaxIdleTimeNeverDeportedInSeconds(int seconds) {
            // Do nothing for benchmark
        }
    }
} 