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

package com.tenio.core.bootstrap.event.handlers;

import com.tenio.common.bootstrap.annotation.AutowiredAcceptNull;
import com.tenio.common.bootstrap.annotation.Component;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.data.ServerMessage;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.define.result.PlayerLoggedInResult;
import com.tenio.core.entity.define.result.PlayerReconnectedResult;
import com.tenio.core.event.Subscriber;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.extension.events.EventDisconnectPlayer;
import com.tenio.core.extension.events.EventPlayerLoggedinResult;
import com.tenio.core.extension.events.EventPlayerReconnectRequestHandle;
import com.tenio.core.extension.events.EventPlayerReconnectedResult;
import com.tenio.core.extension.events.EventReceivedMessageFromPlayer;
import com.tenio.core.extension.events.EventSendMessageToPlayer;
import com.tenio.core.network.entity.session.Session;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dispatching all events related to players.
 */
@Component
public final class PlayerEventHandler {

  @AutowiredAcceptNull
  private EventPlayerLoggedinResult eventPlayerLoggedInResult;

  @AutowiredAcceptNull
  private EventPlayerReconnectRequestHandle eventPlayerReconnectRequestHandle;

  @AutowiredAcceptNull
  private EventPlayerReconnectedResult eventPlayerReconnectedResult;

  @AutowiredAcceptNull
  private EventReceivedMessageFromPlayer eventReceivedMessageFromPlayer;

  @AutowiredAcceptNull
  private EventSendMessageToPlayer eventSendMessageToPlayer;

  @AutowiredAcceptNull
  private EventDisconnectPlayer eventDisconnectPlayer;

  /**
   * Initialization.
   *
   * @param eventManager the event manager
   */
  public void initialize(EventManager eventManager) {

    final var eventPlayerLoggedInResultOp =
        Optional.ofNullable(eventPlayerLoggedInResult);

    final var eventPlayerReconnectRequestHandleOp =
        Optional.ofNullable(eventPlayerReconnectRequestHandle);
    final var eventPlayerReconnectedResultOp =
        Optional.ofNullable(eventPlayerReconnectedResult);

    final var eventReceivedMessageFromPlayerOp =
        Optional.ofNullable(eventReceivedMessageFromPlayer);
    final var eventSendMessageToPlayerOp =
        Optional.ofNullable(eventSendMessageToPlayer);

    final var eventDisconnectPlayerOp =
        Optional.ofNullable(eventDisconnectPlayer);

    eventPlayerLoggedInResultOp.ifPresent(new Consumer<EventPlayerLoggedinResult>() {

      public void accept(EventPlayerLoggedinResult event) {

        eventManager.on(ServerEvent.PLAYER_LOGGEDIN_RESULT, new Subscriber() {

          @Override
          public Object dispatch(Object... params) {
            var player = (Player) params[0];
            var result = (PlayerLoggedInResult) params[1];

            event.handle(player, result);

            return null;
          }
        });
      }
    });

    eventPlayerReconnectRequestHandleOp.ifPresent(
        new Consumer<EventPlayerReconnectRequestHandle>() {

          public void accept(EventPlayerReconnectRequestHandle event) {

            eventManager.on(ServerEvent.PLAYER_RECONNECT_REQUEST_HANDLE, new Subscriber() {

              @Override
              public Object dispatch(Object... params) {
                var session = (Session) params[0];
                var message = (ServerMessage) params[1];

                event.handle(session, message);

                return null;
              }
            });
          }
        });

    eventPlayerReconnectedResultOp.ifPresent(new Consumer<EventPlayerReconnectedResult>() {

      public void accept(EventPlayerReconnectedResult event) {

        eventManager.on(ServerEvent.PLAYER_RECONNECTED_RESULT, new Subscriber() {

          @Override
          public Object dispatch(Object... params) {
            var player = (Player) params[0];
            var session = (Session) params[1];
            var result = (PlayerReconnectedResult) params[2];

            event.handle(player, session, result);

            return null;
          }
        });
      }
    });

    eventReceivedMessageFromPlayerOp.ifPresent(new Consumer<EventReceivedMessageFromPlayer>() {

      @Override
      public void accept(EventReceivedMessageFromPlayer event) {
        eventManager.on(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER, new Subscriber() {

          @Override
          public Object dispatch(Object... params) {
            var player = (Player) params[0];
            var message = (ServerMessage) params[1];

            event.handle(player, message);

            return null;
          }
        });
      }
    });

    eventSendMessageToPlayerOp.ifPresent(new Consumer<EventSendMessageToPlayer>() {

      @Override
      public void accept(EventSendMessageToPlayer event) {
        eventManager.on(ServerEvent.SEND_MESSAGE_TO_PLAYER, new Subscriber() {

          @Override
          public Object dispatch(Object... params) {
            var player = (Player) params[0];
            var message = (ServerMessage) params[1];

            event.handle(player, message);

            return null;
          }
        });
      }
    });

    eventDisconnectPlayerOp.ifPresent(new Consumer<EventDisconnectPlayer>() {

      @Override
      public void accept(EventDisconnectPlayer event) {
        eventManager.on(ServerEvent.DISCONNECT_PLAYER, new Subscriber() {

          @Override
          public Object dispatch(Object... params) {
            var player = (Player) params[0];
            var mode = (PlayerDisconnectMode) params[1];

            event.handle(player, mode);

            return null;
          }
        });
      }
    });
  }
}
