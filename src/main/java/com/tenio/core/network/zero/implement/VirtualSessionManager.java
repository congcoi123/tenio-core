/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.zero.implement;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A virtual thread-based session manager implementation.
 */
public final class VirtualSessionManager implements SessionManager {
  private final EventManager eventManager;
  private final Map<String, Session> sessions;
  private final ReentrantLock lock;
  private final AtomicBoolean isRunning;
  private Thread sessionMonitorThread;

  private VirtualSessionManager(EventManager eventManager) {
    this.eventManager = eventManager;
    this.sessions = new ConcurrentHashMap<>();
    this.lock = new ReentrantLock();
    this.isRunning = new AtomicBoolean(true);
  }

  /**
   * Creates a new instance of the virtual session manager.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link VirtualSessionManager}
   */
  public static VirtualSessionManager newInstance(EventManager eventManager) {
    return new VirtualSessionManager(eventManager);
  }

  @Override
  public void initialize() {
    // Start session monitor thread
    sessionMonitorThread = Thread.ofVirtual().start(() -> {
      while (isRunning.get()) {
        try {
          // Monitor sessions for inactivity
          sessions.forEach((id, session) -> {
            if (session.isExpired()) {
              removeSession(id);
            }
          });
          
          // Sleep for a short duration
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          eventManager.emit(ServerEvent.SERVER_EXCEPTION, e);
        }
      }
    });
  }

  @Override
  public void addSession(Session session) {
    lock.lock();
    try {
      sessions.put(session.getId(), session);
      eventManager.emit(ServerEvent.SESSION_ADDED, session);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void removeSession(String sessionId) {
    lock.lock();
    try {
      Session session = sessions.remove(sessionId);
      if (session != null) {
        eventManager.emit(ServerEvent.SESSION_REMOVED, session);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Session getSession(String sessionId) {
    return sessions.get(sessionId);
  }

  @Override
  public boolean containsSession(String sessionId) {
    return sessions.containsKey(sessionId);
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }

  @Override
  public void clear() {
    lock.lock();
    try {
      sessions.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    isRunning.set(false);
    if (sessionMonitorThread != null) {
      sessionMonitorThread.interrupt();
    }
    clear();
  }
} 