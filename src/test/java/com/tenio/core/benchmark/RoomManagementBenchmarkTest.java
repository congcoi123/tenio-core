package com.tenio.core.benchmark;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.implement.DefaultPlayer;
import com.tenio.core.entity.implement.DefaultRoom;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests for room management operations.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RoomManagementBenchmarkTest extends BenchmarkTestBase {

    private Room room;
    private List<Player> players;
    private static final int MAX_PLAYERS = 100;
    private static final String ROOM_NAME = "BenchmarkRoom";

    @Setup
    public void setup() {
        room = DefaultRoom.newInstance();
        room.setName(ROOM_NAME);
        room.setMaxParticipants(MAX_PLAYERS);
        room.setMaxSpectators(MAX_PLAYERS / 2);
        room.setRoomRemoveMode(RoomRemoveMode.WHEN_EMPTY);
        
        players = new ArrayList<>(MAX_PLAYERS);
        
        // Create test players
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player player = DefaultPlayer.newInstance("Player" + i);
            players.add(player);
        }
    }

    @Benchmark
    public void benchmarkPlayerJoinRoom() {
        Player player = players.get(0);
        if (!room.containsPlayerIdentity(player.getIdentity())) {
            room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        }
    }

    @Benchmark
    public void benchmarkPlayerLeaveRoom() {
        Player player = players.get(0);
        if (room.containsPlayerIdentity(player.getIdentity())) {
            room.removePlayer(player);
        }
    }

    @Benchmark
    public void benchmarkRoomPlayerIteration() {
        // Add some players first
        for (int i = 0; i < 10; i++) {
            if (!room.containsPlayerIdentity(players.get(i).getIdentity())) {
                room.addPlayer(players.get(i), null, false, Room.DEFAULT_SLOT);
            }
        }

        // Iterate over players
        room.getReadonlyPlayersList().forEach(player -> {
            // Simulate some operation on each player
            player.getIdentity();
        });
    }

    @Benchmark
    public void benchmarkRoomStateUpdate() {
        // Add some players
        for (int i = 0; i < 10; i++) {
            if (!room.containsPlayerIdentity(players.get(i).getIdentity())) {
                room.addPlayer(players.get(i), null, false, Room.DEFAULT_SLOT);
            }
        }

        // Update room state
        room.setName(ROOM_NAME + "_Updated");
        room.getReadonlyPlayersList().forEach(player -> {
            // Simulate state update for each player
            player.setProperty("lastUpdate", System.currentTimeMillis());
        });
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(RoomManagementBenchmarkTest.class);
    }
} 