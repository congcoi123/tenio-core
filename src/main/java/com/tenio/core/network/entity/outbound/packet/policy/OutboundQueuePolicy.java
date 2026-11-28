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

import com.tenio.core.network.entity.outbound.packet.OutboundQueue;
import com.tenio.core.network.entity.outbound.packet.Packet;

/**
 * The outbound queue policy.
 *
 * @see OutboundQueue
 */
public interface OutboundQueuePolicy {

    /**
     * Evaluates admission before the queue inserts the packet. Hard packet and byte bounds
     * cannot be bypassed by accepting a packet that exceeds capacity. Implementations must not
     * mutate the queue or packet during this call.
     *
     * @param outboundQueue    the queue being checked; its depth excludes an eligible replacement
     *                         during policy evaluation
     * @param packet           the packet proposed for admission
     * @param capacityExceeded whether admitting the packet would exceed a hard packet or byte bound,
     *                         after accounting for an eligible replacement
     * @return the admission decision, never {@code null}; expected rejections must return a code
     * @throws RuntimeException if an unexpected policy failure occurs
     */
    Decision applyPolicy(OutboundQueue outboundQueue, Packet packet, boolean capacityExceeded);

    /**
     * Policy admission decisions. The queue determines the capacity or policy rejection reason;
     * the policy determines whether the writer should log the rejection.
     */
    enum Decision {
        /**
         * Accept subject to the queue's hard capacity bounds.
         */
        ACCEPT,
        /**
         * Reject and allow the writer to log a rate-limited warning.
         */
        REJECT,
        /**
         * Reject without rejection warnings; drop counters are still updated.
         */
        REJECT_SILENTLY
    }
}
