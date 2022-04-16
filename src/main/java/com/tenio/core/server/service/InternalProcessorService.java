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

package com.tenio.core.server.service;

import com.tenio.core.controller.Controller;
import com.tenio.core.entity.manager.PlayerManager;

/**
 * The internal processor service, the heart of server.
 */
public interface InternalProcessorService extends Controller {

/**
* Subscribes all events on the server.
*/
  void subscribe();

/**
* Sets the maximum number of players allowed participating on the server.
*
* @param maxPlayers <code>integer</code> value, the maximum number of players allowed participating on the server
*/
  void setMaxNumberPlayers(int maxPlayers);

/**
* Determines if a player could be kept its connection when it is disconnected from the server for a while.
*
* @param keepPlayerOnDisconnection sets to <code>true</code> if a player could be kept its connection when it is disconnected from the server for a while, otherwise <code>false</code>
*/
  void setKeepPlayerOnDisconnection(boolean keepPlayerOnDisconnection);

/**
* Sets a player manager for the server which is used to manage all players.
*
* @param playerManager an instance of {@link PlayerManager}
*/
  void setPlayerManager(PlayerManager playerManager);
}
