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

package com.tenio.core.network.entity.outbound.packet.implement;

import com.tenio.core.admin.OperationalHistory;
import com.tenio.core.exception.OutboundQueueFullException;
import com.tenio.core.exception.OutboundQueuePolicyViolationException;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

/**
 * The implementation for outbound queue.
 *
 * @see OutboundQueue
 */
public final class OutboundQueueImpl implements OutboundQueue {

    private final Queue<Packet> queue;
    private volatile int snapshotSize;
    private OutboundQueuePolicy outboundQueuePolicy;
    private int maxSize;
    private Monitor monitor;
    private Packet claimed;
    private Delivery delivery;

    /**
     * Constructor.
     */
    private OutboundQueueImpl() {
        queue = new LinkedList<>();
    }

    /**
     * Creates a new instance of an outbound queue.
     *
     * @return a new instance of {@link OutboundQueue}
     */
    public static OutboundQueueImpl newInstance() {
        return new OutboundQueueImpl();
    }

    @Override
    public Packet peek() {
        synchronized (queue) {
            if (delivery == null) {
                return queue.peek();
            }
            purgeExpired();
            claimed = queue.peek();
            return claimed;
        }
    }

    @Override
    public Packet take() {
        synchronized (queue) {
            Packet packet = queue.poll();
            if (delivery != null && packet != null) {
                forget(packet);
            }
            if (packet == claimed) {
                claimed = null;
            }
            snapshotSize = queue.size();
            if (monitor != null) {
                updatePressure();
            }
            return packet;
        }
    }

    @Override
    public boolean isSnapshotEmpty() {
        return snapshotSize == 0;
    }

    @Override
    public boolean isEmpty() {
        synchronized (queue) {
            snapshotSize = queue.size();
            return isSnapshotEmpty();
        }
    }

    @Override
    public boolean isSnapshotFull() {
        return snapshotSize >= maxSize;
    }

    @Override
    public boolean isFull() {
        synchronized (queue) {
            snapshotSize = queue.size();
            return isSnapshotFull();
        }
    }

    @Override
    public int getSnapshotSize() {
        return snapshotSize;
    }

    @Override
    public int getSize() {
        synchronized (queue) {
            snapshotSize = queue.size();
            return getSnapshotSize();
        }
    }

    @Override
    public void configureMaxSize(int maxSize) {
        if (maxSize <= 0) {
            maxSize = Integer.MAX_VALUE;
        }
        this.maxSize = maxSize;
    }

    @Override
    public void enableMonitoring(boolean diagnostics, OperationalHistory history, long id, String transport) {
        monitor = diagnostics || history != null ? new Monitor(diagnostics, history, id, transport) : null;
    }

    @Override
    public void configureOutboundQueuePolicy(OutboundQueuePolicy outboundQueuePolicy) {
        this.outboundQueuePolicy = outboundQueuePolicy;
    }

    @Override
    public float getPercentageUsed() {
        return maxSize == 0 ? 0.0f : (((float) snapshotSize * 100) / maxSize);
    }

    @Override
    public void put(Packet packet) {
        putWithDelivery(packet);
    }

    @Override
    public Admission putWithDelivery(Packet packet) {
       Objects.requireNonNull(packet, "packet");
        synchronized (queue) {
            if (delivery != null || packet.getDeliveryOptions() != null) {
                return enhancedPut(packet);
            }
            snapshotSize = queue.size();
            if (isSnapshotFull()) {
                if (monitor != null && monitor.diagnostics) {
                    monitor.fullDrops++;
                }
                throw new OutboundQueueFullException(snapshotSize);
            }
            try {
                outboundQueuePolicy.applyPolicy(this, packet);
            } catch (OutboundQueuePolicyViolationException rejected) {
                if (monitor != null && monitor.diagnostics) {
                    monitor.policyDrops++;
                }
                throw rejected;
            } catch (OutboundQueueFullException full) {
                if (monitor != null && monitor.diagnostics) {
                    monitor.fullDrops++;
                }
                throw full;
            }
            queue.add(packet);
            snapshotSize = queue.size();
            if (monitor != null) {
                if (monitor.diagnostics) {
                    monitor.highWater = Math.max(monitor.highWater, snapshotSize);
                }
                updatePressure();
            }
            return Admission.ENQUEUED;
        }
    }

