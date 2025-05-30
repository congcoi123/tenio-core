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

package com.tenio.core.network.zero.handler;

import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;

/**
 * Base interface for I/O handlers that manage network communication.
 * This interface defines the core functionality for handling network events,
 * session management, and network statistics tracking.
 *
 * <p>Key features:
 * <ul>
 *   <li>Session exception handling</li>
 *   <li>Session manager integration</li>
 *   <li>Network statistics tracking</li>
 *   <li>Event-based communication</li>
 * </ul>
 *
 * <p>Note: This interface is typically implemented by specific I/O handlers
 * for different network protocols (TCP, WebSocket, UDP, etc.).
 *
 * @see Session
 * @see SessionManager
 * @see NetworkReaderStatistic
 * @since 0.3.0
 */
public interface BaseIoHandler {

  /**
   * When any exception occurred on a session then this method is invoked.
   *
   * @param session   the {@link Session} using to communicate to client side
   * @param exception an {@link Exception} emerging
   */
  void sessionException(Session session, Exception exception);

  /**
   * Set a session manager.
   *
   * @param sessionManager the {@link SessionManager}
   */
  void setSessionManager(SessionManager sessionManager);

  /**
   * Sets a network reader statistic instance which takes responsibility recording the
   * receiving data from clients.
   *
   * @param networkReaderStatistic a {@link NetworkReaderStatistic} instance
   */
  void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic);
}
