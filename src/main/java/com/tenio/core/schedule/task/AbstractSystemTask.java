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

package com.tenio.core.schedule.task;

import com.tenio.common.logger.SystemLogger;
import com.tenio.common.task.Task;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.manager.AbstractManager;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * The abstract class for system tasks.
 */
public abstract class AbstractSystemTask extends AbstractManager implements Task {

  private static final int DEFAULT_INITIAL_DELAY_IN_SECONDS = 60;
  private static final int DEFAULT_INTERVAL_IN_SECONDS = 60;

  /**
   * The event manager.
   */
  protected EventManager eventManager;
  /**
   * The interval value.
   */
  protected int interval;
  /**
   * The initial delay time. The task should wait a little until the system becomes stable.
   */
  protected int initialDelay;

  /**
   * Initialization.
   *
   * @param eventManager the event manager
   */
  protected AbstractSystemTask(EventManager eventManager) {
    super(eventManager);
    this.eventManager = eventManager;
    interval = DEFAULT_INTERVAL_IN_SECONDS;
    initialDelay = DEFAULT_INITIAL_DELAY_IN_SECONDS;
  }

  /**
   * Runs the task with the provided executor service.
   *
   * @param executorService the {@link ScheduledExecutorService} to use for scheduling
   * @return a {@link ScheduledFuture} representing pending completion of the task
   */
  public abstract ScheduledFuture<?> run(ScheduledExecutorService executorService);

  /**
   * Sets the interval for the task.
   *
   * @param interval the interval in seconds
   */
  public void setInterval(int interval) {
    this.interval = interval;
  }

  /**
   * Sets the initial delay for the task.
   *
   * @param initialDelay the initial delay in seconds
   */
  public void setInitialDelay(int initialDelay) {
    this.initialDelay = initialDelay;
  }
}
