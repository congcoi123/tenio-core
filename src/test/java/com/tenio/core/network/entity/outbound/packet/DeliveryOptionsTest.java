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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeliveryOptionsTest {

  @Test
  void factoriesChooseDocumentedCongestionPriorities() {
    assertEquals(DeliveryOptions.Priority.HIGH, DeliveryOptions.important().priority());
    assertEquals(DeliveryOptions.Priority.NORMAL,
        DeliveryOptions.replaceable("position", Duration.ZERO).priority());
    assertEquals(DeliveryOptions.Priority.LOW,
        DeliveryOptions.bestEffort(Duration.ZERO).priority());
  }

  @Test
  void selectingPriorityPreservesModeKeyAndLifetimeWithoutMutatingOriginal() {
    var original = DeliveryOptions.replaceable("position", Duration.ofSeconds(2));
    var changed = original.withPriority(DeliveryOptions.Priority.HIGH);
    assertEquals(DeliveryOptions.Priority.NORMAL, original.priority());
    assertEquals(DeliveryOptions.Priority.HIGH, changed.priority());
    assertEquals(original.mode(), changed.mode());
    assertEquals(original.key(), changed.key());
    assertEquals(original.timeToLive(), changed.timeToLive());
    assertSame(changed, changed.withPriority(DeliveryOptions.Priority.HIGH));
  }

  @Test
  void nullFieldsAreRejected() {
    assertThrows(NullPointerException.class,
        () -> new DeliveryOptions(null, DeliveryOptions.Priority.NORMAL, null, Duration.ZERO));
    assertThrows(NullPointerException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.IMPORTANT, null, null, Duration.ZERO));
    assertThrows(NullPointerException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.IMPORTANT, DeliveryOptions.Priority.HIGH,
            null, null));
    assertThrows(NullPointerException.class, () -> DeliveryOptions.important().withPriority(null));
  }

  @Test
  void priorityDoesNotRelaxExpiryOrReplacementValidation() {
    assertThrows(IllegalArgumentException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.IMPORTANT, DeliveryOptions.Priority.HIGH,
            null, Duration.ofMillis(1)));
    assertThrows(IllegalArgumentException.class,
        () -> new DeliveryOptions(DeliveryOptions.Mode.BEST_EFFORT, DeliveryOptions.Priority.HIGH,
            "not-a-replacement", Duration.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.replaceable(" ", Duration.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.replaceable("x".repeat(129), Duration.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.bestEffort(Duration.ofNanos(-1)));
    assertThrows(IllegalArgumentException.class,
        () -> DeliveryOptions.bestEffort(Duration.ofDays(1).plusNanos(1)));
    assertEquals(Duration.ofDays(1), DeliveryOptions.bestEffort(Duration.ofDays(1)).timeToLive());
  }
}
