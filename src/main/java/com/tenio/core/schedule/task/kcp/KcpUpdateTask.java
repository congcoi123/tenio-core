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

package com.tenio.core.schedule.task.kcp;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.schedule.task.AbstractTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public final class KcpUpdateTask extends AbstractTask {

  private static final int DEFAULT_BYTE_BUFFER_SIZE = 10240;

  private SessionManager sessionManager;

  private KcpUpdateTask(EventManager eventManager) {
    super(eventManager);
  }

  public static KcpUpdateTask newInstance(EventManager eventManager) {
    return new KcpUpdateTask(eventManager);
  }

  @Override
  public ScheduledFuture<?> run() {
    return Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        () -> {
          var iterator = sessionManager.getSessionIterator();
          while (iterator.hasNext()) {
            var session = iterator.next();
            if (session.containsKcp()) {
              var ukcp = session.getUkcp();
              ukcp.update();
              byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];
              ukcp.receive(buffer);
            }
          }
        }, 0, 10, TimeUnit.MILLISECONDS);
  }

  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }
}
