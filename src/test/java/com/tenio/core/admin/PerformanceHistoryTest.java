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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerformanceHistoryTest {
  @TempDir Path directory;

  static PerformanceHistory.Sample sample(long time, String boot, double cpu) {
    return new PerformanceHistory.Sample(time, boot, 2, 1, 3, cpu, 1024, 10, 100, 200, 1, 2, 0);
  }

  @Test
  void drainsToDiskAndQueriesUtcRangeAcrossRestarts() throws Exception {
    try (var history = new PerformanceHistory(directory, false)) {
      history.record(sample(1000, history.bootId(), 20));
      assertTrue(history.recent().isEmpty());
    }
    try (var history = new PerformanceHistory(directory, true)) {
      history.record(sample(2000, history.bootId(), 30));
      assertEquals(1, history.recent().size());
    }
    try (var reader = new PerformanceHistory(directory, false)) {
      var all = reader.query(Instant.EPOCH, Instant.ofEpochMilli(3000), 10);
      assertEquals(2, all.size());
      assertNotEquals(all.get(0).bootId(), all.get(1).bootId());
      assertEquals(
          1, reader.query(Instant.ofEpochMilli(1500), Instant.ofEpochMilli(2500), 10).size());
      assertEquals(0, reader.droppedExports());
    }
  }

  @Test
  void downsamplingIsBoundedAndPreservesACpuPeak() throws Exception {
    var data = new StringBuilder("# header\n");
    for (int i = 0; i < 1000; i++) {
      data.append(sample(i, "boot", i == 512 ? 99 : 1).line()).append('\n');
    }
    Files.writeString(directory.resolve("tenio-performance-old.tsv"), data);
    try (var history = new PerformanceHistory(directory, false)) {
      var points = history.query(Instant.EPOCH, Instant.ofEpochMilli(1000), 10);
      assertTrue(points.size() <= 10);
      assertTrue(points.stream().anyMatch(p -> p.cpuPercent() == 99));
      assertEquals(999, points.getLast().timestamp());
      assertThrows(
          IllegalArgumentException.class, () -> history.query(Instant.EPOCH, Instant.now(), 0));
    }
  }

  @Test
  void rotationPreservesUnrelatedFilesAndRetainsAtMostSevenExports() throws Exception {
    Files.writeString(directory.resolve("application.tsv"), "retain");
    for (int i = 0; i < 9; i++) {
      Path file = directory.resolve("tenio-performance-old-" + i + ".tsv");
      Files.writeString(file, sample(i, "old", 1).line());
      Files.setLastModifiedTime(file, FileTime.fromMillis(i));
    }
    try (var history = new PerformanceHistory(directory, false)) {
      history.record(sample(10000, history.bootId(), 1));
    }
    try (var files = Files.list(directory)) {
      assertEquals(
          7,
          files.filter(f -> f.getFileName().toString().startsWith("tenio-performance-")).count());
    }
    assertEquals("retain", Files.readString(directory.resolve("application.tsv")));
  }

  @Test
  void liveRingAndShutdownAdmissionAreBounded() throws Exception {
    var history = new PerformanceHistory(directory, true);
    for (int i = 0; i < 1000; i++) {
      history.record(sample(i, history.bootId(), 1));
    }
    assertEquals(720, history.recent().size());
    history.close();
    history.record(sample(9999, history.bootId(), 1));
    assertEquals(999, history.recent().getLast().timestamp());
  }

  @Test
  void preventsTwoWritersFromRotatingTheSameArchive() throws Exception {
    try (var first = new PerformanceHistory(directory, false)) {
      assertThrows(java.io.IOException.class, () -> new PerformanceHistory(directory, false));
    }
  }
}
