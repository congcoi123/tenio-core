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

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable queued-message replacement, expiry and overflow behavior.
 *
 * <p>Important messages preserve FIFO order and cannot expire or replace another message.
 * Congestion admission is controlled separately by the outbound queue policy. Every mode is
 * subject to packet/byte capacity and that policy, including the default UDP cutoff at 90 percent
 * occupancy. No mode provides end-to-end acknowledgement or durable storage. Responses with no
 * options retain basic FIFO behavior without allocating this record or enhanced bookkeeping.
 *
 * @param mode       queued-message replacement, expiry and overflow behavior
 * @param key        application replacement key, required only for REPLACEABLE
 * @param timeToLive maximum queued age, or zero for no deadline; at most one day
 */
public record DeliveryOptions(Mode mode, String key, Duration timeToLive) {

    private static final DeliveryOptions IMPORTANT = new DeliveryOptions(Mode.IMPORTANT, null, Duration.ZERO);

    /**
     * Validates the complete delivery configuration.
     *
     * @throws NullPointerException     if mode or timeToLive is null
     * @throws IllegalArgumentException if lifetime, replacement key or mode constraints are invalid
     */
    public DeliveryOptions {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(timeToLive);
        if (timeToLive.isNegative()
                || timeToLive.compareTo(Duration.ofDays(1)) > 0
                || (mode == Mode.IMPORTANT && !timeToLive.isZero())
                || (mode == Mode.REPLACEABLE && (key == null || key.isBlank() || key.length() > 128))
                || (mode != Mode.REPLACEABLE && key != null)) {
            throw new IllegalArgumentException("Invalid delivery mode, replacement key or lifetime");
        }
    }

    /**
     * Returns shared immutable settings for an important FIFO message without expiry.
     * Queue capacity and custom admission policies still apply.
     *
     * @return important delivery settings
     */
    public static DeliveryOptions important() {
        return IMPORTANT;
    }

    /**
     * Creates an update that may supersede an unclaimed update for the same key and
     * transport. Superseded content is not delivered; writer-claimed packets cannot be replaced.
     *
     * @param key      non-blank replacement key of at most 128 characters
     * @param lifetime maximum queued age; zero disables expiry, maximum one day
     * @return immutable replaceable delivery settings
     * @throws NullPointerException     if lifetime is null
     * @throws IllegalArgumentException if key or lifetime is invalid
     */
    public static DeliveryOptions replaceable(String key, Duration lifetime) {
        return new DeliveryOptions(Mode.REPLACEABLE, key, lifetime);
    }

    /**
     * Creates an expendable update with an optional queue lifetime. Capacity failure
     * returns a dropped admission result; outbound queue policy rejections remain explicit.
     *
     * @param lifetime maximum queued age; zero disables expiry, maximum one day
     * @return immutable best-effort delivery settings
     * @throws NullPointerException     if lifetime is null
     * @throws IllegalArgumentException if lifetime is negative or exceeds one day
     */
    public static DeliveryOptions bestEffort(Duration lifetime) {
        return new DeliveryOptions(Mode.BEST_EFFORT, null, lifetime);
    }

    /**
     * Queued-message behavior categories; all remain subject to the outbound queue policy.
     */
    public enum Mode {
        /**
         * Reject overflow explicitly, retain FIFO ordering, and never expire.
         */
        IMPORTANT,
        /**
         * Replace an unclaimed queued update with the same key and transport.
         */
        REPLACEABLE,
        /**
         * Drop this packet when bounded queue capacity admission fails.
         */
        BEST_EFFORT
    }
}
