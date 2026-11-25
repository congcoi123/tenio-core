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

package com.tenio.core.network.entity.outbound.packet.policy;

import com.tenio.core.exception.OutboundQueueFullException;
import com.tenio.core.exception.OutboundQueuePolicyViolationException;
import com.tenio.core.network.entity.outbound.packet.DeliveryOptions;
import com.tenio.core.network.entity.outbound.packet.Packet;
import com.tenio.core.network.entity.outbound.packet.OutboundQueue;

/**
 * The default congestion admission policy. Missing delivery settings use NORMAL priority.
 * Priority never reorders packets or bypasses a full queue.
 *
 * @see OutboundQueuePolicy
 */
public class DefaultOutboundQueuePolicy implements OutboundQueuePolicy {

  private static final float THREE_QUARTERS_FULL = 75.0f;
  private static final float NINETY_PERCENT_FULL = 90.0f;

  @Override
  public void applyPolicy(OutboundQueue outboundQueue, Packet packet) {
    if (outboundQueue.isSnapshotFull()) {
      throw new OutboundQueueFullException(outboundQueue.getSnapshotSize());
    }

    float percentageUsed = outboundQueue.getPercentageUsed();
    if (percentageUsed < THREE_QUARTERS_FULL) {
      return;
    }
    var options = packet.getDeliveryOptions();
    var priority = options == null ? DeliveryOptions.Priority.NORMAL : options.priority();
    if ((priority == DeliveryOptions.Priority.LOW && percentageUsed >= THREE_QUARTERS_FULL)
        || (priority == DeliveryOptions.Priority.NORMAL && percentageUsed >= NINETY_PERCENT_FULL)) {
      throw new OutboundQueuePolicyViolationException(packet, percentageUsed);
    }
  }
}
