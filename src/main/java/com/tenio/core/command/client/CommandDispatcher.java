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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Bounded virtual-thread dispatch. Ordered mailboxes share keys across command handlers. No thread
 * is created until a request is accepted. Closing discards queued work and interrupts active work;
 * handlers must cooperate with interruption and use external I/O timeouts.
 */
public final class CommandDispatcher implements AutoCloseable {

    private final int capacity;
    private final int concurrency;
    private final Map<Object, ArrayDeque<Runnable>> lanes = new HashMap<>();
    private final Set<Thread> workers = new HashSet<>();
    private int outstanding;
    private boolean closed;

    /**
     * Creates admission limits shared by all commands using this dispatcher.
     *
     * @param capacity    maximum queued plus running invocations
     * @param concurrency maximum active workers, including ordered lanes
     */
    public CommandDispatcher(int capacity, int concurrency) {
        if (capacity <= 0 || concurrency <= 0 || concurrency > capacity) {
            throw new IllegalArgumentException("Expected 0 < concurrency <= capacity");
        }
        this.capacity = capacity;
        this.concurrency = concurrency;
    }

    /**
     * Admits work without blocking its caller. A null key selects independent concurrent work.
     *
     * @param key          immutable lane key, or null
     * @param laneCapacity queued plus running bound for this lane
     * @param work         invocation; errors are forwarded to the thread uncaught-exception handler
     * @return whether work was admitted
     */
    public synchronized boolean submit(Object key, int laneCapacity, Runnable work) {
        Objects.requireNonNull(work, "work");
        if (laneCapacity <= 0) {
            throw new IllegalArgumentException("Lane capacity must be positive");
        }
        if (closed || outstanding >= capacity) {
            return false;
        }
        Object laneKey = key == null ? new Object() : key;
        var queue = lanes.get(laneKey);
        if (queue != null) {
            // The running invocation remains at the head until it completes.
            if (queue.size() >= laneCapacity) {
                return false;
            }
            queue.add(work);
            outstanding++;
            return true;
        }
        if (workers.size() >= concurrency) {
            return false;
        }
        queue = new ArrayDeque<>();
        queue.add(work);
        lanes.put(laneKey, queue);
        outstanding++;
        var owned = queue;
        Thread worker = Thread.ofVirtual().name("tenio-command").unstarted(() -> drain(laneKey, owned));
        workers.add(worker);
        try {
            worker.start();
        } catch (RuntimeException | Error failure) {
            workers.remove(worker);
            lanes.remove(laneKey);
            outstanding--;
            throw failure;
        }
        return true;
    }

    private void drain(Object key, ArrayDeque<Runnable> owned) {
        try {
            while (true) {
                Runnable work;
                synchronized (this) {
                    var lane = lanes.get(key);
                    if (closed || lane == null || lane.isEmpty()) {
                        return;
                    }
                    work = lane.peek();
                }
                try {
                    work.run();
                } catch (Throwable failure) {
                    Thread thread = Thread.currentThread();
                    thread.getUncaughtExceptionHandler().uncaughtException(thread, failure);
                }
                synchronized (this) {
                    var lane = lanes.get(key);
                    if (lane == null) {
                        return;
                    }
                    lane.remove();
                    outstanding--;
                    if (lane.isEmpty()) {
                        lanes.remove(key);
                        return;
                    }
                }
            }
        } finally {
            synchronized (this) {
                // Release the slot atomically with removal of the lane, including exceptional exits.
                var abandoned = lanes.get(key) == owned ? lanes.remove(key) : null;
                if (abandoned != null) {
                    outstanding -= abandoned.size();
                }
                workers.remove(Thread.currentThread());
                notifyAll();
            }
        }
    }

    /**
     * @return queued plus running work at this instant
     */
    public synchronized int outstanding() {
        return closed ? workers.size() : outstanding;
    }

    /**
     * Stops admission and queued work, interrupts workers, and waits up to five seconds in total.
     */
    @Override
    public void close() {
        Set<Thread> running;
        synchronized (this) {
            closed = true;
            lanes.clear();
            outstanding = 0;
            running = Set.copyOf(workers);
        }
        running.forEach(Thread::interrupt);
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        for (Thread worker : running) {
            if (worker == Thread.currentThread()) {
                continue;
            }
            long remaining = until - System.nanoTime();
            if (remaining <= 0) {
                break;
            }
            try {
                worker.join(Duration.ofNanos(remaining));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
