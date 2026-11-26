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

package com.tenio.core.network.entity.session.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class SlowQueueWarningTest {

  @Test
  void thresholdAndRecoveryDoNotReadTheClock() {
    var calls = new AtomicInteger();
    var gate =
        new SlowQueueWarning(
            10,
            () -> {
              calls.incrementAndGet();
              return 0;
            });
    assertEquals(10, gate.threshold());
    assertFalse(gate.shouldWarn(9));
    assertEquals(0, calls.get());
    assertTrue(gate.shouldWarn(10));
    assertFalse(gate.shouldWarn(100));
    assertFalse(gate.shouldWarn(0));
    assertEquals(2, calls.get());
    assertTrue(gate.shouldWarn(10));
  }

  @Test
  void remindersUseTheExactIntervalAndRecoveryRearmsImmediately() {
    var time = new AtomicLong();
    var gate = new SlowQueueWarning(2, time::get);
    assertTrue(gate.shouldWarn(2));
    time.set(SlowQueueWarning.REMINDER_INTERVAL_NANOS - 1);
    assertFalse(gate.shouldWarn(10));
    time.incrementAndGet();
    assertTrue(gate.shouldWarn(10));
    assertFalse(gate.shouldWarn(10));
    assertFalse(gate.shouldWarn(1));
    assertTrue(gate.shouldWarn(2));
  }

  @Test
  void gatesAreIndependentAcrossSessionsAndDirections() {
    var time = new AtomicLong();
    var inbound = new SlowQueueWarning(1, time::get);
    var outbound = new SlowQueueWarning(1, time::get);
    var otherSession = new SlowQueueWarning(1, time::get);
    assertTrue(inbound.shouldWarn(1));
    assertTrue(outbound.shouldWarn(1));
    assertTrue(otherSession.shouldWarn(1));
    assertFalse(inbound.shouldWarn(1));
    assertFalse(outbound.shouldWarn(1));
    assertFalse(otherSession.shouldWarn(1));
  }

  @Test
  void concurrentEnqueuesAllowOneWarningPerInterval() throws Exception {
    var time = new AtomicLong();
    var gate = new SlowQueueWarning(1, time::get);
    assertEquals(1, warningsFromConcurrentCalls(gate));
    assertEquals(0, warningsFromConcurrentCalls(gate));
    time.addAndGet(SlowQueueWarning.REMINDER_INTERVAL_NANOS);
    assertEquals(1, warningsFromConcurrentCalls(gate));
  }

  @Test
  void monotonicClockWraparoundDoesNotSkipOrRepeatReminders() {
    var time = new AtomicLong(Long.MAX_VALUE - 10);
    var gate = new SlowQueueWarning(1, time::get);
    assertTrue(gate.shouldWarn(1));
    time.addAndGet(SlowQueueWarning.REMINDER_INTERVAL_NANOS - 1);
    assertFalse(gate.shouldWarn(1));
    time.incrementAndGet();
    assertTrue(gate.shouldWarn(1));
  }

  private int warningsFromConcurrentCalls(SlowQueueWarning gate) throws Exception {
    var start = new CountDownLatch(1);
    var ready = new CountDownLatch(32);
    var results = new ArrayList<java.util.concurrent.Future<Boolean>>();
    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < 32; i++) {
        results.add(
            workers.submit(
                () -> {
                  ready.countDown();
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  return gate.shouldWarn(10);
                }));
      }
      try {
        assertTrue(ready.await(5, TimeUnit.SECONDS));
      } finally {
        start.countDown();
      }
      int count = 0;
      for (var result : results) {
        if (result.get(5, TimeUnit.SECONDS)) {
          count++;
        }
      }
      return count;
    }
  }
}
