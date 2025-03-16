package com.tenio.core.benchmark;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.implement.DefaultPlayer;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.implement.PlayerManagerImpl;
import com.tenio.core.event.implement.EventManager;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests for player management operations.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class PlayerManagementBenchmarkTest extends BenchmarkTestBase {

    private PlayerManager playerManager;
    private List<String> playerNames;
    private static final int NUM_PLAYERS = 1000;

    @Setup
    public void setup() {
        playerManager = PlayerManagerImpl.newInstance(EventManager.newInstance());
        playerNames = new ArrayList<>(NUM_PLAYERS);
        
        // Create player names
        for (int i = 0; i < NUM_PLAYERS; i++) {
            playerNames.add("Player" + i);
        }
    }

    @Benchmark
    public void benchmarkPlayerCreation() {
        Player player = DefaultPlayer.newInstance(playerNames.get(0));
        playerManager.configureInitialPlayer(player);
    }

    @Benchmark
    public void benchmarkPlayerAddition() {
        String playerName = playerNames.get(0);
        if (!playerManager.containsPlayerIdentity(playerName)) {
            playerManager.createPlayer(playerName);
        }
    }

    @Benchmark
    public void benchmarkPlayerRemoval() {
        String playerName = playerNames.get(0);
        if (playerManager.containsPlayerIdentity(playerName)) {
            playerManager.removePlayerByIdentity(playerName);
        }
    }

    @Benchmark
    public void benchmarkPlayerLookup() {
        // Add some players first
        for (int i = 0; i < 10; i++) {
            String playerName = playerNames.get(i);
            if (!playerManager.containsPlayerIdentity(playerName)) {
                playerManager.createPlayer(playerName);
            }
        }

        // Lookup players
        for (int i = 0; i < 10; i++) {
            playerManager.getPlayerByIdentity(playerNames.get(i));
        }
    }

    @Benchmark
    public void benchmarkPlayerIteration() {
        // Add some players first
        for (int i = 0; i < 10; i++) {
            String playerName = playerNames.get(i);
            if (!playerManager.containsPlayerIdentity(playerName)) {
                playerManager.createPlayer(playerName);
            }
        }

        // Iterate over players
        playerManager.getReadonlyPlayersList().forEach(player -> {
            // Simulate some operation on each player
            player.getIdentity();
        });
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(PlayerManagementBenchmarkTest.class);
    }
} 