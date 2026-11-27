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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CommandDispatcherTest {

    static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    @Test
    void sharesOrderedLaneAndBoundsQueuedPlusRunning() {
        try (var dispatcher = new CommandDispatcher(4, 2)) {
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var done = new CountDownLatch(2);
            var order = new CopyOnWriteArrayList<Integer>();
            assertTrue(dispatcher.submit(
                    "room-1",
                    2,
                    () -> {
                        started.countDown();
                        await(release);
                        order.add(1);
                        done.countDown();
                    })
            );
            await(started);
            assertTrue(dispatcher.submit(
                    "room-1",
                    2,
                    () -> {
                        order.add(2);
                        done.countDown();
                    })
            );
            assertFalse(dispatcher.submit("room-1", 2, () -> fail("not admitted")));
            assertEquals(2, dispatcher.outstanding());
            release.countDown();
            await(done);
            assertEquals(List.of(1, 2), order);
        }
    }

    @Test
    void unrelatedLanesRunConcurrentlyButAdmissionIsBounded() {
        try (var dispatcher = new CommandDispatcher(3, 2)) {
            var started = new CountDownLatch(2);
            var release = new CountDownLatch(1);
            var completed = new CountDownLatch(3);
            var failures = new CopyOnWriteArrayList<Throwable>();
            Runnable work = () -> {
                started.countDown();
                try {
                    await(release);
                } catch (Throwable failure) {
                    // Assertions in a worker must fail this JUnit test, not just print on stderr.
                    failures.add(failure);
                } finally {
                    completed.countDown();
                }
            };
            try {
                assertTrue(dispatcher.submit("a", 2, work));
                assertTrue(dispatcher.submit("b", 2, work));
                await(started);
                assertFalse(dispatcher.submit(null, 2, work));
                assertTrue(dispatcher.submit("a", 2, completed::countDown));
                assertFalse(dispatcher.submit("b", 2, () -> {
                }));
            } finally {
                release.countDown();
            }
            // close() deliberately interrupts running work. Finish this admission test first;
            // the separate shutdown test below checks interruption and queued-work cancellation.
            await(completed);
            assertTrue(failures.isEmpty(), () -> "Dispatcher worker failed: " + failures);
        }
    }

    @Test
    void closeDiscardsQueuedWorkAndInterruptsRunningWork() {
        var dispatcher = new CommandDispatcher(3, 1);
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var calls = new AtomicInteger();
        assertTrue(dispatcher.submit(
                "key",
                3,
                () -> {
                    started.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException expected) {
                        interrupted.countDown();
                    }
                })
        );
        await(started);
        assertTrue(dispatcher.submit("key", 3, calls::incrementAndGet));
        dispatcher.close();
        await(interrupted);
        assertEquals(0, calls.get());
        assertEquals(0, dispatcher.outstanding());
        assertFalse(dispatcher.submit("new", 3, calls::incrementAndGet));
        dispatcher.close();
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new CommandDispatcher(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new CommandDispatcher(1, 2));
        try (var dispatcher = new CommandDispatcher(2, 1)) {
            assertThrows(IllegalArgumentException.class, () -> dispatcher.submit("x", 0, () -> {
            }));
        }
    }
}
