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

package com.tenio.core.command.client;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * Optional fixed-size duration histogram; never allocated for unmonitored dispatch.
 */
final class CommandTiming {

    private final LongAdder completed = new LongAdder();
    private final LongAdder failures = new LongAdder();
    private final LongAdder total = new LongAdder();
    private final AtomicLong maximum = new AtomicLong();
    private final AtomicLongArray buckets = new AtomicLongArray(64);

    void record(long nanos, boolean successful) {
        long duration = Math.max(0, nanos);
        int bucket = duration == 0 ? 0 : 63 - Long.numberOfLeadingZeros(duration);
        buckets.incrementAndGet(bucket);
        total.add(duration);
        maximum.accumulateAndGet(duration, Math::max);
        if (!successful) {
            failures.increment();
        }
        completed.increment();
    }

    ClientCommandManager.ExecutionStatistics snapshot(int outstanding) {
        long count = completed.sum();
        long target = (long) Math.ceil(count * 0.95);
        long seen = 0, p95 = 0;
        if (count > 0) {
            for (int i = 0; i < buckets.length(); i++) {
                seen += buckets.get(i);
                if (seen >= target) {
                    p95 = i >= 62 ? Long.MAX_VALUE : 1L << (i + 1);
                    break;
                }
            }
        }
        return new ClientCommandManager.ExecutionStatistics(
                count,
                failures.sum(),
                count == 0 ? 0 : total.sum() / count,
                p95,
                maximum.get(),
                outstanding);
    }
}
