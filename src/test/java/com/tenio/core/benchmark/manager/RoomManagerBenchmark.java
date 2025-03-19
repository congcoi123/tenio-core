package com.tenio.core.benchmark.manager;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.implement.DefaultPlayer;
import com.tenio.core.entity.implement.DefaultRoom;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.entity.manager.implement.RoomManagerImpl;
import com.tenio.core.entity.setting.InitialRoomSetting;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomPlayerSlotGeneratedStrategy;
import com.tenio.core.event.implement.EventManager;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class RoomManagerBenchmark {
    private RoomManager roomManager;
    private Room room;
    private Player player;
    private EventManager eventManager;
    private static int counter = 0;
    private InitialRoomSetting roomSetting;

    @Setup(Level.Iteration)
    public void setup() {
        eventManager = EventManager.newInstance();
        roomManager = RoomManagerImpl.newInstance(eventManager);
        roomManager.configureMaxRooms(10); // Set a smaller max rooms limit for benchmarking
        
        String playerName = "Player" + counter++;
        player = DefaultPlayer.newInstance(playerName);
        
        roomSetting = InitialRoomSetting.Builder.newInstance()
            .setName("Room" + counter)
            .setMaxParticipants(10)
            .setMaxSpectators(5)
            .setActivated(true)
            .setRoomCredentialValidatedStrategy(DefaultRoomCredentialValidatedStrategy.class)
            .setRoomPlayerSlotGeneratedStrategy(DefaultRoomPlayerSlotGeneratedStrategy.class)
            .build();
            
        room = roomManager.createRoomWithOwner(roomSetting, player);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        roomManager.clear(); // Clean up rooms between iterations
    }

    @Benchmark
    public void benchmarkAddRoom(Blackhole blackhole) {
        // First remove any existing rooms to avoid hitting the limit
        roomManager.clear();
        
        // Create a new room setting with a unique name for this iteration
        InitialRoomSetting newRoomSetting = InitialRoomSetting.Builder.newInstance()
            .setName("Room" + counter++)
            .setMaxParticipants(10)
            .setMaxSpectators(5)
            .setActivated(true)
            .setRoomCredentialValidatedStrategy(DefaultRoomCredentialValidatedStrategy.class)
            .setRoomPlayerSlotGeneratedStrategy(DefaultRoomPlayerSlotGeneratedStrategy.class)
            .build();
        
        Room newRoom = roomManager.createRoomWithOwner(newRoomSetting, player);
        blackhole.consume(newRoom);
    }

    @Benchmark
    public void benchmarkRemoveRoom(Blackhole blackhole) {
        Room newRoom = roomManager.createRoomWithOwner(roomSetting, player);
        roomManager.removeRoomById(newRoom.getId());
        blackhole.consume(roomManager);
    }

    @Benchmark
    public void benchmarkGetRoomById(Blackhole blackhole) {
        Room found = roomManager.getRoomById(room.getId());
        blackhole.consume(found);
    }

    @Benchmark
    public void benchmarkContainsRoom(Blackhole blackhole) {
        boolean contains = roomManager.containsRoomId(room.getId());
        blackhole.consume(contains);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RoomManagerBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
} 