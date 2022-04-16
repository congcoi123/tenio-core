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

package com.tenio.core.network.entity.protocol;

import com.tenio.core.entity.Player;
import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;
import java.util.List;

/**
 * The response was formed when the server wants to send a message to clients.
 */
public interface Response {

/**
* Retrieves an array of binaries data that is carried by the response.
*
* @return an array of <code>byte</code> data that is carried by the response
*/
  byte[] getContent();

/**
* Sets content for the response.
*
* @param content an array of <code>byte</code> data that is carried by the response
* @return the pointer of response
*/
  Response setContent(byte[] content);

// getReceipientPlayers
  Collection<Player> getPlayers();

// getNonsessionReceipientPlayers
  Collection<Player> getNonSessionPlayers();

/**
* Retrieves a list of recipient socket sessions.
*
* @return a list of recipient socket {@link Session}s
* @see Collection
*/
  Collection<Session> getRecipientSocketSessions();

/**
* Retrieves a list of recipient socket sessions which recieves packets by datagram channel.
*
* @return a list of recipient socket {@link Session}s which recieves packets by datagram channel
* @see Collection
*/
  Collection<Session> getRecipientDatagramSessions();

/**
* Retrieves a list of recipient websocket sessions.
*
* @return a list of recipient websocket {@link Session}s
* @see Collection
*/
  Collection<Session> getRecipientWebSocketSessions();

// setRecipientPlayers
  Response setRecipients(Collection<Player> players);

// setRecipientPlayer
  Response setRecipient(Player player);
  
  // setRecipientSessions
  
  // setRecipientSession

/**
* Sets the higher priority for sending packets via datagram channel. In case the session in use is websocket, then this setting should be ignored.
*
* @return the pointer of response
*/
  Response prioritizedUdp();

/**
* Allows the sending content to be encrypted.
*
* @return the pointer of response
*/
  Response encrypted();

/**
* Sets priority for the response.
*
* @param priority the {@link ResponsePriority}
* @return the pointer of response
* @see Policy
*/
  Response priority(ResponsePriority priority);

/**
* Determines whether the response's content is encrypted.
*
* @return <code>true</code> if the response's content is encrypted, otherwise <code>false</code>
*/
  boolean isEncrypted();

/**
* Retrieves the current priority of response.
*
* @return the current {@link ResponsePriority} of response
* @see Policy
*/
  ResponsePriority getPriority();

/**
* Writes down the content data to sessions for sending to client sides.
*/
  void write();

// Remove it
  void write(List<Session> sessions);

/**
* Writes down the content data to sessions for sending to client sides.
*
* @param delayInSeconds allows delaying in the number of seconds
* @throws UnsupportedOperationException unsupported operation at the moment
*/
  default void writeInDelay(int delayInSeconds) {
    throw new UnsupportedOperationException();
  }
}
