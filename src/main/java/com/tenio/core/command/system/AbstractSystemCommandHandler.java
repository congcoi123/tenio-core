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

package com.tenio.core.command.system;

import com.tenio.core.bootstrap.annotation.SystemCommand;
import com.tenio.core.handler.AbstractHandler;
import java.util.Arrays;
import java.util.List;

/**
 * The base class for all self defined commands.
 *
 * @since 0.4.0
 */
public abstract class AbstractSystemCommandHandler extends AbstractHandler {

  private SystemCommandManager systemCommandManager;

  public String getLabel() {
    return getClass().getAnnotation(SystemCommand.class).label();
  }

  public String[] getUsage() {
    return getClass().getAnnotation(SystemCommand.class).usage();
  }

  public String getDescription() {
    return getClass().getAnnotation(SystemCommand.class).description();
  }

  public boolean isRunningBackground() {
    return getClass().getAnnotation(SystemCommand.class).isBackgroundRunning();
  }

  public SystemCommandManager getCommandManager() {
    return systemCommandManager;
  }

  public void setCommandManager(SystemCommandManager systemCommandManager) {
    this.systemCommandManager = systemCommandManager;
  }

  /**
   * It is called when the server invokes a command.
   *
   * @param args The arguments to the command
   */
  public abstract void execute(List<String> args);

  @Override
  public String toString() {
    return "CommandHandler{" +
        "class=" + getClass().getName() +
        ", label=" + getLabel() +
        ", description=" + getDescription() +
        ", usage=" + Arrays.toString(getUsage()) +
        "}";
  }
}
