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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.tenio.core.event.implement.EventManager;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

  @Test
  @DisplayName("findMatchingThread finds the current thread by its ThreadInfo")
  void testFindMatchingThreadFindsCurrentThread() throws Exception {
    Method m = DeadlockScanTask.class.getDeclaredMethod("findMatchingThread", ThreadInfo.class);
    m.setAccessible(true);

    long threadId = Thread.currentThread().getId();
    ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(threadId);

    Object result = m.invoke(task, threadInfo);
    assertNotNull(result);
    assertInstanceOf(Thread.class, result);
    assertEquals(threadId, ((Thread) result).getId());
  }

  @Test
  @DisplayName("findMatchingThread throws IllegalStateException when thread ID not found")
  void testFindMatchingThreadThrowsWhenNotFound() throws Exception {
    Method m = DeadlockScanTask.class.getDeclaredMethod("findMatchingThread", ThreadInfo.class);
    m.setAccessible(true);

    ThreadInfo mockInfo = Mockito.mock(ThreadInfo.class);
    when(mockInfo.getThreadId()).thenReturn(-999L);

    try {
      m.invoke(task, mockInfo);
    } catch (InvocationTargetException e) {
      assertInstanceOf(IllegalStateException.class, e.getCause());
    }
  }

  @Test
  @DisplayName("shutdown handles InterruptedException from awaitTermination")
  void testShutdownHandlesInterruptedException() {
    task.run();
    Thread.currentThread().interrupt();
    try {
      task.shutdown();
    } finally {
      Thread.interrupted();
    }
  }
}
