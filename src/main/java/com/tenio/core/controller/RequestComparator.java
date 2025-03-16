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

package com.tenio.core.controller;

import com.tenio.core.network.entity.protocol.Request;
import java.util.Comparator;
import java.util.Objects;
import org.apache.logging.log4j.core.tools.picocli.CommandLine.InitializationException;

/**
 * A comparator for sorting network requests based on their priority and creation timestamp.
 * This class implements a natural ordering that considers both the request's priority level
 * and when it was created, ensuring fair and efficient request processing.
 * 
 * <p>The comparison logic:
 * <ol>
 * <li>First compares request priorities (lower value = higher priority)</li>
 * <li>If priorities are equal, compares creation timestamps (earlier = higher priority)</li>
 * </ol>
 * 
 * <p>This ordering ensures that:
 * <ul>
 * <li>High priority requests are processed first</li>
 * <li>Requests with equal priority are processed in FIFO order</li>
 * <li>No request starvation occurs due to timestamp consideration</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * var queue = new PriorityQueue(RequestComparator.newInstance());
 * queue.add(new Request(Priority.HIGH));   // Processed first
 * queue.add(new Request(Priority.NORMAL)); // Processed later
 * 
 * // Two requests with same priority
 * var request1 = new Request(Priority.NORMAL); // Created first
 * Thread.sleep(100);
 * var request2 = new Request(Priority.NORMAL); // Created later
 * // request1 will be processed before request2
 * </pre>
 * 
 * @see Request
 * @see java.util.PriorityQueue
 */
public final class RequestComparator implements Comparator<Request> {

  private static final RequestComparator instance = new RequestComparator();

  private RequestComparator() {
    if (Objects.nonNull(instance)) {
      throw new InitializationException("Could not recreate this class' instance");
    }
  }

  /**
   * Returns the singleton instance of the request comparator.
   * This method ensures that only one instance of the comparator exists in the application,
   * promoting consistent request ordering across all queues.
   *
   * @return the singleton {@link RequestComparator} instance
   */
  public static RequestComparator newInstance() {
    return instance;
  }

  /**
   * Compares two requests for ordering based on their priority and creation timestamp.
   * The comparison follows these rules:
   * <ol>
   * <li>Lower priority value indicates higher priority (negative values first)</li>
   * <li>For equal priorities, earlier creation timestamp indicates higher priority</li>
   * </ol>
   * 
   * <p>Return values:
   * <ul>
   * <li>Negative: request1 should be processed before request2</li>
   * <li>Zero: requests have equal priority and timestamp</li>
   * <li>Positive: request2 should be processed before request1</li>
   * </ul>
   *
   * @param request1 the first {@link Request} to compare
   * @param request2 the second {@link Request} to compare
   * @return negative if request1 has higher priority, positive if request2 has higher priority,
   *         zero if equal
   */
  @Override
  public int compare(Request request1, Request request2) {
    int result;

    if (request1.getPriority().getValue() < request2.getPriority().getValue()) {
      result = -1;
    } else if (request1.getPriority() == request2.getPriority()) {
      result = Long.compare(request1.getCreatedTimestamp(), request2.getCreatedTimestamp());
    } else {
      result = 1;
    }
    return result;
  }
}
