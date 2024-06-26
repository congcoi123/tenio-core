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

package com.tenio.core.configuration.constant;

import com.tenio.common.data.zero.ZeroArray;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.bootstrap.Bootstrapper;
import com.tenio.core.network.zero.engine.implement.ZeroReaderImpl;

/**
 * This class holds constant values for the module.
 */
public final class CoreConstant {

  /**
   * The HTTP response with UTF-8 encoding.
   */
  public static final String UTF_8 = "UTF-8";
  /**
   * The HTTP response with content type in JSON.
   */
  public static final String CONTENT_TYPE_JSON = "application/json";
  /**
   * The HTTP response with content type in text.
   */
  public static final String CONTENT_TYPE_TEXT = "text/html";
  /**
   * The server's default configuration file name.
   */
  public static final String DEFAULT_CONFIGURATION_FILE = "configuration.xml";
  /**
   * The default bootstrap package name using for the bootstrap to scan all classes inside it.
   *
   * @see Bootstrapper
   */
  public static final String DEFAULT_BOOTSTRAP_PACKAGE = "com.tenio.core.bootstrap";
  /**
   * The default events package name using for the bootstrap to scan all event interfaces inside it.
   *
   * @see Bootstrapper
   */
  public static final String DEFAULT_EVENT_PACKAGE = "com.tenio.core.event";
  /**
   * The default commands package.
   */
  public static final String DEFAULT_COMMAND_PACKAGE = "com.tenio.core.command";
  /**
   * The default restful controllers package.
   */
  public static final String DEFAULT_REST_CONTROLLER_PACKAGE = "com.tenio.core.network.jetty" +
      ".controller";
  /**
   * The default key value which carries the convey ID. This is for the first connection to UDP
   * channel on the server side from the client.
   *
   * @see ZeroReaderImpl
   */
  public static final String DEFAULT_KEY_UDP_CONVEY_ID = "u";
  /**
   * The default data value which carries message (array, map). This is for the first connection to
   * UDP channel on the server side from the client.
   *
   * @see ZeroReaderImpl
   * @see ZeroArray
   * @see ZeroMap
   */
  public static final String DEFAULT_KEY_UDP_MESSAGE_DATA = "d";
  /**
   * When the required port is unavailable but the client still inquiries it.
   */
  public static final int NULL_PORT_VALUE = -1;

  private CoreConstant() {
    throw new UnsupportedOperationException("This class does not support to create a new instance");
  }
}
