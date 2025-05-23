/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

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

package com.tenio.core.command.client;

import com.tenio.common.data.DataCollection;
import com.tenio.core.entity.Player;
import com.tenio.core.handler.AbstractHandler;

/**
 * The base class for all self defined commands.
 *
 * @since 0.5.0
 */
public abstract class AbstractClientCommandHandler<P extends Player> extends AbstractHandler {

  private ClientCommandManager clientCommandManager;

  /**
   * Retrieves the client command manager.
   *
   * @return an instance of {@link ClientCommandManager}
   */
  public ClientCommandManager getCommandManager() {
    return clientCommandManager;
  }

  /**
   * Sets value for the client command manager.
   * *
   * @param clientCommandManager an instance of {@link ClientCommandManager}
   */
  public void setCommandManager(ClientCommandManager clientCommandManager) {
    this.clientCommandManager = clientCommandManager;
  }

  /**
   * It is called when the server invokes a command.
   *
   * @param player  The receiver which gets command from its client
   * @param message The message as command
   */
  public abstract void execute(P player, DataCollection message);
}
