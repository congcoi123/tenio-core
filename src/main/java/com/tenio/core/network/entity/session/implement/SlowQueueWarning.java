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

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Per-session, per-direction warning gate. Queue observations below the threshold rearm the gate;
 * sustained congestion allows one warning every 30 seconds. No timer or background task is used.
 * The gate is created only when a positive warning threshold is configured.
 */
final class SlowQueueWarning {

  static final long REMINDER_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

  private final int threshold;
  private final LongSupplier clock;
  private volatile boolean congested;
  private volatile long lastWarningNanos;

  SlowQueueWarning(int threshold) {
    this(threshold, System::nanoTime);
  }

  SlowQueueWarning(int threshold, LongSupplier clock) {
    this.threshold = threshold;
    this.clock = clock;
  }

  int threshold() {
    return threshold;
  }

  boolean shouldWarn(int depth) {
    if (depth < threshold) {
      if (congested) {
        synchronized (this) {
          congested = false;
        }
      }
      return false;
    }
    long now = clock.getAsLong();
    if (congested && now - lastWarningNanos < REMINDER_INTERVAL_NANOS) {
      return false;
    }
    synchronized (this) {
      if (congested && now - lastWarningNanos < REMINDER_INTERVAL_NANOS) {
        return false;
      }
      lastWarningNanos = now;
      congested = true;
      return true;
    }
  }
}
