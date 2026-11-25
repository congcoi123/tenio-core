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
 * Optional pre-write delivery policy. It cannot change data already claimed by the TCP writer.
 * Important messages preserve FIFO order and cannot expire or replace another message.
 *
 * @param mode admission behavior
 * @param key application replacement key, required only for REPLACEABLE
 * @param timeToLive maximum queued age, or zero for no deadline; at most one day
 */
public record DeliveryOptions(Mode mode, String key, Duration timeToLive) {
  /** Queue admission categories; none provides end-to-end delivery acknowledgement. */
  public enum Mode {
    /** Reject overflow explicitly, retain FIFO ordering, and never expire. */
    IMPORTANT,
    /** Replace an unclaimed queued update with the same key and transport. */
    REPLACEABLE,
    /** Drop this packet when bounded queue admission fails. */
    BEST_EFFORT
  }

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
   * @return an important FIFO message without expiry
   */
  public static DeliveryOptions important() {
    return new DeliveryOptions(Mode.IMPORTANT, null, Duration.ZERO);
  }

  /** Creates an update that may supersede a pending update for the same application key. */
  public static DeliveryOptions replaceable(String key, Duration lifetime) {
    return new DeliveryOptions(Mode.REPLACEABLE, key, lifetime);
  }

  /** Creates an expendable update with an optional queue lifetime. */
  public static DeliveryOptions bestEffort(Duration lifetime) {
    return new DeliveryOptions(Mode.BEST_EFFORT, null, lifetime);
  }
}
