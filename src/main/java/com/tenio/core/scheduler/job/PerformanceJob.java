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

package com.tenio.core.scheduler.job;

import com.tenio.core.admin.PerformanceHistory;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.bootstrap.annotation.Scheduled;
import com.tenio.core.configuration.constant.CoreConstant;
import com.tenio.core.network.Network;
import com.tenio.core.server.Server;
import java.lang.management.ManagementFactory;

/** Samples optional performance history on the Injector-owned scheduler. */
@Component(profile = CoreConstant.PERFORMANCE_PROFILE)
public final class PerformanceJob {
  private static final int SAMPLE_SECONDS = 5;
  private PerformanceHistory history;
  private Server server;
  private Network network;
  private long previousTime;
  private long previousRead;
  private long previousWritten;

  /** Binds counters before scheduling starts. */
  public void configure(Server server, Network network, PerformanceHistory history) {
    this.server = server;
    this.network = network;
    this.history = history;
  }

  /** Samples process gauges and rate deltas without scanning sessions or room members. */
  @Scheduled(
      threadName = "RECORD_PERFORMANCE_HISTORY",
      intervalSeconds = SAMPLE_SECONDS,
      initialDelaySeconds = SAMPLE_SECONDS)
  public void sample() {
    if (history == null) {
      return;
    }
    long now = System.nanoTime();
    long read = network.getNetworkReaderStatistic().getReadBytes();
    long written = network.getNetworkWriterStatistic().getWrittenBytes();
    double seconds = previousTime == 0 ? 0 : (now - previousTime) / 1_000_000_000.0;
    long readRate = seconds <= 0 ? 0 : (long) (Math.max(0, read - previousRead) / seconds);
    long writtenRate = seconds <= 0 ? 0 : (long) (Math.max(0, written - previousWritten) / seconds);
    previousTime = now;
    previousRead = read;
    previousWritten = written;
    var os = ManagementFactory.getOperatingSystemMXBean();
    double cpu =
        os instanceof com.sun.management.OperatingSystemMXBean extended
            ? extended.getProcessCpuLoad()
            : -1;
    long gc =
        ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(bean -> Math.max(0, bean.getCollectionTime()))
            .sum();
    var writer = network.getNetworkWriterStatistic();
    var commands = server.getClientCommandManager();
    var execution = commands == null ? null : commands.executionStatistics();
    history.record(
        new PerformanceHistory.Sample(
            System.currentTimeMillis(),
            history.bootId(),
            server.getPlayerManager().getSnapshotPlayerCount(),
            server.getRoomManager().getSnapshotRoomCount(),
            network.getSessionManager().getSnapshotSessionCount(),
            cpu < 0 ? -1 : cpu * 100,
            ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(),
            gc,
            readRate,
            writtenRate,
            network.getNetworkReaderStatistic().getReadDroppedPackets(),
            writer.getWrittenDroppedPacketsByFull() + writer.getWrittenDroppedPacketsByPolicy(),
            execution == null ? 0 : execution.outstanding(),
            execution == null ? 0 : execution.p95UpperBoundNanos(),
            execution == null ? 0 : execution.maximumNanos(),
            execution == null ? 0 : execution.failures()));
  }
}
