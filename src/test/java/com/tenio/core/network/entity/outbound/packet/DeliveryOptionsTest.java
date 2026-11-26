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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeliveryOptionsTest {

  @Test
  void factoriesDescribeQueueBehaviorWithoutSelectingAdmissionRules() {
    var important = DeliveryOptions.important();
    assertEquals(DeliveryOptions.Mode.IMPORTANT, important.mode());
    assertNull(important.key());
    assertEquals(Duration.ZERO, important.timeToLive());
    assertSame(important, DeliveryOptions.important());

    var replaceable = DeliveryOptions.replaceable("position", Duration.ofMillis(250));
    assertEquals(DeliveryOptions.Mode.REPLACEABLE, replaceable.mode());
    assertEquals("position", replaceable.key());
    assertEquals(Duration.ofMillis(250), replaceable.timeToLive());

    var bestEffort = DeliveryOptions.bestEffort(Duration.ofSeconds(2));
    assertEquals(DeliveryOptions.Mode.BEST_EFFORT, bestEffort.mode());
    assertNull(bestEffort.key());
    assertEquals(Duration.ofSeconds(2), bestEffort.timeToLive());
  }

  @Test
  void nullModeAndLifetimeAreRejected() {
    assertThrows(NullPointerException.class,
        () -> new DeliveryOptions(null, null, Duration.ZERO));
    assertThrows(NullPointerException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.IMPORTANT, null, null));
    assertThrows(NullPointerException.class,
        () -> DeliveryOptions.replaceable("position", null));
    assertThrows(NullPointerException.class,
        () -> DeliveryOptions.bestEffort(null));
  }

  @Test
  void modesKeepTheirExpiryAndReplacementConstraints() {
    assertThrows(IllegalArgumentException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.IMPORTANT, null, Duration.ofNanos(1)));
    assertThrows(IllegalArgumentException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.IMPORTANT, "key", Duration.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.BEST_EFFORT, "key", Duration.ZERO));
    for (String key : new String[] {null, "", " ", "x".repeat(129)}) {
      assertThrows(IllegalArgumentException.class,
          () -> DeliveryOptions.replaceable(key, Duration.ZERO));
    }
    assertEquals("x".repeat(128), DeliveryOptions.replaceable("x".repeat(128), Duration.ZERO).key());
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.bestEffort(Duration.ofNanos(-1)));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.replaceable("position", Duration.ofNanos(-1)));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.bestEffort(Duration.ofDays(1).plusNanos(1)));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.replaceable("position", Duration.ofDays(1).plusNanos(1)));
    assertEquals(Duration.ofDays(1), DeliveryOptions.bestEffort(Duration.ofDays(1)).timeToLive());
    assertEquals(Duration.ofDays(1),
        DeliveryOptions.replaceable("position", Duration.ofDays(1)).timeToLive());
  }
}
