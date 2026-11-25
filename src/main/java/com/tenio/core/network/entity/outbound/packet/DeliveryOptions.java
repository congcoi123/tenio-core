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
 * Immutable pre-write admission, replacement and expiry settings.
 *
 * <p>Priority controls congestion admission, not send order or transport reliability. Important
 * messages preserve FIFO order and cannot expire or replace another message. All modes remain
 * subject to packet/byte capacity and the configured outbound queue policy. Responses with no
 * options use NORMAL priority without allocating this record or enhanced queue bookkeeping.
 *
 * @param mode admission and replacement behavior
 * @param priority congestion admission priority; never changes FIFO order
 * @param key application replacement key, required only for REPLACEABLE
 * @param timeToLive maximum queued age, or zero for no deadline; at most one day
 */
public record DeliveryOptions(Mode mode, Priority priority, String key, Duration timeToLive) {

  /** Queue admission categories; none provides end-to-end delivery acknowledgement. */
  public enum Mode {
    /** Reject overflow explicitly, retain FIFO ordering, and never expire. */
    IMPORTANT,
    /** Replace an unclaimed queued update with the same key and transport. */
    REPLACEABLE,
    /** Drop this packet when bounded queue capacity admission fails. */
    BEST_EFFORT
  }

  /**
   * Congestion admission levels used by DefaultOutboundQueuePolicy. Custom policies may define
   * their own admission rules; these levels never bypass hard queue bounds or reorder packets.
   */
  public enum Priority {
    /** The default policy rejects admission at 75 percent packet occupancy. */
    LOW,
    /** The default policy rejects admission at 90 percent packet occupancy. */
    NORMAL,
    /** The default policy admits until the queue is full. */
    HIGH
  }

  private static final DeliveryOptions IMPORTANT =
      new DeliveryOptions(Mode.IMPORTANT, Priority.HIGH, null, Duration.ZERO);

  /**
   * Validates the complete delivery configuration.
   *
   * @throws NullPointerException if mode, priority or timeToLive is null
   * @throws IllegalArgumentException if lifetime, replacement key or mode constraints are invalid
   */
  public DeliveryOptions {
    Objects.requireNonNull(mode);
    Objects.requireNonNull(priority);
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
   * Returns shared immutable settings for an important HIGH-priority FIFO message without expiry.
   * Queue capacity and custom admission policies still apply.
   *
   * @return important delivery settings
   */
  public static DeliveryOptions important() {
    return IMPORTANT;
  }

  /**
   * Creates a NORMAL-priority update that may supersede an unclaimed update for the same key and
   * transport. Superseded content is not delivered; writer-claimed packets cannot be replaced.
   *
   * @param key non-blank replacement key of at most 128 characters
   * @param lifetime maximum queued age; zero disables expiry, maximum one day
   * @return immutable replaceable delivery settings
   * @throws NullPointerException if lifetime is null
   * @throws IllegalArgumentException if key or lifetime is invalid
   */
  public static DeliveryOptions replaceable(String key, Duration lifetime) {
    return new DeliveryOptions(Mode.REPLACEABLE, Priority.NORMAL, key, lifetime);
  }

  /**
   * Creates a LOW-priority expendable update with an optional queue lifetime. Capacity failure
   * returns a dropped admission result; outbound queue policy rejections remain explicit.
   *
   * @param lifetime maximum queued age; zero disables expiry, maximum one day
   * @return immutable best-effort delivery settings
   * @throws NullPointerException if lifetime is null
   * @throws IllegalArgumentException if lifetime is negative or exceeds one day
   */
  public static DeliveryOptions bestEffort(Duration lifetime) {
    return new DeliveryOptions(Mode.BEST_EFFORT, Priority.LOW, null, lifetime);
  }

  /**
   * Selects a congestion priority without changing mode, replacement key or lifetime. A custom
   * policy still has the final admission decision. Lowering an important message's priority can
   * make it subject to default congestion rejection before the queue is full.
   *
   * @param value non-null congestion admission priority
   * @return this instance if unchanged, otherwise new immutable settings
   * @throws NullPointerException if value is null
   */
  public DeliveryOptions withPriority(Priority value) {
    Objects.requireNonNull(value);
    return priority == value ? this : new DeliveryOptions(mode, value, key, timeToLive);
  }
}
