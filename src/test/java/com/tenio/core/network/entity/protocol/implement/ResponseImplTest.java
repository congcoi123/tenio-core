package com.tenio.core.network.entity.protocol.implement;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataType;
import com.tenio.common.data.DataUtility;
import com.tenio.core.entity.Player;
import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.server.ServerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Unit Tests For ResponseImpl")
class ResponseImplTest {

    private Response response;
    private Session session;
    private Player player;
    private ServerImpl serverImpl;
    private DataCollection dataCollection;
    private MockedStatic<ServerImpl> mockedServer;
    private static final byte[] TEST_CONTENT = new byte[]{1, 2, 3};

    @BeforeEach
    void setUp() {
        session = mock(Session.class);
        player = mock(Player.class);
        serverImpl = mock(ServerImpl.class);
        dataCollection = mock(DataCollection.class);
        
        mockedServer = mockStatic(ServerImpl.class);
        mockedServer.when(ServerImpl::getInstance).thenReturn(serverImpl);
        when(serverImpl.getDataType()).thenReturn(DataType.MSG_PACK);
        doNothing().when(serverImpl).write(any(Response.class), anyBoolean());
        response = ResponseImpl.newInstance();
        
        // Default session setup
        when(session.isTcp()).thenReturn(true);
        when(session.isWebSocket()).thenReturn(false);
        when(session.containsUdp()).thenReturn(false);
        when(session.containsKcp()).thenReturn(false);
        
        // Default player setup
        when(player.containsSession()).thenReturn(false);
        when(player.getSession()).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        if (mockedServer != null) {
            mockedServer.close();
        }
    }

  @Test
    @DisplayName("New instance should be properly initialized")
  void testNewInstance() {
        assertNotNull(response);
        assertEquals(ResponsePriority.NORMAL, response.getPriority());
        assertNull(response.getContent());
        assertFalse(response.isEncrypted());
    }

    @Test
    @DisplayName("Response should handle content setting")
    void testContent() {
        response.setContent(TEST_CONTENT);
        assertArrayEquals(TEST_CONTENT, response.getContent());
    }

    @Test
    @DisplayName("Response should handle priority setting")
    void testPriority() {
        response.priority(ResponsePriority.GUARANTEED_QUICKEST);
        assertEquals(ResponsePriority.GUARANTEED_QUICKEST, response.getPriority());
    }

    @Test
    @DisplayName("Response should handle encryption flag")
    void testEncryption() {
        response.encrypted();
        assertTrue(response.isEncrypted());
    }

    @Test
    @DisplayName("Response should handle recipient players")
    void testRecipientPlayers() {
        Collection<Player> players = new ArrayList<>();
        players.add(player);
        
        response.setRecipientPlayers(players);
        assertEquals(players, response.getRecipientPlayers());
    }

    @Test
    @DisplayName("Response should handle single recipient player")
    void testSingleRecipientPlayer() {
        response.setRecipientPlayer(player);
        Collection<Player> players = response.getRecipientPlayers();
        assertEquals(1, players.size());
        assertTrue(players.contains(player));
    }

