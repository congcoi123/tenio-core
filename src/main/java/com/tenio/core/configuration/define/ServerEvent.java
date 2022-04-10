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

package com.tenio.core.configuration.define;

/**
 * All events could be emitted on the server.
 */
public enum ServerEvent {

/**
* When a new session created in the management list.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SESSION_CREATED,
/**
* When a new session requests to connect to the server.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SESSION_REQUEST_CONNECTION,
/**
* When there is any issue occurs to the session.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SESSION_OCCURRED_EXCEPTION,
/**
* When a session is going to disconnect to the server.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SESSION_WILL_BE_CLOSED,
/**
* When a message from client side sent to the session.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SESSION_READ_MESSAGE,
/**
* When a message sent to the sever from client side via datagram channel.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  DATAGRAM_CHANNEL_READ_MESSAGE,
/**
* When the server finished initialization and is ready.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SERVER_INITIALIZATION,
/**
* When the server responds a connection request from client side.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  CONNECTION_ESTABLISHED_RESULT,
/**
* When the server responds a player loggedin request.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  PLAYER_LOGGEDIN_RESULT,
/**
* When the server handles a reconnection request.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  PLAYER_RECONNECT_REQUEST_HANDLE,
/**
* When the server responds a player reconnected request.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  PLAYER_RECONNECTED_RESULT,
/**
* When the server sends a message to client side on behalf of its player.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SEND_MESSAGE_TO_PLAYER,
/**
* When the server receives a message from client side on behalf of its player.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  RECEIVED_MESSAGE_FROM_PLAYER,
/**
* When the server responds a room creation request.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  ROOM_CREATED_RESULT,
/**
* When a room is going to be removed from the management list.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  ROOM_WILL_BE_REMOVED,
/**
* When the server responds a request from player regarding joining a room.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  PLAYER_JOINED_ROOM_RESULT,
/**
* When a player is going to leave its current room.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  PLAYER_BEFORE_LEAVE_ROOM,
/**
* When a player has just left its room.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  PLAYER_AFTER_LEFT_ROOM,
/**
* When a player attempt to change its role from 'player' to 'spectator'.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SWITCH_PLAYER_TO_SPECTATOR,
/**
* When a player attempt to change its role from 'spectator' to 'player'.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SWITCH_SPECTATOR_TO_PLAYER,
/**
* When a player is going to disconnect from the server.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  DISCONNECT_PLAYER,
/**
* When a connection is going to disconnect from the server.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  DISCONNECT_CONNECTION,
/**
* When the server validates an UDP attaching request from a player.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
ATTACH_CONNECTION_REQUEST_VALIDATION,
/**
* When the server responds an UDP attaching request from a player.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  ATTACHED_CONNECTION_RESULT,
/**
* When the server provides information regarding CCU.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  FETCHED_CCU_INFO,
/**
* When the server provides information regarding bandwidth.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  FETCHED_BANDWIDTH_INFO,
/**
* When the server provides information regarding system.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SYSTEM_MONITORING,
/**
* When the server validates an HTTP request from a player.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  HTTP_REQUEST_VALIDATION,
/**
* When the server handles an HTTP request from a player.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  HTTP_REQUEST_HANDLE,
/**
* When there is any exception occurs on the server.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SERVER_EXCEPTION,
/**
* When the server is going to shutdown.
*
* @see Event
* @see Mode
* @see Result
* @see Exception
*/
  SERVER_TEARDOWN;

  @Override
  public String toString() {
    return this.name();
  }
}
