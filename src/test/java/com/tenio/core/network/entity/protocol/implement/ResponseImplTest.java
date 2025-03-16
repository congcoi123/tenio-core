package com.tenio.core.network.entity.protocol.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@DisplayName("Unit Tests For ResponseImpl")
class ResponseImplTest {

    private Response response;
    private Session session;
    private Player player;
    private static final byte[] TEST_CONTENT = new byte[]{1, 2, 3};

    @BeforeEach
    void setUp() {
        response = ResponseImpl.newInstance();
        session = mock(Session.class);
        player = mock(Player.class);
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
        assertNotNull(response.getNonSessionRecipientPlayers());
        assertTrue(response.getNonSessionRecipientPlayers().contains(player));
    }
}

