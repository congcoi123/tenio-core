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

package com.tenio.core.exception;

import com.tenio.core.network.define.RestMethod;
import com.tenio.core.network.define.data.PathConfig;
import java.util.List;

/**
 * When you try to define an existing Uri method in the HTTP server.
 */
public final class DuplicatedUriAndMethodException extends RuntimeException {

  private static final long serialVersionUID = -5226506274080400540L;

  /**
   * Initialization.
   *
   * @param method      a {@link RestMethod}
   * @param pathConfigs a list of {@link PathConfig}
   * @see List 
   */
  public DuplicatedUriAndMethodException(RestMethod method, List<PathConfig> pathConfigs) {
    super(String.format("Duplicated REST method: %s, with the list of configurations: %s",
        method.toString(),
        pathConfigs.toString()));
  }
}
