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

package com.tenio.core.entity.define.mode;

/**
 * All modes associated with the player disconnected phase.
 */
public enum PlayerDisconnectMode {

  /**
   * When a player manually disconnect from the server.
   */
  DEFAULT,
  /**
   * When a player's connection is lost and the reason comes from the client side.
   */
  CONNECTION_LOST,
  /**
   * When a player falls in IDLE state for a long time enough to be disconnected.
   */
  IDLE,
  /**
   * When a player is removed from the server in purpose.
   */
  KICK,
  /**
   * When a player is removed from the server in purpose and its IP address is also listed in
   * the black list.
   */
  BAN,
  /**
   * Disconnected by other reasons.
   */
  UNKNOWN,
  /**
   * Actually, the player will not get disconnected, it only changes its session
   *
   * @since 0.5.0
   */
  RECONNECTION;

  @Override
  public String toString() {
    return this.name();
  }
}
