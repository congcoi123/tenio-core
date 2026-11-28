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

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.zero.utility.ZeroUtility;
import com.tenio.core.api.ServerApi;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.inbound.implement.SessionRequest;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.zero.engine.manager.DatagramChannelManager;
import com.tenio.core.server.core.ZeroProcessorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecoveryProcessorTest {

    @ParameterizedTest
    @EnumSource(
            value = TransportType.class,
            names = {"TCP", "WEB_SOCKET"}
    )
    void tcpAndWebSocketUseTheSameValidatedRecoveryAndEventOrder(TransportType transport) {
        var events = mock(EventManager.class);
        var api = mock(ServerApi.class);
        var player = RecoveryManagerTest.player("alice");
        var session = mock(Session.class);
        when(player.getSession()).thenReturn(Optional.empty());
        when(session.isActivated()).thenReturn(true);
        when(session.getTransportType()).thenReturn(transport);
        when(session.transitionAssociatedState(Session.AssociatedState.NONE, Session.AssociatedState.DOING))
                .thenReturn(true);
        when(session.associatePlayer(player)).thenReturn(true);
        var order = new ArrayList<String>();
        var token = new String[1];
        var policy = new RecoveryManagerTest.Policy() {
                    {
                        selected = Strategy.SNAPSHOT;
                    }

                    @Override
                    public Credentials credentials(Session s, DataCollection message) {
                        assertEquals(transport, s.getTransportType());
                        return new Credentials("alice", token[0], 0);
                    }

                    @Override
                    public void resumed(Player p, Session s, RecoveryManager.Result result) {
                        assertSame(player, p);
                        assertSame(session, s);
                        assertNotNull(result.snapshot());
                        order.add("snapshot");
                    }
                };
        try (var manager = new RecoveryManager(policy, ignored -> {})) {
            token[0] = manager.issue(player);
            var processor = ZeroProcessorImpl.newInstance(events, api, mock(DatagramChannelManager.class));
            processor.setSessionManager(mock(SessionManager.class));
            processor.configureRecovery(manager, policy);
            doAnswer(call -> {
                        order.add("resumed");
                        return null;
                    })
                    .when(events)
                    .emit(eq(ServerEvent.PLAYER_CONNECTION_RESUMED), same(player), same(session));
            var request = SessionRequest.newInstance();
            request.setEvent(ServerEvent.SESSION_REQUEST_CONNECTION);
            request.setSender(session);
            request.setMessage(ZeroUtility.newZeroMap());
            processor.processRequest(request);
            assertEquals(List.of("snapshot", "resumed"), order);
            assertFalse(manager.isRecovering(player));
            verify(events, never()).emit(eq(ServerEvent.PLAYER_CONNECTION_RETRY), any(), any());
        }
    }

    @Test
    void invalidRecoveryCannotFallBackToNormalLoginOrLegacyRetry() throws Exception {
        var events = mock(EventManager.class);
        var api = mock(ServerApi.class);
        var session = mock(Session.class);
        when(session.isActivated()).thenReturn(true);
        when(session.transitionAssociatedState(Session.AssociatedState.NONE, Session.AssociatedState.DOING))
                .thenReturn(true);
        var policy = new RecoveryManagerTest.Policy() {
                    @Override
                    public Credentials credentials(Session s, DataCollection message) {
                        return new Credentials("alice", "bad", 0);
                    }
                };
        try (var manager = new RecoveryManager(policy, ignored -> {})) {
            var processor = ZeroProcessorImpl.newInstance(events, api, mock(DatagramChannelManager.class));
            processor.configureRecovery(manager, policy);
            var request = SessionRequest.newInstance();
            request.setEvent(ServerEvent.SESSION_REQUEST_CONNECTION);
            request.setSender(session);
            request.setMessage(ZeroUtility.newZeroMap());
            processor.processRequest(request);
            verify(session).close(ConnectionDisconnectMode.REFUSED_CONNECTION, PlayerDisconnectMode.CONNECTION_LOST);
            verifyNoInteractions(events);
        }
    }
}
