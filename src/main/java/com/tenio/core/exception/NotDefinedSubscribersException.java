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

package com.tenio.core.exception;

/**
 * Exception thrown when an event is declared but no subscribers are defined to handle it.
 * This exception helps maintain the integrity of the event system by ensuring that all
 * declared events have corresponding handlers.
 * 
 * <p>This exception typically occurs when:
 * <ul>
 * <li>An event is declared but no class implements its subscriber interface</li>
 * <li>The subscriber class exists but is not properly registered</li>
 * <li>The subscriber class is missing required annotations</li>
 * <li>The subscriber interface is not properly implemented</li>
 * </ul>
 * 
 * <p>To resolve this exception:
 * <ul>
 * <li>Implement the missing subscriber interfaces</li>
 * <li>Register all event subscribers with the event system</li>
 * <li>Ensure subscriber classes are properly annotated</li>
 * <li>Verify subscriber method signatures match event requirements</li>
 * </ul>
 * 
 * <p>Example scenario:
 * <pre>
 * // Event interface
 * public interface PlayerJoinedEvent {
 *     void onPlayerJoined(Player player);
 * }
 * 
 * // Event declaration without subscriber
 * public class GameEventManager {
 *     public void firePlayerJoined(Player player) {
 *         // Will throw NotDefinedSubscribersException
 *         // because no class implements PlayerJoinedEvent
 *     }
 * }
 * 
 * // Fix by adding subscriber
 * public class PlayerEventHandler implements PlayerJoinedEvent {
 *     public void onPlayerJoined(Player player) {
 *         // Handle player joined event
 *     }
 * }
 * </pre>
 */
public final class NotDefinedSubscribersException extends RuntimeException {

  private static final long serialVersionUID = 4569867192216119437L;

  /**
   * Creates a new NotDefinedSubscribersException with details about the missing subscribers.
   * The exception message includes a list of interfaces that need to be implemented to handle
   * the declared events.
   *
   * @param classes array of {@link Class} objects representing the subscriber interfaces that
   *                need to be implemented
   */
  public NotDefinedSubscribersException(Class<?>... classes) {
    super(getMessage(classes));
  }

  /**
   * Formats the exception message by listing all missing subscriber interfaces.
   * The message is formatted as "Need to implement interfaces: interface1, interface2, ..."
   *
   * @param classes array of {@link Class} objects representing the missing subscriber interfaces
   * @return formatted error message string
   */
  private static String getMessage(Class<?>... classes) {
    var builder = new StringBuilder();
    builder.append("Need to implement interfaces: ");
    for (var clazz : classes) {
      builder.append(clazz.getName());
      builder.append(", ");
    }
    builder.setLength(builder.length() - 2);
    return builder.toString();
  }
}
