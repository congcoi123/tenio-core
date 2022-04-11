/*
The MIT License

Copyright (c) 2016-2021 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.define;

/**
 * Definition the priority for incoming requests.
 * @see RequestQueue
 * @see RequestQueuePolicy
 */
public enum RequestPriority {

/**
* The lowest priority of a request waiting to be processed.
*/
  LOWEST(1),
  /**
* The low priority of a request waiting to be processed.
*/
  LOW(2),
  /**
* The regular priority of a request waiting to be processed.
*/
  NORMAL(3),
  /**
* The highest priority of a request waiting to be processed.
*/
  QUICKEST(4);

  private final int value;

  RequestPriority(final int value) {
    this.value = value;
  }

/**
* Retrieves the numeric value of a request's priority.
*
* @return the <code>integer</code> value of a request's priority
*/
  public final int getValue() {
    return value;
  }

  @Override
  public final String toString() {
    return name();
  }
}
