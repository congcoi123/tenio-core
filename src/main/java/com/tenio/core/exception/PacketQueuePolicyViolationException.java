/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

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

package com.tenio.core.exception;

import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import java.io.Serial;

/**
 * Exception thrown when a packet queue policy is violated.
 * This exception occurs when a packet being sent from the server to clients violates
 * the configured packet queue policies, such as size limits or rate limits.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Packet queue size exceeds configured limits</li>
 *   <li>Packet rate exceeds configured thresholds</li>
 *   <li>Packet size exceeds maximum allowed size</li>
 *   <li>Queue policy configuration conflicts</li>
 * </ul>
 *
 * <p>Note: This exception provides information about the dropped packet and the
 * current queue usage percentage to help diagnose policy violation issues.
 *
 * @see PacketQueue
 * @see PacketQueuePolicy
 * @see Packet
 * @since 0.3.0
 */
public final class PacketQueuePolicyViolationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -1620230030870946508L;

  /**
   * Creates a new exception.
   *
   * @param packet         the dropping {@link Packet}
   * @param percentageUsed the current usage of queue in percent ({@code float} value)
   * @see PacketQueue
   * @see PacketQueuePolicy
   */
  public PacketQueuePolicyViolationException(Packet packet, float percentageUsed) {
    super(String.format("Dropped packet: [%s], current packet queue usage: %f%%", packet.toString(),
        percentageUsed));
  }
}
