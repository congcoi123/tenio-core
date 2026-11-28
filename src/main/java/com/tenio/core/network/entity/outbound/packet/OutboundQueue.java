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

package com.tenio.core.network.entity.outbound.packet;

import com.tenio.core.admin.OperationalHistory;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy;

/**
 * Represents a thread-safe queue for managing outbound packets on the server.
 * This interface defines the core functionality for packet queuing, including
 * packet management, queue size control, and policy enforcement.
 *
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe packet queuing</li>
 *   <li>Queue size management and monitoring</li>
 *   <li>Packet policy enforcement</li>
 *   <li>Queue usage tracking</li>
 *   <li>FIFO packet processing</li>
 * </ul>
 *
 * <p>Thread safety: This interface is designed to be thread-safe and can be
 * safely accessed from multiple threads. Implementations should ensure atomic
 * operations for queue modifications.
 *
 * @see Packet
 * @see OutboundQueuePolicy
 * @see DefaultOutboundQueuePolicy
 * @since 0.3.0
 */
public interface OutboundQueue {

    /**
     * Retrieves the last-in packet in the queue.
     *
     * @return the last-in {@link Packet} in the queue
     */
    Packet peek();

    /**
     * Retrieves the last-in packet in the queue and removes it as well.
     *
     * @return the last-in {@link Packet} in the queue
     */
    Packet take();

    /**
     * Determines whether the queue is empty (approximately correct).
     *
     * @return {@code true} if the queue is approximated to be empty, otherwise returns {@code false}
     * @since 0.7.3
     */
    boolean isSnapshotEmpty();

    /**
     * Determines whether the queue is empty.
     * This method is costly, use with caution.
     *
     * @return {@code true} if the queue is empty, otherwise returns {@code false}
     * @since 0.7.3
     */
    boolean isEmpty();

    /**
     * Determines whether the queue is full (approximately correct).
     *
     * @return {@code true} if the queue is approximated to be full, otherwise returns {@code false}
     * @since 0.7.3
     */
    boolean isSnapshotFull();

    /**
     * Determines whether the queue is full.
     * This method is costly, use with caution.
     *
     * @return {@code true} if the queue is full, otherwise returns {@code false}
     * @since 0.7.3
     */
    boolean isFull();

    /**
     * Retrieves the most recent update of the current size of queue.
     *
     * @return the most recent update of the current {@code integer} size of queue
     * @since 0.7.3
     */
    int getSnapshotSize();

    /**
     * Retrieves the current size of queue.
     * This method is costly, use with caution.
     *
     * @return the current {@code integer} size of queue
     * @since 0.7.3
     */
    int getSize();

    /**
     * Sets the maximum allowed size of queue.
     *
     * @param maxSize the maximum {@code integer} allowed size of queue
     */
    void configureMaxSize(int maxSize);

    /**
     * Enables queue diagnostics and optionally records queue pressure events.
     * Call this method before making the session available to other threads.
     *
     * @param history   the operational history recorder, or {@code null} to skip history recording
     * @param id        the session ID associated with recorded events
     * @param transport the transport name associated with recorded events
     * @see #enableMonitoring(boolean, OperationalHistory, long, String)
     */
    default void enableMonitoring(OperationalHistory history, long id, String transport) {
        enableMonitoring(true, history, id, transport);
    }

    /**
     * Configures queue diagnostics and pressure event recording independently.
     * Call this method before making the queue available to other threads.
     *
     * @param diagnostics whether to track the high-water mark, packet drops, and pressure duration
     * @param history     the operational history recorder, or {@code null} to skip history recording
     * @param id          the session ID associated with recorded events
     * @param transport   the transport name associated with recorded events
     */
    void enableMonitoring(boolean diagnostics, OperationalHistory history, long id, String transport);

    /**
     * Sets a set of rules for managing packets in queue.
     *
     * @param outboundQueuePolicy the implementation of {@link OutboundQueuePolicy} used to manage
     *                            packets in the queue
     * @see DefaultOutboundQueuePolicy
     */
    void configureOutboundQueuePolicy(OutboundQueuePolicy outboundQueuePolicy);

    /**
     * Retrieves the current usage of queue.
     *
     * @return the current {@code float} usage of queue. Its range is between 0 and 100 percent
     */
    float getPercentageUsed();

