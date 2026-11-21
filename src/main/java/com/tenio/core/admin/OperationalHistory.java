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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/** Bounded instance-local history. Export callbacks run on a dedicated daemon worker. */
public final class OperationalHistory implements AutoCloseable {
  public enum Kind { DISCONNECT, RECONNECT, PRESSURE_START, PRESSURE_END }
  public record Entry(long timestamp, UUID instance, long sessionId, String transport,
                      Kind kind, String reason) {}

  private final UUID instance = UUID.randomUUID();
  private final int capacity;
  private final ArrayDeque<Entry> entries = new ArrayDeque<>();
  private final LongAdder lostExports = new LongAdder();
  private ThreadPoolExecutor worker;
  private Consumer<Entry> exporter;
  private boolean closed;

  public OperationalHistory(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("History capacity must be positive");
    }
    this.capacity = capacity;
  }

  public synchronized void enableExport(int backlog, Consumer<Entry> sink) {
    if (worker != null || closed) {
      throw new IllegalStateException("Exporter already configured or history closed");
    }
    exporter = Objects.requireNonNull(sink);
    worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(backlog),
        Thread.ofPlatform().daemon(true).name("tenio-history-export").factory());
  }

  public synchronized void record(long id, String transport, Kind kind, String reason) {
    if (closed) {
      return;
    }
    var entry = new Entry(System.currentTimeMillis(), instance, id, transport, kind, reason);
    if (entries.size() == capacity) {
      entries.removeFirst();
    }
    entries.addLast(entry);
    if (worker != null) {
      try {
        worker.execute(() -> {
          try {
            exporter.accept(entry);
          } catch (RuntimeException failure) {
            lostExports.increment();
          }
        });
      } catch (RejectedExecutionException full) {
        lostExports.increment();
      }
    }
  }

  public synchronized List<Entry> snapshot() {
    return List.copyOf(entries);
  }

  public long droppedExports() {
    return lostExports.sum();
  }

  @Override
  public synchronized void close() {
    closed = true;
    if (worker != null) {
      lostExports.add(worker.shutdownNow().size());
    }
    entries.clear();
  }
}