    @Test
    @DisplayName("Response should handle recipient sessions")
    void testRecipientSessions() {
        Collection<Session> sessions = new ArrayList<>();
        sessions.add(session);
        
        response.setRecipientSessions(sessions);
        assertNotNull(response.getRecipientSocketSessions());
        assertTrue(response.getRecipientSocketSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle single recipient session")
    void testSingleRecipientSession() {
        response.setRecipientSession(session);
        assertNotNull(response.getRecipientSocketSessions());
        assertTrue(response.getRecipientSocketSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle UDP prioritization")
    void testUdpPrioritization() {
        when(session.containsUdp()).thenReturn(true);
        response.prioritizedUdp().setRecipientSession(session);
        assertNotNull(response.getRecipientDatagramSessions());
        assertTrue(response.getRecipientDatagramSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle KCP prioritization")
    void testKcpPrioritization() {
        when(session.containsKcp()).thenReturn(true);
        response.prioritizedKcp().setRecipientSession(session);
        assertNotNull(response.getRecipientKcpSessions());
        assertTrue(response.getRecipientKcpSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle WebSocket sessions")
    void testWebSocketSessions() {
        when(session.isTcp()).thenReturn(false);
        when(session.isWebSocket()).thenReturn(true);
        response.setRecipientSession(session);
        assertNotNull(response.getRecipientWebSocketSessions());
        assertTrue(response.getRecipientWebSocketSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle null content")
    void testNullContent() {
        response.setContent(null);
        assertNull(response.getContent());
    }

    @Test
    @DisplayName("Response should handle empty recipients")
    void testEmptyRecipients() {
        response.setRecipientPlayers(new ArrayList<>());
        assertTrue(response.getRecipientPlayers().isEmpty());
    }

    @Test
    @DisplayName("Response should handle null recipients")
    void testNullRecipients() {
        response.setRecipientPlayers(null);
        assertNull(response.getRecipientPlayers());
    }

    @Test
    @DisplayName("Response should handle non-session players")
    void testNonSessionPlayers() {
        when(player.containsSession()).thenReturn(false);
        response.setRecipientPlayer(player);
        response.write();
        assertEquals(1, response.getNonSessionRecipientPlayers().size());
        assertTrue(response.getNonSessionRecipientPlayers().contains(player));
    }

    @Test
    @DisplayName("Response should handle player with session")
    void testPlayerWithSession() {
        when(player.containsSession()).thenReturn(true);
        when(player.getSession()).thenReturn(Optional.of(session));
        response.setRecipientPlayer(player);
        response.write();
        assertEquals(1, response.getRecipientSocketSessions().size());
        assertTrue(response.getRecipientSocketSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle multiple transport priorities")
    void testMultipleTransportPriorities() {
        when(session.containsKcp()).thenReturn(true);
        
        response.prioritizedUdp();
        response.prioritizedKcp();
        response.setRecipientSession(session);
        
        assertTrue(response.getRecipientKcpSessions().contains(session));
        assertFalse(response.getRecipientSocketSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle mixed session types")
    void testMixedSessionTypes() {
        Session tcpSession = mock(Session.class);
        Session webSocketSession = mock(Session.class);
        
        when(tcpSession.isTcp()).thenReturn(true);
        when(webSocketSession.isWebSocket()).thenReturn(true);
        when(webSocketSession.isTcp()).thenReturn(false);

        Collection<Session> sessions = new ArrayList<>();
        sessions.add(tcpSession);
        sessions.add(webSocketSession);

        response.setRecipientSessions(sessions);

        assertTrue(response.getRecipientSocketSessions().contains(tcpSession));
        assertTrue(response.getRecipientWebSocketSessions().contains(webSocketSession));
    }

    @Test
    @DisplayName("Response should handle null session in player")
    void testNullSessionInPlayer() {
        when(player.containsSession()).thenReturn(true);
        when(player.getSession()).thenReturn(Optional.empty());
        
        response.setRecipientPlayer(player);
        response.write();
        
        assertTrue(response.getRecipientSocketSessions().isEmpty());
    }

    @Test
    @DisplayName("Response should handle write then close")
    void testWriteThenClose() {
        response.setContent(TEST_CONTENT);
        response.setRecipientSession(session);
        response.writeThenClose();
        
        assertTrue(response.getRecipientSocketSessions().contains(session));
    }

    @Test
    @DisplayName("Response should handle multiple players with mixed session states")
    void testMultiplePlayersWithMixedSessionStates() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        Session session2 = mock(Session.class);

        when(player1.containsSession()).thenReturn(false);
        when(player2.containsSession()).thenReturn(true);
        when(player2.getSession()).thenReturn(Optional.of(session2));
        when(session2.isTcp()).thenReturn(true);

        Collection<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);

        response.setRecipientPlayers(players);
        response.write();

        assertEquals(1, response.getNonSessionRecipientPlayers().size());
        assertEquals(1, response.getRecipientSocketSessions().size());
  }
}

