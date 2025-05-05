package com.tenio.core.benchmark.network.packet;

import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.implement.PacketImpl;
import com.tenio.core.network.entity.packet.implement.PacketQueueImpl;
import com.tenio.core.network.entity.packet.policy.DefaultPacketQueuePolicy;
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
public class PacketQueueBenchmark {
    private PacketQueue packetQueue;
    private Packet normalPacket;
    private Packet highPriorityPacket;
    private static int counter = 0;
    private static final int MAX_QUEUE_SIZE = 100;

    @Setup(Level.Iteration)
    public void setup() {
        // Create queue with default policy
        packetQueue = PacketQueueImpl.newInstance();
        packetQueue.configureMaxSize(MAX_QUEUE_SIZE);
        packetQueue.configurePacketQueuePolicy(new DefaultPacketQueuePolicy());

        // Create normal priority packet
        normalPacket = PacketImpl.newInstance();
        normalPacket.setData(("NormalData" + counter++).getBytes());
        normalPacket.setPriority(ResponsePriority.NORMAL);
        normalPacket.setTransportType(TransportType.TCP);

        // Create high priority packet
        highPriorityPacket = PacketImpl.newInstance();
        highPriorityPacket.setData(("HighPriorityData" + counter++).getBytes());
        highPriorityPacket.setPriority(ResponsePriority.GUARANTEED_QUICKEST);
        highPriorityPacket.setTransportType(TransportType.TCP);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        packetQueue.clear();
    }

    @Benchmark
    public void benchmarkPutNormalPriority(Blackhole blackhole) {
        packetQueue.put(normalPacket);
        blackhole.consume(packetQueue);
    }

    @Benchmark
    public void benchmarkPutHighPriority(Blackhole blackhole) {
        packetQueue.put(highPriorityPacket);
        blackhole.consume(packetQueue);
    }

    @Benchmark
    public void benchmarkTakeFromQueue(Blackhole blackhole) {
        packetQueue.put(normalPacket);
        Packet taken = packetQueue.take();
        blackhole.consume(taken);
    }

    @Benchmark
    public void benchmarkPeekFromQueue(Blackhole blackhole) {
        packetQueue.put(normalPacket);
        Packet peeked = packetQueue.peek();
        blackhole.consume(peeked);
    }

    @Benchmark
    public void benchmarkQueueOperations(Blackhole blackhole) {
        // Test multiple operations in sequence
        packetQueue.put(normalPacket);
        packetQueue.put(highPriorityPacket);
        blackhole.consume(packetQueue.getSize());
        blackhole.consume(packetQueue.take());
        blackhole.consume(packetQueue.peek());
        blackhole.consume(packetQueue.getPercentageUsed());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PacketQueueBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}