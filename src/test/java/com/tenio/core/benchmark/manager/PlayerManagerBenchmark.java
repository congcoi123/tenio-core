package com.tenio.core.benchmark.manager;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.implement.DefaultPlayer;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.implement.PlayerManagerImpl;
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
public class PlayerManagerBenchmark {
    private PlayerManager playerManager;
    private Player player;
    private static int counter = 0;
    private EventManager eventManager;

    @Setup(Level.Iteration)
    public void setup() {
        eventManager = EventManager.newInstance();
        playerManager = PlayerManagerImpl.newInstance(eventManager);
        String playerName = "Player" + counter++;
        player = DefaultPlayer.newInstance(playerName);
    }

    @Benchmark
    public void benchmarkAddPlayer(Blackhole blackhole) {
        playerManager.addPlayer(player);
        blackhole.consume(playerManager);
    }

    @Benchmark
    public void benchmarkRemovePlayer(Blackhole blackhole) {
        playerManager.addPlayer(player);
        playerManager.removePlayerByIdentity(player.getIdentity());
        blackhole.consume(playerManager);
    }

    @Benchmark
    public void benchmarkGetPlayerByName(Blackhole blackhole) {
        playerManager.addPlayer(player);
        Player found = playerManager.getPlayerByIdentity(player.getIdentity());
        blackhole.consume(found);
    }

    @Benchmark
    public void benchmarkContainsPlayer(Blackhole blackhole) {
        playerManager.addPlayer(player);
        boolean contains = playerManager.containsPlayerIdentity(player.getIdentity());
        blackhole.consume(contains);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PlayerManagerBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
} 