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

package com.tenio.core.admin;

import com.tenio.common.logger.SystemLogger;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optional bounded asynchronous performance export. Files contain no player/session identities.
 * Rotation retains at most seven completed/current files in the configured dedicated directory. A
 * writer is created only when the performance profile is selected. Querying never runs on the EDT.
 */
public final class PerformanceHistory extends SystemLogger implements AutoCloseable {
  /**
   * One UTC sample. Rates are measured over actual elapsed monotonic time, not assumed intervals.
   *
   * @param timestamp epoch milliseconds
   * @param bootId unique server-start identifier
   * @param players current players
   * @param rooms current rooms
   * @param sessions current transport sessions
   * @param cpuPercent process CPU percentage, or -1 if unavailable
   * @param heapBytes used JVM heap
   * @param gcMillis cumulative GC time
   * @param readBytesPerSecond inbound serialized/network byte rate
   * @param writtenBytesPerSecond outbound byte rate
   * @param inboundDrops cumulative inbound drops
   * @param outboundDrops cumulative outbound capacity/policy drops
   * @param commandOutstanding opted-in queued plus running commands
   */
  public record Sample(
      long timestamp,
      String bootId,
      int players,
      int rooms,
      int sessions,
      double cpuPercent,
      long heapBytes,
      long gcMillis,
      long readBytesPerSecond,
      long writtenBytesPerSecond,
      long inboundDrops,
      long outboundDrops,
      int commandOutstanding,
      long commandP95Nanos,
      long commandMaximumNanos,
      long commandFailures) {
    /** Creates a sample without optional command duration metrics. */
    public Sample(
        long timestamp,
        String bootId,
        int players,
        int rooms,
        int sessions,
        double cpuPercent,
        long heapBytes,
        long gcMillis,
        long readBytesPerSecond,
        long writtenBytesPerSecond,
        long inboundDrops,
        long outboundDrops,
        int commandOutstanding) {
      this(
          timestamp,
          bootId,
          players,
          rooms,
          sessions,
          cpuPercent,
          heapBytes,
          gcMillis,
          readBytesPerSecond,
          writtenBytesPerSecond,
          inboundDrops,
          outboundDrops,
          commandOutstanding,
          0,
          0,
          0);
    }

    String line() {
      return timestamp
          + "\t"
          + bootId
          + "\t"
          + players
          + "\t"
          + rooms
          + "\t"
          + sessions
          + "\t"
          + cpuPercent
          + "\t"
          + heapBytes
          + "\t"
          + gcMillis
          + "\t"
          + readBytesPerSecond
          + "\t"
          + writtenBytesPerSecond
          + "\t"
          + inboundDrops
          + "\t"
          + outboundDrops
          + "\t"
          + commandOutstanding
          + "\t"
          + commandP95Nanos
          + "\t"
          + commandMaximumNanos
          + "\t"
          + commandFailures;
    }

    static Sample parse(String line) {
      var p = line.split("\t");
      if (p.length != 13 && p.length != 16) {
        throw new IllegalArgumentException("Invalid performance sample");
      }
      return new Sample(
          Long.parseLong(p[0]),
          p[1],
          Integer.parseInt(p[2]),
          Integer.parseInt(p[3]),
          Integer.parseInt(p[4]),
          Double.parseDouble(p[5]),
          Long.parseLong(p[6]),
          Long.parseLong(p[7]),
          Long.parseLong(p[8]),
          Long.parseLong(p[9]),
          Long.parseLong(p[10]),
          Long.parseLong(p[11]),
          Integer.parseInt(p[12]),
          p.length == 16 ? Long.parseLong(p[13]) : 0,
          p.length == 16 ? Long.parseLong(p[14]) : 0,
          p.length == 16 ? Long.parseLong(p[15]) : 0);
    }
  }

  private static final String PREFIX = "tenio-performance-";
  private static final int QUEUE_CAPACITY = 256;
  private static final int RETAINED_FILES = 7;
  private static final long FILE_BYTES = 8 * 1024 * 1024;
  private static final int MEMORY_SAMPLES = 720;
  private final ArrayBlockingQueue<Sample> pending = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
  private final ArrayDeque<Sample> recent;
  private final Path directory;
  private final String bootId = UUID.randomUUID().toString();
  private final AtomicLong lost = new AtomicLong();
  private final Thread writer;
  private final java.nio.channels.FileChannel lockChannel;
  private final java.nio.channels.FileLock archiveLock;
  private volatile boolean closing;
  private int fileIndex;
  private long fileSize;
  private BufferedWriter output;

