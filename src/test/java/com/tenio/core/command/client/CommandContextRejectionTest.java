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

package com.tenio.core.command.client;

import com.tenio.common.data.DataCollection;
import com.tenio.core.api.ServerApi;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.zero.engine.manager.DatagramChannelManager;
import com.tenio.core.server.core.ZeroProcessorImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommandContextRejectionTest {

    @ParameterizedTest
    @ValueSource(shorts = {41, 42})
    void missingContextIsRejectedBeforeCopyingOrDispatch(short code) {
        var manager = new ClientCommandManager();
        var handler = code == 41 ? new SessionCommand() : new RoomCommand();
        var player = mock(Player.class);
        var message = mock(DataCollection.class);
        try {
            manager.registerCommand(code, handler);
            for (int i = 0; i < 20; i++) {
                assertDoesNotThrow(() -> manager.invoke(code, player, message));
                assertDoesNotThrow(() -> manager.invoke(code, null, message));
            }
            assertEquals(0, handler.calls.get());
            assertEquals(0, manager.outstandingCommands());
            verifyNoInteractions(message);
        } finally {
            manager.clear();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void automaticRoutingRejectsRoomCommandsBeforeJoiningWithoutFallingBackToEvents(boolean monitoring) throws Exception {
        var manager = new ClientCommandManager();
        var handler = new RoomCommand();
        var player = mock(Player.class);
        var session = mock(Session.class);
        var players = mock(PlayerManager.class);
        var events = mock(EventManager.class);
        var message = mock(DataCollection.class);
        when(player.getSession()).thenReturn(Optional.of(session));
        when(session.isAssociatedToPlayer(Session.AssociatedState.DONE)).thenReturn(true);
        when(session.getName()).thenReturn("alice");
        when(players.getPlayerByIdentity("alice")).thenReturn(player);
        var processor = ZeroProcessorImpl.newInstance(events, mock(ServerApi.class), mock(DatagramChannelManager.class));
        processor.setPlayerManager(players);
        try {
            manager.registerCommand((short) 42, handler);
            if (monitoring) {
                manager.enableExecutionMonitoring();
            }
            processor.configureClientCommands(manager, input -> new ClientCommandPolicy.CommandRequest((short) 42, input));
            var receive = ZeroProcessorImpl.class.getDeclaredMethod("processSessionReadMessage", Session.class, DataCollection.class);
            receive.setAccessible(true);
            for (int i = 0; i < 20; i++) {
                assertDoesNotThrow(() -> receive.invoke(processor, session, message));
            }
            assertEquals(0, handler.calls.get());
            assertEquals(0, manager.outstandingCommands());
            verifyNoInteractions(message, events);
        } finally {
            manager.clear();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void genuineInlineHandlerFailuresStillPropagate(boolean monitoring) {
        var manager = new ClientCommandManager();
        try {
            manager.registerCommand((short) 43, new FailingCommand());
            if (monitoring) {
                manager.enableExecutionMonitoring();
            }
            assertThrows(IllegalStateException.class, () -> manager.invoke((short) 43, null, null));
        } finally {
            manager.clear();
        }
    }

    @ClientCommand(value = 41, execution = ExecutionMode.SESSION)
    static class SessionCommand extends AbstractClientCommandHandler<Player, DataCollection> {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public void execute(Player player, DataCollection message) {
            calls.incrementAndGet();
        }
    }

    @ClientCommand(value = 42, execution = ExecutionMode.ROOM)
    static class RoomCommand extends SessionCommand {
    }

    @ClientCommand(value = 43)
    static class FailingCommand extends AbstractClientCommandHandler<Player, DataCollection> {
        @Override
        public void execute(Player player, DataCollection message) {
            throw new IllegalStateException("application failure");
        }
    }
}
