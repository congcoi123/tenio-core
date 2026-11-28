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

package com.tenio.core.recovery;

import com.tenio.core.api.ServerApi;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.implement.SessionImpl;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.zero.engine.manager.DatagramChannelManager;
import com.tenio.core.server.core.ZeroProcessorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecoveryRetentionTest {

    @ParameterizedTest
    @CsvSource({
            "false,false,LOST_IN_READ,false", "false,true,LOST_IN_READ,true",
            "false,false,LOST_IN_WRITTEN,false", "false,true,LOST_IN_WRITTEN,true",
            "false,false,EXCEPTION,false", "false,true,EXCEPTION,true",
            "false,true,CLIENT_REQUEST,false", "false,true,IDLE,false",
            "true,false,LOST_IN_READ,true", "true,true,LOST_IN_READ,true",
            "true,false,LOST_IN_WRITTEN,true", "true,true,LOST_IN_WRITTEN,true",
            "true,false,EXCEPTION,true", "true,true,EXCEPTION,true",
            "true,true,CLIENT_REQUEST,false", "true,true,IDLE,false",
            "true,true,KICK,false", "true,true,BAN,false", "true,true,SERVER_DOWN,false"
    })
    void enrollmentSelectsRecoveryOrLegacyRetention(boolean enrolled, boolean legacyKeep,
                                                    ConnectionDisconnectMode reason, boolean retained) throws Exception {
        var player = RecoveryManagerTest.player("alice");
        var session = mock(Session.class);
        var players = mock(PlayerManager.class);
        var api = mock(ServerApi.class);
        when(session.getName()).thenReturn("alice");
        when(session.beginPlayerAssociationClose()).thenReturn(true);
        when(player.getSession()).thenReturn(Optional.of(session));
        when(players.getPlayerByIdentity("alice")).thenReturn(player);
        var policy = new RecoveryManagerTest.Policy();
        try (var manager = new RecoveryManager(policy, ignored -> {})) {
            String token = enrolled ? manager.issue(player) : null;
            var processor = ZeroProcessorImpl.newInstance(mock(EventManager.class), api, mock(DatagramChannelManager.class));
            processor.setPlayerManager(players);
            processor.setKeepPlayerOnDisconnection(legacyKeep);
            processor.configureRecovery(manager, policy);
            var method = ZeroProcessorImpl.class.getDeclaredMethod("processSessionWillBeClosed",
                    Session.class, ConnectionDisconnectMode.class, PlayerDisconnectMode.class);
            method.setAccessible(true);
            var playerReason = reason == ConnectionDisconnectMode.CLIENT_REQUEST
                    ? PlayerDisconnectMode.CLIENT_REQUEST
                    : reason == ConnectionDisconnectMode.IDLE ? PlayerDisconnectMode.IDLE : PlayerDisconnectMode.CONNECTION_LOST;
            method.invoke(processor, session, reason, playerReason);
            verify(player).setSession(null);
            verify(session).remove();
            if (retained) {
                verify(player, never()).clean();
                verify(players, never()).removePlayerByIdentity(any());
                verify(api, never()).unsubscribeFromAllChannels(any());
            } else {
                verify(player).clean();
                verify(players).removePlayerByIdentity("alice");
                verify(api).unsubscribeFromAllChannels(player);
                if (enrolled) {
                    assertNull(manager.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
                }
            }
        }
    }

    @Test
    void replacementFenceStillPreventsCleanupOfThePlayerBeingHandedOver() throws Exception {
        var player = RecoveryManagerTest.player("alice");
        var session = SessionImpl.newInstance();
        session.configureSessionManager(mock(SessionManager.class));
        session.setAssociatedToPlayer(Session.AssociatedState.DONE);
        session.prepareForReplacement();
        assertFalse(session.beginPlayerAssociationClose());
        var players = mock(PlayerManager.class);
        var api = mock(ServerApi.class);
        var policy = new RecoveryManagerTest.Policy();
        try (var manager = new RecoveryManager(policy, ignored -> {})) {
            manager.issue(player);
            var processor = ZeroProcessorImpl.newInstance(mock(EventManager.class), api, mock(DatagramChannelManager.class));
            processor.setPlayerManager(players);
            processor.configureRecovery(manager, policy);
            var method = ZeroProcessorImpl.class.getDeclaredMethod("processSessionWillBeClosed",
                    Session.class, ConnectionDisconnectMode.class, PlayerDisconnectMode.class);
            method.setAccessible(true);
            method.invoke(processor, session, ConnectionDisconnectMode.RECONNECTION, PlayerDisconnectMode.RECONNECTION);
            verify(player, never()).clean();
            verify(players, never()).getPlayerByIdentity(any());
            verify(api, never()).unsubscribeFromAllChannels(any());
        }
    }

    @Test
    void aClosedManagerDoesNotFallBackToRetainingAnEnrolledPlayer() {
        var player = RecoveryManagerTest.player("alice");
        var manager = new RecoveryManager(new RecoveryManagerTest.Policy(), ignored -> {});
        manager.issue(player);
        manager.close();
        assertFalse(manager.disconnected(player, ConnectionDisconnectMode.EXCEPTION, true));
    }
}
