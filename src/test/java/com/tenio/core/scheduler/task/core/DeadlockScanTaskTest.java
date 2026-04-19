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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tenio.core.event.implement.EventManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Unit Test Cases For DeadlockScanTask")
class DeadlockScanTaskTest {

  private EventManager eventManager;
  private DeadlockScanTask task;

  @BeforeEach
  void setUp() {
    eventManager = Mockito.mock(EventManager.class);
    task = DeadlockScanTask.newInstance(eventManager);
  }

  @Test
  @DisplayName("Test creating a new instance")
  void testNewInstance() {
    assertNotNull(DeadlockScanTask.newInstance(eventManager));
  }

  @Test
  @DisplayName("Test scheduler is null before run")
  void testGetSchedulerBeforeRunIsNull() {
    assertNull(task.getScheduler());
  }

  @Test
  @DisplayName("Test run initializes the scheduler")
  void testRunInitializesScheduler() {
    task.run();
    assertNotNull(task.getScheduler());
    task.shutdown();
  }

  @Test
  @DisplayName("Test shutdown after run completes without exception")
  void testShutdownAfterRun() {
    task.run();
    task.shutdown(); // should not throw
  }

  @Test
  @DisplayName("Test shutdown before run completes without exception")
  void testShutdownBeforeRun() {
    task.shutdown(); // scheduledService is null, should not throw
  }

  @Test
  @DisplayName("Test setInterval updates interval without exception")
  void testSetInterval() {
    task.setInterval(30);
    // no exception expected
  }

  @Test
  @DisplayName("Test checkForDeadlockedThreads via reflection does not throw when no deadlock")
  void testCheckForDeadlockedThreadsViaReflection() throws Exception {
    java.lang.reflect.Method method =
        DeadlockScanTask.class.getDeclaredMethod("checkForDeadlockedThreads");
    method.setAccessible(true);
    // In a normal test environment there are no deadlocks, so threadIds should be null/empty
    method.invoke(task);
  }
}