    @Override
    public void clear() {
        synchronized (queue) {
            queue.clear();
            claimed = null;
            if (delivery != null) {
                delivery.sizes.clear();
                delivery.references.clear();
                delivery.bytes = 0;
            }
            snapshotSize = 0;
            if (monitor != null) {
                updatePressure();
            }
        }
    }

    @Override
    public void configureDeliveryByteLimit(long maximumBytes) {
        if (maximumBytes < 0) {
            throw new IllegalArgumentException("Negative byte limit");
        }
        synchronized (queue) {
            activateDelivery();
            delivery.maximumBytes = maximumBytes;
        }
    }

    private void activateDelivery() {
        if (delivery == null) {
            delivery = new Delivery();
            for (var packet : queue) {
                remember(packet);
            }
        }
    }

    private void remember(Packet packet) {
        int size = packet.getData() == null ? 0 : packet.getData().length;
        delivery.sizes.put(packet, size);
        delivery.references.merge(packet, 1, Integer::sum);
        delivery.bytes += size;
    }

    private void forget(Packet packet) {
        Integer references = delivery.references.get(packet);
        if (references == null) {
            return;
        }
        delivery.bytes -= delivery.sizes.get(packet);
        if (references == 1) {
            delivery.references.remove(packet);
            delivery.sizes.remove(packet);
        } else {
            delivery.references.put(packet, references - 1);
        }
    }

    private boolean removable(Packet packet) {
        return packet != claimed && !packet.isFragmented() && !packet.isMarkedAsLast();
    }

    private void purgeExpired() {
        long now = System.nanoTime();
        var iterator = queue.iterator();
        while (iterator.hasNext()) {
            var packet = iterator.next();
            long deadline = packet.getDeliveryDeadlineNanos();
            if (removable(packet) && deadline != 0 && now - deadline >= 0) {
                iterator.remove();
                forget(packet);
                if (monitor != null && monitor.diagnostics) {
                    delivery.expired++;
                }
            }
        }
        snapshotSize = queue.size();
        if (monitor != null) {
            updatePressure();
        }
    }

    private Admission enhancedPut(Packet packet) {
        activateDelivery();
        purgeExpired();
        var options = packet.getDeliveryOptions();
        if (options != null
                && packet.getDeliveryDeadlineNanos() != 0
                && System.nanoTime() - packet.getDeliveryDeadlineNanos() >= 0
                && !packet.isMarkedAsLast()
                && !packet.isFragmented()) {
            if (monitor != null && monitor.diagnostics) {
                delivery.expired++;
            }
            return Admission.EXPIRED;
        }
        Packet replacement = null;
        if (options != null
                && options.mode() == DeliveryOptions.Mode.REPLACEABLE
                && !packet.isMarkedAsLast()) {
            for (var pending : queue) {
                var previous = pending.getDeliveryOptions();
                if (removable(pending)
                        && previous != null
                        && previous.mode() == options.mode()
                        && options.key().equals(previous.key())
                        && packet.getTransportType() == pending.getTransportType()) {
                    replacement = pending;
                    break;
                }
            }
        }
        int packetBytes = packet.getData() == null ? 0 : packet.getData().length;
        long replacedBytes = replacement == null ? 0 : delivery.sizes.get(replacement);
        int finalSize = queue.size() + (replacement == null ? 1 : 0);
        if (finalSize > maxSize
                || (delivery.maximumBytes > 0
                && delivery.bytes - replacedBytes + packetBytes > delivery.maximumBytes)) {
            if (monitor != null && monitor.diagnostics) {
                monitor.fullDrops++;
            }
            if (options != null
                    && options.mode() == DeliveryOptions.Mode.BEST_EFFORT
                    && !packet.isMarkedAsLast()) {
                if (monitor != null && monitor.diagnostics) {
                    delivery.bestEffortDrops++;
                }
                return Admission.DROPPED;
            }
            throw new OutboundQueueFullException(snapshotSize);
        }
        int position = replacement == null ? -1 : ((LinkedList<Packet>) queue).indexOf(replacement);
        if (replacement != null) {
            queue.remove(replacement);
            snapshotSize = queue.size();
        }
        try {
            outboundQueuePolicy.applyPolicy(this, packet);
        } catch (RuntimeException rejected) {
            if (replacement != null) {
                ((LinkedList<Packet>) queue).add(position, replacement);
            }
            snapshotSize = queue.size();
            if (monitor != null && monitor.diagnostics) {
                if (rejected instanceof OutboundQueueFullException) {
                    monitor.fullDrops++;
                } else if (rejected instanceof OutboundQueuePolicyViolationException) {
                    monitor.policyDrops++;
                }
            }
            throw rejected;
        }
        if (replacement != null) {
            forget(replacement);
            if (monitor != null && monitor.diagnostics) {
                delivery.replaced++;
            }
        }
        queue.add(packet);
        remember(packet);
        snapshotSize = queue.size();
        if (monitor != null) {
            if (monitor.diagnostics) {
                monitor.highWater = Math.max(monitor.highWater, snapshotSize);
            }
            updatePressure();
        }
        return Admission.ENQUEUED;
    }

