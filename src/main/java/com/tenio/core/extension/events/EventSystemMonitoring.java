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

package com.tenio.core.extension.events;

import com.tenio.core.configuration.define.CoreConfigurationType;

/**
 * Monitoring the system information.
 */
@FunctionalInterface
public interface EventSystemMonitoring {

  /**
   * Monitoring the system information on the server. The information should be frequently
   * updated every interval time.
   *
   * @param cpuUsage            <code>double</code> value, the current CPU's usage
   * @param totalMemory         <code>long</code> value, the total size of memory in byte that
   *                            the JVM occupy from the host machine
   * @param usedMemory          <code>long</code> value, the memory volume that the JVM is consuming
   * @param freeMemory          <code>long</code> value, the available memory that the JVM can use
   * @param countRunningThreads <code>integer</code> value, the current running threads on the JVM
   * @see CoreConfigurationType#INTERVAL_SYSTEM_MONITORING
   */
  void handle(double cpuUsage, long totalMemory, long usedMemory, long freeMemory,
              int countRunningThreads);
}
