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

package com.tenio.core.event.handler.implement;

import com.tenio.common.data.DataCollection;
import com.tenio.common.utility.TimeUtility;
import com.tenio.core.bootstrap.annotation.AutowiredAcceptNull;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.define.result.PlayerLoggedInResult;
import com.tenio.core.entity.define.result.PlayerReconnectedResult;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.handler.event.EventDisconnectPlayer;
import com.tenio.core.handler.event.EventPlayerLoggedinResult;
import com.tenio.core.handler.event.EventPlayerReconnectRequestHandle;
import com.tenio.core.handler.event.EventPlayerReconnectedResult;
import com.tenio.core.handler.event.EventReceivedMessageFromPlayer;
import com.tenio.core.handler.event.EventSendMessageToPlayer;
import com.tenio.core.network.entity.session.Session;
import java.util.Optional;

/**
 * Dispatching all events related to players.
 */
@Component
public final class PlayerEventHandler {

  @AutowiredAcceptNull
  private EventPlayerLoggedinResult<Player> eventPlayerLoggedInResult;

  @AutowiredAcceptNull
  private EventPlayerReconnectRequestHandle<Player> eventPlayerReconnectRequestHandle;

  @AutowiredAcceptNull
  private EventPlayerReconnectedResult<Player> eventPlayerReconnectedResult;

  @AutowiredAcceptNull
  private EventReceivedMessageFromPlayer<Player> eventReceivedMessageFromPlayer;

  @AutowiredAcceptNull
  private EventSendMessageToPlayer<Player> eventSendMessageToPlayer;

  @AutowiredAcceptNull
  private EventDisconnectPlayer<Player> eventDisconnectPlayer;

  /**
   * Initialization.
   *
   * @param eventManager the event manager
   */
  public void initialize(EventManager eventManager) {

    final Optional<EventPlayerLoggedinResult<Player>> eventPlayerLoggedInResultOp =
        Optional.ofNullable(eventPlayerLoggedInResult);

    final Optional<EventPlayerReconnectRequestHandle<Player>> eventPlayerReconnectRequestHandleOp =
        Optional.ofNullable(eventPlayerReconnectRequestHandle);
    final Optional<EventPlayerReconnectedResult<Player>> eventPlayerReconnectedResultOp =
        Optional.ofNullable(eventPlayerReconnectedResult);

    final Optional<EventReceivedMessageFromPlayer<Player>> eventReceivedMessageFromPlayerOp =
        Optional.ofNullable(eventReceivedMessageFromPlayer);
    final Optional<EventSendMessageToPlayer<Player>> eventSendMessageToPlayerOp =
        Optional.ofNullable(eventSendMessageToPlayer);

    final Optional<EventDisconnectPlayer<Player>> eventDisconnectPlayerOp =
        Optional.ofNullable(eventDisconnectPlayer);

    eventPlayerLoggedInResultOp.ifPresent(
        event -> eventManager.on(ServerEvent.PLAYER_LOGGEDIN_RESULT, params -> {
          Player player = (Player) params[0];
          PlayerLoggedInResult result = (PlayerLoggedInResult) params[1];

          event.handle(player, result);

          return null;
        }));

    eventPlayerReconnectRequestHandleOp.ifPresent(
        event -> eventManager.on(ServerEvent.PLAYER_RECONNECT_REQUEST_HANDLE, params -> {
          Session session = (Session) params[0];
          DataCollection message = (DataCollection) params[1];

          return event.handle(session, message);
        }));

    eventPlayerReconnectedResultOp.ifPresent(
        event -> eventManager.on(ServerEvent.PLAYER_RECONNECTED_RESULT, params -> {
          Player player = (Player) params[0];
          Session session = (Session) params[1];
          PlayerReconnectedResult result = (PlayerReconnectedResult) params[2];

          event.handle(player, session, result);

          return null;
        }));

    eventReceivedMessageFromPlayerOp.ifPresent(
        event -> eventManager.on(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER, params -> {
          Player player = (Player) params[0];
          DataCollection message = (DataCollection) params[1];
          player.setLastReadTime(TimeUtility.currentTimeMillis());

          event.handle(player, message);

          return null;
        }));

    eventSendMessageToPlayerOp.ifPresent(
        event -> eventManager.on(ServerEvent.SEND_MESSAGE_TO_PLAYER, params -> {
          Player player = (Player) params[0];
          DataCollection message = (DataCollection) params[1];
          player.setLastWriteTime(TimeUtility.currentTimeMillis());

          event.handle(player, message);

          return null;
        }));

    eventDisconnectPlayerOp.ifPresent(event -> eventManager.on(ServerEvent.DISCONNECT_PLAYER,
        params -> {
          Player player = (Player) params[0];
          PlayerDisconnectMode mode = (PlayerDisconnectMode) params[1];

          event.handle(player, mode);

          return null;
        }));
  }
}