    /**
     * Admits a packet subject to the configured policy and hard packet/byte bounds. Expected
     * capacity and policy rejections return enum results instead of throwing. The writer uses
     * the result to count drops, log visible rejections and schedule admitted packets.
     *
     * @param packet the packet proposed for admission
     * @return the admission outcome, never {@code null}
     * @throws RuntimeException if an unexpected admission failure occurs
     * @see DefaultOutboundQueuePolicy
     */
    Admission put(Packet packet);

    /**
     * Clears all packets from the queue.
     */
    void clear();

    /**
     * Returns a snapshot of outbound queue statistics.
     *
     * <p>The default implementation uses {@link #getSnapshotSize()} for depth, returns {@code -1}
     * for capacity, high-water mark, and drop counters, and returns {@code 0} for pressure age.
     *
     * @return the outbound queue statistics snapshot
     */
    default Statistics statistics() {
        return new Statistics(getSnapshotSize(), -1, -1, -1, -1, 0);
    }

    /**
     * Enables a serialized-payload byte bound in addition to the existing packet bound. Configure
     * before publishing the queue; zero disables the byte bound.
     *
     * @param maximumBytes positive bound or zero
     */
    void configureDeliveryByteLimit(long maximumBytes);

    /**
     * @return enhanced delivery diagnostics, or unavailable values when disabled
     */
    default DeliveryStatistics deliveryStatistics() {
        return new DeliveryStatistics(-1, -1, -1, -1);
    }

    /**
     * Delegates to {@link #put(Packet)}, preserving its admission result. Delivery metadata is
     * handled by the queue's normal admission path; callers can use {@code put} for all packets.
     *
     * @param packet the packet proposed for admission
     * @return the admission outcome from {@code put}, never {@code null}
     */
    default Admission putWithDelivery(Packet packet) {
        return put(packet);
    }

    /**
     * Outcome of basic or enhanced queue admission. Silent outcomes suppress rejection warnings
     * while retaining the same capacity or policy drop counters.
     */
    enum Admission {
        /**
         * Queued successfully.
         */
        ENQUEUED,
        /**
         * Deadline elapsed before admission.
         */
        EXPIRED,
        /**
         * Packet rejected at a hard packet or byte bound; allow a rejection warning.
         */
        DROPPED,
        /**
         * Packet rejected by policy within hard bounds; allow a rejection warning.
         */
        POLICY_REJECTED,
        /**
         * Packet rejected at a hard packet or byte bound without a rejection warning.
         */
        SILENTLY_DROPPED,
        /**
         * Packet rejected by policy within hard bounds without a rejection warning.
         */
        SILENTLY_POLICY_REJECTED;

        /**
         * Determines whether this result counts as a capacity drop.
         *
         * @return {@code true} for either visible or silent capacity rejection
         */
        public boolean isCapacityRejection() {
            return this == DROPPED || this == SILENTLY_DROPPED;
        }

        /**
         * Determines whether this result counts as a policy drop.
         *
         * @return {@code true} for either visible or silent policy rejection
         */
        public boolean isPolicyRejection() {
            return this == POLICY_REJECTED || this == SILENTLY_POLICY_REJECTED;
        }

        /**
         * Determines whether the policy suppressed rejection warnings for this result.
         *
         * @return {@code true} when a rejection must be counted without a warning
         */
        public boolean isSilent() {
            return this == SILENTLY_DROPPED || this == SILENTLY_POLICY_REJECTED;
        }
    }

    /**
     * A snapshot of outbound queue occupancy, packet drops, and pressure.
     *
     * @param depth             the last recorded number of packets in the queue
     * @param capacity          the maximum number of packets the queue can hold, or {@code -1} if unavailable
     * @param highWater         the highest recorded queue depth since diagnostics began, or {@code -1}
     *                          if unavailable
     * @param fullDrops         the number of packets rejected because the queue was full, or {@code -1}
     *                          if unavailable
     * @param policyDrops       the number of packets rejected for violating the outbound queue policy,
     *                          or {@code -1} if unavailable
     * @param pressureAgeMillis the elapsed time in milliseconds since the current period of queue
     *                          pressure began, or {@code 0} if the queue is below the pressure threshold or pressure
     *                          tracking is unavailable
     */
    record Statistics(int depth, int capacity, int highWater, long fullDrops, long policyDrops, long pressureAgeMillis) {
    }

    /**
     * Queued bytes and additional policy outcomes; counters are -1 without monitor diagnostics.
     */
    record DeliveryStatistics(long queuedBytes, long expired, long replaced, long bestEffortDrops) {
    }
}
