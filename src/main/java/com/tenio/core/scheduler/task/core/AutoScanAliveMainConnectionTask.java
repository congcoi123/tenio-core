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

package com.tenio.core.scheduler.task.core;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.scheduler.task.AbstractTask;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors keepalive activity on authenticated main connections, currently primary TCP sessions.
 * Closes sessions that stop receiving client bytes; this is not a heartbeat-packet-only timer.
 * All inbound TCP traffic counts, including application heartbeats. Outbound snapshots,
 * UDP traffic, auxiliary WebSocket traffic and gameplay inactivity do not renew this deadline.
 * The normal session-close pipeline emits connection loss and detaches the connection;
 * this task never removes the logical player or decides room and reconnect-grace policy.
 *
 * <p>Configure the player manager, receive timeout and scan interval before calling {@link #run()}.
 * Scans are serial and use the session timestamps' wall-clock basis. Each scan starts after
 * the configured interval has elapsed since the previous scan completed; the first scan waits
 * one interval too. Failures are isolated per player. A non-positive scan interval disables
 * scheduling. A zero timeout makes scheduled scans skip expiry checks without accessing
 * the player registry. Shutdown is terminal and idempotent; repeated run calls do not create
 * workers.
 */
public final class AutoScanAliveMainConnectionTask extends AbstractTask {

  private PlayerManager playerManager;
  private volatile long timeoutMillis;
  private ScheduledExecutorService executor;
  private ScheduledFuture<?> scheduler;
  private boolean stopped;

  /**
   * Creates an optional scan task with scheduling disabled until an interval is configured.
   */
  public AutoScanAliveMainConnectionTask() {
    interval = -1;
  }

  /**
   * Configures the scan delay before scheduling begins.
   * A positive interval starts the task; a zero receive timeout makes scans skip expiry checks.
   * Set {@code 0} to disable; other non-positive values also disable the scan task.
   *
   * @param interval delay in seconds between scans and before the first scan
   */
  @Override
  public void setInterval(int interval) {
    super.setInterval(interval);
  }

  /**
   * Supplies the owner of authenticated player sessions before scheduling begins.
   *
   * @param playerManager instance-local player registry
   */
  public void setPlayerManager(PlayerManager playerManager) {
    this.playerManager = playerManager;
  }

  /**
   * Sets the maximum time since reliable inbound bytes were received.
   *
   * @param timeoutSeconds receive deadline in seconds; zero disables expiry
   * @throws IllegalArgumentException if the timeout is negative
   */
  public void setTimeoutSeconds(int timeoutSeconds) {
    if (timeoutSeconds < 0) {
      throw new IllegalArgumentException("main-connection-keepalive-timeout-seconds must not be negative");
    }
    timeoutMillis = TimeUnit.SECONDS.toMillis(timeoutSeconds);
  }

  @Override
  public synchronized void run() {
    if (interval <= 0 || stopped || executor != null) {
      return;
    }
    if (playerManager == null) {
      throw new IllegalStateException("A player manager is required for main-connection keepalive monitoring");
    }
    executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "task-scan-main-alive-connection");
      thread.setDaemon(true);
      return thread;
    });
    scheduler = executor.scheduleWithFixedDelay(() -> {
      try {
        scan(System.currentTimeMillis());
      } catch (RuntimeException exception) {
        // A registry failure must not cancel subsequent executions of the watchdog.
        if (isErrorEnabled()) {
          error(exception);
        }
      }
    }, interval, interval, TimeUnit.SECONDS);
  }

  /**
   * Scans current primary TCP sessions without removing retained logical players.
   * Replaced, inactive and non-TCP sessions are skipped; one failed close cannot prevent
   * another player from being checked. A future read timestamp is treated as recent activity.
   * A non-positive scan interval or 0 timeout makes this method a no-op.
   *
   * @param currentTimeMillis current wall-clock time in milliseconds
   */
  void scan(long currentTimeMillis) {
    long receiveTimeoutMillis = timeoutMillis;
    if (receiveTimeoutMillis == 0 || interval <= 0) {
      return;
    }
    for (Player player : playerManager.getSnapshotPlayersList()) {
      try {
        Session session = player.getSession().orElse(null);
        if (session != null && session.isTcp() && session.isActivated()
            && player.getSession().orElse(null) == session
            && currentTimeMillis - session.getLastReliableReadTime() >= receiveTimeoutMillis) {
          session.close(ConnectionDisconnectMode.IDLE, PlayerDisconnectMode.CONNECTION_LOST);
        }
      } catch (IOException | RuntimeException exception) {
        if (isErrorEnabled()) {
          error(exception);
        }
      }
    }
  }

  @Override
  public synchronized ScheduledFuture<?> getScheduler() {
    return scheduler;
  }

  @Override
  public synchronized void shutdown() {
    stopped = true;
    if (scheduler != null) {
      scheduler.cancel(false);
    }
    if (executor != null) {
      executor.shutdownNow();
    }
  }
}
