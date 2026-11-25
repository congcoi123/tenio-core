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

package com.tenio.core.command.client;

import com.tenio.common.data.DataCollection;
import java.util.Objects;

/**
 * Optional application bean resolving commands from decoded TCP, UDP and WebSocket messages.
 * Register one implementation in the bootstrap scan path. There are no reserved field names.
 * Routing is enabled at startup only when this bean and registered client commands both exist.
 * Registration and policy changes require a server restart.
 */
@FunctionalInterface
public interface ClientCommandPolicy {

  /**
   * A resolved command and its application payload.
   *
   * @param code registered command code
   * @param content non-null payload passed to the command handler
   */
  record CommandRequest(short code, DataCollection content) {
    /** Validates the resolved payload. */
    public CommandRequest {
      Objects.requireNonNull(content, "content");
    }
  }

  /**
   * Resolves one associated player's message after transport envelope processing.
   *
   * <p>Return {@code null} or throw an exception to reject missing or malformed command intent.
   * Unknown resolved codes are also rejected. Command mode never emits
   * RECEIVED_MESSAGE_FROM_PLAYER, including on rejection. Resolved commands go through the
   * manager's rate limits and execution mode. Without a policy or registered commands at startup,
   * event mode remains active instead. This method may run concurrently across sessions;
   * implementations must be thread-safe and should not mutate data.
   *
   * @param message decoded application message, with the UDP envelope already removed
   * @return resolved command, or {@code null} to reject the message
   */
  CommandRequest applyPolicy(DataCollection message);
}
