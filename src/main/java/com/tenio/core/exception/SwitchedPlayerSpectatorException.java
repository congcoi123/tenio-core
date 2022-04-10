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

import com.tenio.core.entity.define.result.SwitchedPlayerSpectatorResult;

/**
 * When a player try to change its role to a spectator and vice versa.
 */
public final class SwitchedPlayerSpectatorException extends RuntimeException {

  private static final long serialVersionUID = 8858056991799548907L;

  private final SwitchedPlayerSpectatorResult result;

/**
* Creats a new exception.
*
* @param message a warning {@link String} message
* @param result a {@link SwitchedPlayerSpectatorResult} singleton value indicates result of switching
*/
  public SwitchedPlayerSpectatorException(String message, SwitchedPlayerSpectatorResult result) {
    super(message);
    this.result = result;
  }

/**
* Retrieves the switching result.
*
* @return result a {@link SwitchedPlayerSpectatorResult} singleton value indicates result of switching
*/
  public SwitchedPlayerSpectatorResult getResult() {
    return result;
  }
}