  /**
   * Opens a dedicated export directory. A live ring is retained only when a monitor is available.
   *
   * @param directory performance-only output directory
   * @param retainRecent enable a bounded in-memory ring for local inspection
   * @throws IOException if the directory cannot be prepared
   */
  public PerformanceHistory(Path directory, boolean retainRecent) throws IOException {
    this.directory = directory;
    recent = retainRecent ? new ArrayDeque<>() : null;
    Files.createDirectories(directory);
    lockChannel =
        java.nio.channels.FileChannel.open(
            directory.resolve(".tenio-performance.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE);
    java.nio.channels.FileLock acquired;
    try {
      acquired = lockChannel.tryLock();
    } catch (java.nio.channels.OverlappingFileLockException unavailable) {
      lockChannel.close();
      throw new IOException("Performance directory is already in use", unavailable);
    }
    if (acquired == null) {
      lockChannel.close();
      throw new IOException("Performance directory is already in use");
    }
    archiveLock = acquired;
    writer =
        Thread.ofPlatform().daemon(true).name("tenio-performance-export").unstarted(this::drain);
    writer.start();
  }

  /**
   * @return the server-start identifier included in every sample
   */
  public String bootId() {
    return bootId;
  }

  /**
   * @return output directory
   */
  public Path directory() {
    return directory;
  }

  /**
   * @return samples lost due to a full export queue or I/O failure
   */
  public long droppedExports() {
    return lost.get();
  }

  /** Records a sample without waiting for disk I/O. */
  public synchronized void record(Sample sample) {
    if (closing) {
      return;
    }
    if (recent != null) {
      if (recent.size() == MEMORY_SAMPLES) {
        recent.removeFirst();
      }
      recent.addLast(sample);
    }
    if (!pending.offer(sample)) {
      lost.incrementAndGet();
    }
  }

  /**
   * @return immutable recent samples, or an empty list in file-only mode
   */
  public synchronized List<Sample> recent() {
    return recent == null ? List.of() : List.copyOf(recent);
  }

  /**
   * Reads a bounded-size retained archive for an explicit local investigation. At most 2,000 points
   * are returned. When downsampling, each bucket retains its busiest CPU sample so spikes are
   * visible; the last sample is retained. This is a historical approximation.
   *
   * @param from inclusive UTC start
   * @param to inclusive UTC end
   * @param maximumPoints desired chart/table bound, between 2 and 2000
   * @return chronologically sorted points from all retained boots
   * @throws IOException when archive access fails
   */
  public List<Sample> query(Instant from, Instant to, int maximumPoints) throws IOException {
    if (maximumPoints < 2 || maximumPoints > 2000 || to.isBefore(from)) {
      throw new IllegalArgumentException("Invalid performance query range or point limit");
    }
    var buckets = new java.util.TreeMap<Integer, Sample>();
    Sample latest = null;
    long first = from.toEpochMilli(), last = to.toEpochMilli();
    double width = Math.max(1.0, ((double) last - first + 1) / (maximumPoints - 1));
    for (Path file : files()) {
      try (var lines = Files.lines(file)) {
        var iterator = lines.iterator();
        while (iterator.hasNext()) {
          String line = iterator.next();
          if (line.startsWith("#")) {
            continue;
          }
          try {
            var sample = Sample.parse(line);
            if (sample.timestamp < first || sample.timestamp > last) {
              continue;
            }
            if (latest == null || sample.timestamp > latest.timestamp) {
              latest = sample;
            }
            int bucket = Math.min(maximumPoints - 2, (int) ((sample.timestamp - first) / width));
            var previous = buckets.get(bucket);
            if (previous == null || sample.cpuPercent >= previous.cpuPercent) {
              buckets.put(bucket, sample);
            }
          } catch (IllegalArgumentException malformed) {
            /* Ignore a crashed/incomplete final line. */
          }
        }
      } catch (java.nio.file.NoSuchFileException rotated) {
        /* File rotated between list and open. */
      }
    }
    var result = new ArrayList<>(buckets.values());
    if (latest != null && !result.contains(latest)) {
      result.add(latest);
    }
    result.sort(Comparator.comparingLong(Sample::timestamp));
    return List.copyOf(result);
  }

  private List<Path> files() throws IOException {
    try (var files = Files.list(directory)) {
      return files
          .filter(
              p ->
                  p.getFileName().toString().startsWith(PREFIX)
                      && p.getFileName().toString().endsWith(".tsv")
                      && Files.isRegularFile(p))
          .sorted(Comparator.comparingLong(this::modified))
          .toList();
    }
  }

  private long modified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (IOException missing) {
      return 0;
    }
  }

  private void open() throws IOException {
    if (output != null) {
      output.close();
    }
    var file = directory.resolve(PREFIX + bootId + "-" + fileIndex++ + ".tsv");
    output = Files.newBufferedWriter(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    String header =
        "# epochMillis\tbootId\tplayers\trooms\tsessions\tcpuPercent\theapBytes\tgcMillis"
            + "\treadBytesPerSecond\twrittenBytesPerSecond\tinboundDrops\toutboundDrops"
            + "\tcommandOutstanding\tcommandP95Nanos\tcommandMaximumNanos\tcommandFailures";
    output.write(header);
    output.newLine();
    fileSize = header.length() + 1;
    var files = files();
    for (int i = 0; i < files.size() - RETAINED_FILES; i++) {
      Files.deleteIfExists(files.get(i));
    }
  }

  private void drain() {
    try {
      while (!closing || !pending.isEmpty()) {
        Sample sample;
        try {
          sample = pending.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
          continue;
        }
        if (sample == null) {
          continue;
        }
        try {
          if (output == null || fileSize >= FILE_BYTES) {
            open();
          }
          String line = sample.line();
          output.write(line);
          output.newLine();
          output.flush();
          fileSize += line.length() + 1;
        } catch (IOException failure) {
          lost.incrementAndGet();
          if (isErrorEnabled()) {
            error(failure, "Performance export failed");
          }
          try {
            if (output != null) {
              output.close();
            }
          } catch (IOException ignored) {
          }
          output = null;
        }
      }
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException failure) {
        if (isErrorEnabled()) {
          error(failure);
        }
      }
      try {
        archiveLock.release();
        lockChannel.close();
      } catch (IOException failure) {
        if (isErrorEnabled()) {
          error(failure);
        }
      }
    }
  }

  /** Stops sampling admission and drains exports, waiting at most five seconds. */
  @Override
  public void close() {
    synchronized (this) {
      closing = true;
    }
    writer.interrupt();
    try {
      writer.join(5000);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
    if (writer.isAlive() && isErrorEnabled()) {
      error("Performance export did not finish before shutdown deadline");
    }
  }
}
