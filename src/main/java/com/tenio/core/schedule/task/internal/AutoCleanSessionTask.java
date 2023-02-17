/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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

package com.tenio.core.schedule.task.internal;

import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.schedule.task.AbstractSystemTask;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * For a session which is in IDLE mode and no longer associated to any player, that means for a
 * long time without receiving or sending any data from the server or from a client. This task
 * will scan those IDLE sessions in period time and force them to disconnect. Those
 * sessions got a "timeout" error.
 *
 * @since 0.5.0
 */
public final class AutoCleanSessionTask extends AbstractSystemTask {

  private SessionManager sessionManager;

  private AutoCleanSessionTask(EventManager eventManager) {
    super(eventManager);
  }

  public static AutoCleanSessionTask newInstance(EventManager eventManager) {
    return new AutoCleanSessionTask(eventManager);
  }

  @Override
  public ScheduledFuture<?> run() {
    return Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        () -> {
          Iterator<Session> iterator = sessionManager.getSessionIterator();
          while (iterator.hasNext()) {
            Session session = iterator.next();
            if (session.isOrphan() || session.isIdle()) {
              try {
                session.close(ConnectionDisconnectMode.ORPHAN, PlayerDisconnectMode.DEFAULT);
              } catch (IOException exception) {
                error(exception, session.toString());
              }
            }
          }
        }, initialDelay, interval, TimeUnit.SECONDS);
  }

  /**
   * Set the session manager.
   *
   * @param sessionManager the session manager
   */
  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }
}
