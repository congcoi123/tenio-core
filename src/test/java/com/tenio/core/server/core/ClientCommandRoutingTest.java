/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

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

package com.tenio.core.server.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataUtility;
import com.tenio.core.api.ServerApi;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.client.ClientCommandPolicy;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.zero.engine.manager.DatagramChannelManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientCommandRoutingTest {
  private final EventManager events = mock(EventManager.class);
  private final ClientCommandManager commands = mock(ClientCommandManager.class);
  private final ClientCommandPolicy policy = mock(ClientCommandPolicy.class);
  private final Session session = mock(Session.class);
  private final Player player = mock(Player.class);
  private final PlayerManager players = mock(PlayerManager.class);
  private final DataCollection message = DataUtility.newZeroMap().putShort("op", (short) 42);
  private ZeroProcessorImpl processor;

  @BeforeEach
  void setUp() {
    processor =
        ZeroProcessorImpl.newInstance(
            events, mock(ServerApi.class), mock(DatagramChannelManager.class));
    processor.setPlayerManager(players);
    when(session.isAssociatedToPlayer(Session.AssociatedState.DONE)).thenReturn(true);
    when(session.getName()).thenReturn("alice");
    when(players.getPlayerByIdentity("alice")).thenReturn(player);
    when(player.getSession()).thenReturn(Optional.of(session));
  }

  private void receive() throws Exception {
    var method =
        ZeroProcessorImpl.class.getDeclaredMethod(
            "processSessionReadMessage", Session.class, DataCollection.class);
    method.setAccessible(true);
    method.invoke(processor, session, message);
  }

  @SuppressWarnings("unchecked")
  private void configureCommands() {
    var handler = mock(AbstractClientCommandHandler.class);
    when(commands.getHandlers()).thenReturn(Map.of((short) 42, handler));
    when(commands.getHandler((short) 42)).thenReturn(handler);
    processor.configureClientCommands(commands, policy);
  }

  @Test
  void noPolicyPreservesEventModeEvenWithCommands() throws Exception {
    processor.configureClientCommands(commands, null);
    receive();
    verify(events).emit(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER, player, message);
    verifyNoInteractions(policy, commands);
  }

  @Test
  void noCommandsCachesDisabledModeAndNeverInvokesPolicy() throws Exception {
    var registry = new HashMap<Short, AbstractClientCommandHandler<Player, DataCollection>>();
    when(commands.getHandlers()).thenReturn(registry);
    processor.configureClientCommands(commands, policy);
    receive();
    receive();
    verify(commands, times(1)).getHandlers();
    verifyNoInteractions(policy);
    verify(events, times(2)).emit(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER, player, message);
  }

  @Test
  void routesResolvedPayloadExactlyOnceAndSkipsLegacyEvent() throws Exception {
    configureCommands();
    var payload = DataUtility.newZeroMap().putString("text", "hello");
    when(policy.applyPolicy(message))
        .thenReturn(new ClientCommandPolicy.CommandRequest((short) 42, payload));
    receive();
    verify(commands).invoke((short) 42, player, payload);
    verify(events, never()).emit(eq(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER), any(), any());
    verify(commands, times(1)).getHandlers();
  }

  @Test
  void missingCodeCannotFallBackToEventMode() throws Exception {
    configureCommands();
    receive();
    verify(commands, never()).invoke(any(), any(), any());
    verifyNoInteractions(events);
  }

  @Test
  void malformedEnvelopeCannotFallBackToEventMode() throws Exception {
    configureCommands();
    when(policy.applyPolicy(message)).thenThrow(new IllegalArgumentException("bad payload"));
    receive();
    verify(commands, never()).invoke(any(), any(), any());
    verifyNoInteractions(events);
  }

  @Test
  void unknownCommandCannotFallBackToEventMode() throws Exception {
    configureCommands();
    when(policy.applyPolicy(message))
        .thenReturn(new ClientCommandPolicy.CommandRequest((short) 99, message));
    receive();
    verify(commands, never()).invoke(any(), any(), any());
    verifyNoInteractions(events);
  }

  @Test
  void fullMailboxDoesNotFallBackOrTerminateReceiveProcessing() throws Exception {
    configureCommands();
    when(policy.applyPolicy(message))
        .thenReturn(new ClientCommandPolicy.CommandRequest((short) 42, message));
    doThrow(new RejectedExecutionException())
        .doNothing()
        .when(commands)
        .invoke((short) 42, player, message);
    receive();
    receive();
    verify(commands, times(2)).invoke((short) 42, player, message);
    verifyNoInteractions(events);
  }

  @Test
  void staleSessionNeverReachesPolicy() throws Exception {
    configureCommands();
    when(player.getSession()).thenReturn(Optional.of(mock(Session.class)));
    receive();
    verifyNoInteractions(policy, events);
  }

  @Test
  void destroyedProcessorDoesNotRetainThePreviousRoutingMode() throws Exception {
    configureCommands();
    processor.onDestroyed();
    receive();
    verifyNoInteractions(policy);
    verify(events).emit(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER, player, message);
  }
}
