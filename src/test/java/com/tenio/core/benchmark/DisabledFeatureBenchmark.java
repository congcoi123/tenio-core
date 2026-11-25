/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.benchmark;

import com.tenio.common.data.DataCollection;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.entity.Player;
import com.tenio.core.network.entity.outbound.packet.implement.OutboundQueueImpl;
import com.tenio.core.network.entity.outbound.packet.implement.PacketImpl;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * Synthetic disabled-path comparison only, not an end-to-end server throughput benchmark. Run the
 * same class with baseline and candidate classes, fixed JDK/heap and several fresh forks.
 */
public final class DisabledFeatureBenchmark {
  private static volatile long sink;
  private static volatile Object packetSink;
  private static final int ITERATIONS = 2_000_000;

  public static void main(String[] args) {
    var queue = OutboundQueueImpl.newInstance();
    queue.configureMaxSize(100);
    queue.configureOutboundQueuePolicy(new DefaultOutboundQueuePolicy());
    var packet = PacketImpl.newInstance();
    packet.setData(new byte[64]);
    var commands = new ClientCommandManager();
    commands.registerCommand(
        (short) 1,
        new AbstractClientCommandHandler<Player, DataCollection>() {
          @Override
          public void execute(Player player, DataCollection message) {
            sink++;
          }
        });
    Runnable fifo =
        () -> {
          for (int i = 0; i < ITERATIONS; i++) {
            queue.put(packet);
            sink += queue.peek().getId();
            sink += queue.take().getId();
          }
        };
    Runnable inline =
        () -> {
          for (int i = 0; i < ITERATIONS; i++) {
            commands.invoke((short) 1, null, null);
          }
        };
    Runnable createPacket =
        () -> {
          for (int i = 0; i < ITERATIONS; i++) {
            packetSink = PacketImpl.newInstance();
          }
        };
    report("packet-creation", createPacket);
    report("fifo", fifo);
    report("inline", inline);
    commands.clear();
    boolean featureWorker =
        Thread.getAllStackTraces().keySet().stream()
            .anyMatch(
                thread ->
                    thread.getName().startsWith("tenio-performance")
                        || thread.getName().equals("tenio-command"));
    if (featureWorker) {
      throw new AssertionError("Disabled feature worker was created");
    }
    System.out.println("disabledFeatureWorkers=0 sink=" + sink);
  }

  private static void report(String label, Runnable work) {
    for (int i = 0; i < 4; i++) {
      work.run();
    }
    var time = new double[7];
    var allocations = new double[7];
    var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    long id = Thread.currentThread().threadId();
    for (int i = 0; i < time.length; i++) {
      long beforeBytes = bean.getThreadAllocatedBytes(id), before = System.nanoTime();
      work.run();
      time[i] = (System.nanoTime() - before) / (double) ITERATIONS;
      allocations[i] = (bean.getThreadAllocatedBytes(id) - beforeBytes) / (double) ITERATIONS;
    }
    Arrays.sort(time);
    Arrays.sort(allocations);
    System.out.printf(
        java.util.Locale.ROOT, "%s ns/op=%.3f bytes/op=%.3f%n", label, time[3], allocations[3]);
  }
}
