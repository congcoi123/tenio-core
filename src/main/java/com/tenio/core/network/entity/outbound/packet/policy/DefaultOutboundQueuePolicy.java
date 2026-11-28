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
 * The default transport-based congestion admission policy. Incoming UDP packets are rejected
 * silently at 90 percent queue occupancy or on a hard capacity rejection. Other transports
 * remain eligible until the queue is full; their capacity rejections remain visible in logs.
 * Delivery mode does not override admission. This policy neither reorders queued packets nor
 * evicts existing packets. Applications can register a custom policy to change admission rules.
 *
 * @see OutboundQueuePolicy
 */
public class DefaultOutboundQueuePolicy implements OutboundQueuePolicy {

    private static final float NINETY_PERCENT_FULL = 90.0f;

    @Override
    public Decision applyPolicy(OutboundQueue outboundQueue, Packet packet, boolean capacityExceeded) {
        if ((capacityExceeded || outboundQueue.getPercentageUsed() >= NINETY_PERCENT_FULL) && packet.isUdp()) {
            return Decision.REJECT_SILENTLY;
        }
        return Decision.ACCEPT;
    }
}
