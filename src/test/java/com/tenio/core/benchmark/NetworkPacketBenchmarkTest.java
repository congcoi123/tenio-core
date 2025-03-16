package com.tenio.core.benchmark;

import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.implement.PacketImpl;
import com.tenio.core.network.entity.packet.implement.PacketQueueImpl;
import com.tenio.core.network.entity.packet.policy.DefaultPacketQueuePolicy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark tests for network packet handling performance.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class NetworkPacketBenchmarkTest extends BenchmarkTestBase {

    private PacketQueueImpl packetQueue;
    private DefaultPacketQueuePolicy policy;
    private static final int QUEUE_SIZE = 1000;
    private static final int PACKET_DATA_SIZE = 1024; // 1KB

    @Setup
    public void setup() {
        packetQueue = PacketQueueImpl.newInstance();
        packetQueue.configureMaxSize(QUEUE_SIZE);
        policy = new DefaultPacketQueuePolicy();
        packetQueue.configurePacketQueuePolicy(policy);
    }

    @Benchmark
    public void benchmarkPacketCreation() {
        Packet packet = PacketImpl.newInstance();
        packet.setData(new byte[PACKET_DATA_SIZE]);
        packet.setPriority(ResponsePriority.NORMAL);
        packet.setTransportType(TransportType.TCP);
    }

    @Benchmark
    public void benchmarkPacketQueueInsertion() {
        Packet packet = PacketImpl.newInstance();
        packet.setData(new byte[PACKET_DATA_SIZE]);
        packet.setPriority(ResponsePriority.NORMAL);
        packet.setTransportType(TransportType.TCP);
        policy.applyPolicy(packetQueue, packet);
    }

    @Benchmark
    public void benchmarkPacketQueueProcessing() {
        // Fill queue
        for (int i = 0; i < 100; i++) {
            Packet packet = PacketImpl.newInstance();
            packet.setData(new byte[PACKET_DATA_SIZE]);
            packet.setPriority(ResponsePriority.NORMAL);
            packet.setTransportType(TransportType.TCP);
            policy.applyPolicy(packetQueue, packet);
        }

        // Process queue
        while (!packetQueue.isEmpty()) {
            packetQueue.take();
        }
    }

    public static void main(String[] args) throws RunnerException {
        runBenchmark(NetworkPacketBenchmarkTest.class);
    }
} 