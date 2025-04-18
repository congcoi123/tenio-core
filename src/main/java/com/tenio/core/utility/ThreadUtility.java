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

package com.tenio.core.utility;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Utility class for managing virtual threads.
 */
public final class ThreadUtility {

  private static final boolean ENABLE_VIRTUAL_THREADS = Boolean.parseBoolean(
      System.getProperty("tenio.thread.virtual.enabled", "false"));
  
  private static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors();

  private ThreadUtility() {
    throw new UnsupportedOperationException("This class cannot be instantiated");
  }

  /**
   * Creates a new virtual thread factory with the given name format.
   *
   * @param nameFormat the name format for the threads
   * @return a new {@link ThreadFactory}
   */
  public static ThreadFactory newVirtualThreadFactory(String nameFormat) {
    if (ENABLE_VIRTUAL_THREADS) {
      return Thread.ofVirtual().name(nameFormat).factory();
    } else {
      return new ThreadFactoryBuilder().setDaemon(true).setNameFormat(nameFormat).build();
    }
  }

  /**
   * Creates a new virtual thread executor service.
   *
   * @param nameFormat the name format for the threads
   * @return a new {@link ExecutorService}
   */
  public static ExecutorService newVirtualThreadExecutor(String nameFormat) {
    if (ENABLE_VIRTUAL_THREADS) {
      return Executors.newVirtualThreadPerTaskExecutor();
    } else {
      return Executors.newCachedThreadPool(newVirtualThreadFactory(nameFormat));
    }
  }

  /**
   * Creates a new virtual thread scheduled executor service.
   *
   * @param nameFormat the name format for the threads
   * @return a new {@link ScheduledExecutorService}
   */
  public static ScheduledExecutorService newVirtualThreadScheduledExecutor(String nameFormat) {
    if (ENABLE_VIRTUAL_THREADS) {
      return Executors.newScheduledThreadPool(1, Thread.ofVirtual().name(nameFormat).factory());
    } else {
      return Executors.newSingleThreadScheduledExecutor(newVirtualThreadFactory(nameFormat));
    }
  }

  /**
   * Creates a new virtual thread executor service with a fixed number of threads.
   *
   * @param nameFormat the name format for the threads
   * @param nThreads the number of threads
   * @return a new {@link ExecutorService}
   */
  public static ExecutorService newVirtualThreadExecutor(String nameFormat, int nThreads) {
    if (ENABLE_VIRTUAL_THREADS) {
      return Executors.newVirtualThreadPerTaskExecutor();
    } else {
      return Executors.newFixedThreadPool(nThreads, newVirtualThreadFactory(nameFormat));
    }
  }

  /**
   * Creates a new virtual thread scheduled executor service with multiple threads.
   *
   * @param nameFormat the name format for the threads
   * @param nThreads the number of threads
   * @return a new {@link ScheduledExecutorService}
   */
  public static ScheduledExecutorService newVirtualThreadScheduledExecutor(String nameFormat, int nThreads) {
    if (ENABLE_VIRTUAL_THREADS) {
      return Executors.newScheduledThreadPool(nThreads, Thread.ofVirtual().name(nameFormat).factory());
    } else {
      return Executors.newScheduledThreadPool(nThreads, newVirtualThreadFactory(nameFormat));
    }
  }

  /**
   * Checks if virtual threads are enabled.
   *
   * @return true if virtual threads are enabled
   */
  public static boolean isVirtualThreadsEnabled() {
    return ENABLE_VIRTUAL_THREADS;
  }
} 