    @Override
    public DeliveryStatistics deliveryStatistics() {
        synchronized (queue) {
            if (delivery == null) {
                return new DeliveryStatistics(-1, -1, -1, -1);
            }
            boolean diagnostics = monitor != null && monitor.diagnostics;
            return new DeliveryStatistics(
                    delivery.bytes,
                    diagnostics ? delivery.expired : -1,
                    diagnostics ? delivery.replaced : -1,
                    diagnostics ? delivery.bestEffortDrops : -1
            );
        }
    }

    @Override
    public Statistics statistics() {
        synchronized (queue) {
            if (monitor == null || !monitor.diagnostics) {
                return new Statistics(snapshotSize, maxSize, -1, -1, -1, 0);
            }
            long age = monitor.pressured ? (System.nanoTime() - monitor.pressureSince) / 1_000_000 : 0;
            return new Statistics(snapshotSize, maxSize, monitor.highWater, monitor.fullDrops, monitor.policyDrops, age);
        }
    }

    private void updatePressure() {
        boolean next = maxSize > 0 && snapshotSize * 100L >= maxSize * 80L;
        if (next != monitor.pressured) {
            monitor.pressured = next;
            if (monitor.diagnostics) {
                monitor.pressureSince = next ? System.nanoTime() : 0;
            }
            if (monitor.history != null) {
                monitor.history.record(monitor.id, monitor.transport, next ? OperationalHistory.Kind.PRESSURE_START
                        : OperationalHistory.Kind.PRESSURE_END, "outbound queue 80% threshold");
            }
        }
    }

    private static final class Monitor {

        int highWater;
        long fullDrops;
        long policyDrops;
        boolean pressured;
        long pressureSince;
        final boolean diagnostics;
        final OperationalHistory history;
        final long id;
        final String transport;

        Monitor(boolean diagnostics, OperationalHistory history, long id, String transport) {
            this.diagnostics = diagnostics;
            this.history = history;
            this.id = id;
            this.transport = transport;
        }
    }

    private static final class Delivery {

        final IdentityHashMap<Packet, Integer> sizes = new IdentityHashMap<>();
        final IdentityHashMap<Packet, Integer> references = new IdentityHashMap<>();
        long maximumBytes = 1024 * 1024;
        long bytes;
        long expired;
        long replaced;
        long bestEffortDrops;
    }

    @Override
    public String toString() {
        return "OutboundQueue{" +
                "outboundQueuePolicyType=" + (outboundQueuePolicy == null ? null : outboundQueuePolicy.getClass().getName()) +
                ", maxSize=" + maxSize +
                ", snapshotSize=" + snapshotSize +
                '}';
    }
}
