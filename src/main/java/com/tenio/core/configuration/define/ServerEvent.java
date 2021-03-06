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

package com.tenio.core.configuration.define;

import com.tenio.core.extension.events.EventAttachConnectionRequestValidation;
import com.tenio.core.extension.events.EventAttachedConnectionResult;
import com.tenio.core.extension.events.EventConnectionEstablishedResult;
import com.tenio.core.extension.events.EventDisconnectConnection;
import com.tenio.core.extension.events.EventDisconnectPlayer;
import com.tenio.core.extension.events.EventFetchedBandwidthInfo;
import com.tenio.core.extension.events.EventFetchedCcuInfo;
import com.tenio.core.extension.events.EventHttpRequestHandle;
import com.tenio.core.extension.events.EventHttpRequestValidation;
import com.tenio.core.extension.events.EventPlayerAfterLeftRoom;
import com.tenio.core.extension.events.EventPlayerBeforeLeaveRoom;
import com.tenio.core.extension.events.EventPlayerJoinedRoomResult;
import com.tenio.core.extension.events.EventPlayerLoggedinResult;
import com.tenio.core.extension.events.EventPlayerReconnectRequestHandle;
import com.tenio.core.extension.events.EventPlayerReconnectedResult;
import com.tenio.core.extension.events.EventReceivedMessageFromPlayer;
import com.tenio.core.extension.events.EventRoomCreatedResult;
import com.tenio.core.extension.events.EventRoomWillBeRemoved;
import com.tenio.core.extension.events.EventSendMessageToPlayer;
import com.tenio.core.extension.events.EventServerException;
import com.tenio.core.extension.events.EventServerInitialization;
import com.tenio.core.extension.events.EventServerTeardown;
import com.tenio.core.extension.events.EventSwitchParticipantToSpectatorResult;
import com.tenio.core.extension.events.EventSwitchSpectatorToParticipantResult;
import com.tenio.core.extension.events.EventSystemMonitoring;
import com.tenio.core.extension.events.EventWriteMessageToConnection;

/**
 * All supported events could be emitted on the server.
 */
public enum ServerEvent {

  /**
   * When a new session created in the management list.
   */
  SESSION_CREATED,
  /**
   * When a new session requests to connect to the server.
   */
  SESSION_REQUEST_CONNECTION,
  /**
   * When there is any issue occurs to a session.
   */
  SESSION_OCCURRED_EXCEPTION,
  /**
   * When a session is going to disconnect to the server.
   *
   * @see EventDisconnectConnection
   */
  SESSION_WILL_BE_CLOSED,
  /**
   * When a message from client side sent to a session.
   */
  SESSION_READ_MESSAGE,
  /**
   * When a message sent to a session.
   *
   * @see EventWriteMessageToConnection
   */
  SESSION_WRITE_MESSAGE,
  /**
   * When a message sent to the sever from client side via datagram channel.
   */
  DATAGRAM_CHANNEL_READ_MESSAGE,
  /**
   * When the server finished initialization and is ready.
   *
   * @see EventServerInitialization
   */
  SERVER_INITIALIZATION,
  /**
   * When the server responds a connection request from client side.
   *
   * @see EventConnectionEstablishedResult
   */
  CONNECTION_ESTABLISHED_RESULT,
  /**
   * When the server responds a player logged in request.
   *
   * @see EventPlayerLoggedinResult
   */
  PLAYER_LOGGEDIN_RESULT,
  /**
   * When the server handles a reconnection request.
   *
   * @see EventPlayerReconnectRequestHandle
   */
  PLAYER_RECONNECT_REQUEST_HANDLE,
  /**
   * When the server responds a player reconnected request.
   *
   * @see EventPlayerReconnectedResult
   */
  PLAYER_RECONNECTED_RESULT,
  /**
   * When the server sends a message to client side on behalf of its player.
   *
   * @see EventSendMessageToPlayer
   */
  SEND_MESSAGE_TO_PLAYER,
  /**
   * When the server receives a message from client side on behalf of its player.
   *
   * @see EventReceivedMessageFromPlayer
   */
  RECEIVED_MESSAGE_FROM_PLAYER,
  /**
   * When the server responds a room creation request.
   *
   * @see EventRoomCreatedResult
   */
  ROOM_CREATED_RESULT,
  /**
   * When a room is going to be removed from the management list.
   *
   * @see EventRoomWillBeRemoved
   */
  ROOM_WILL_BE_REMOVED,
  /**
   * When the server responds a request from player regarding joining a room.
   *
   * @see EventPlayerJoinedRoomResult
   */
  PLAYER_JOINED_ROOM_RESULT,
  /**
   * When a player is going to leave its current room.
   *
   * @see EventPlayerBeforeLeaveRoom
   */
  PLAYER_BEFORE_LEAVE_ROOM,
  /**
   * When a player has just left its room.
   *
   * @see EventPlayerAfterLeftRoom
   */
  PLAYER_AFTER_LEFT_ROOM,
  /**
   * When a player attempts to change its role from 'participant' to 'spectator'.
   *
   * @see EventSwitchParticipantToSpectatorResult
   */
  SWITCH_PARTICIPANT_TO_SPECTATOR,
  /**
   * When a player attempts to change its role from 'spectator' to 'participant'.
   *
   * @see EventSwitchSpectatorToParticipantResult
   */
  SWITCH_SPECTATOR_TO_PARTICIPANT,
  /**
   * When a player is going to disconnect from the server.
   *
   * @see EventDisconnectPlayer
   */
  DISCONNECT_PLAYER,
  /**
   * When a connection is going to disconnect from the server.
   *
   * @see EventDisconnectConnection
   */
  DISCONNECT_CONNECTION,
  /**
   * When the server validates a UDP attaching request from a player.
   *
   * @see EventAttachConnectionRequestValidation
   */
  ATTACH_CONNECTION_REQUEST_VALIDATION,
  /**
   * When the server responds a UDP attaching request from a player.
   *
   * @see EventAttachedConnectionResult
   */
  ATTACHED_CONNECTION_RESULT,
  /**
   * When the server provides information regarding CCU.
   *
   * @see EventFetchedCcuInfo
   */
  FETCHED_CCU_INFO,
  /**
   * When the server provides information regarding bandwidth.
   *
   * @see EventFetchedBandwidthInfo
   */
  FETCHED_BANDWIDTH_INFO,
  /**
   * When the server provides information regarding system.
   *
   * @see EventSystemMonitoring
   */
  SYSTEM_MONITORING,
  /**
   * When the server validates an HTTP request from a player.
   *
   * @see EventHttpRequestValidation
   */
  HTTP_REQUEST_VALIDATION,
  /**
   * When the server handles an HTTP request from a player.
   *
   * @see EventHttpRequestHandle
   */
  HTTP_REQUEST_HANDLE,
  /**
   * When there is any exception occurs on the server.
   *
   * @see EventServerException
   */
  SERVER_EXCEPTION,
  /**
   * When the server is going to shut down.
   *
   * @see EventServerTeardown
   */
  SERVER_TEARDOWN;

  @Override
  public String toString() {
    return this.name();
  }
}
