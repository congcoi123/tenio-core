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

import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.schedule.task.AbstractSystemTask;
import java.util.concurrent.ScheduledFuture;

/**
 * For a session which is in IDLE mode and no longer associated to any player, that means for a
 * long time without receiving or sending any data from the server or from a client. This task
 * will scan those IDLE sessions in period time and force them to disconnect. Those
 * sessions got a "timeout" error.
 */
public final class AutoCleanOrphanSessionTask extends AbstractSystemTask {

  private AutoCleanOrphanSessionTask(EventManager eventManager) {
    super(eventManager);
  }

  public static AutoCleanOrphanSessionTask newInstance(EventManager eventManager) {
    return new AutoCleanOrphanSessionTask(eventManager);
  }

  @Override
  public ScheduledFuture<?> run() {
    return null;
  }

  /**
   * Set the session manager.
   *
   * @param sessionManager the session manager
   */
  public void setSessionManager(SessionManager sessionManager) {
  }
}