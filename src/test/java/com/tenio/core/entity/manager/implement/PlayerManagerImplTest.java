package com.tenio.core.entity.manager.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.implement.DefaultPlayer;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.RemovedNonExistentPlayerException;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

@DisplayName("Unit Tests For PlayerManagerImpl")
class PlayerManagerImplTest {

    private PlayerManagerImpl playerManager;
    private EventManager eventManager;
    private Session session;
    private static final String PLAYER_NAME = "TestPlayer";

    @BeforeEach
    void setUp() {
        eventManager = mock(EventManager.class);
        session = mock(Session.class);
        playerManager = (PlayerManagerImpl) PlayerManagerImpl.newInstance(eventManager);
    }

    @Test
    @DisplayName("New instance should be properly initialized")
    void testNewInstance() {
        assertNotNull(playerManager);
        assertEquals(0, playerManager.getPlayerCount());
        assertTrue(playerManager.getReadonlyPlayersList().isEmpty());
    }

    @Test
    @DisplayName("Player manager should handle null event manager")
    void testNewInstanceWithNullEventManager() {
        assertThrows(NullPointerException.class, () -> PlayerManagerImpl.newInstance(null));
    }

    @Test
    @DisplayName("Player manager should handle player addition")
    void testAddPlayer() {
        Player player = DefaultPlayer.newInstance(PLAYER_NAME);
        playerManager.addPlayer(player);
        
        assertEquals(1, playerManager.getPlayerCount());
        assertTrue(playerManager.containsPlayerIdentity(PLAYER_NAME));
        assertEquals(player, playerManager.getPlayerByIdentity(PLAYER_NAME));
    }

    @Test
    @DisplayName("Player manager should handle duplicate player addition")
    void testAddDuplicatePlayer() {
        Player player1 = DefaultPlayer.newInstance(PLAYER_NAME);
        Player player2 = DefaultPlayer.newInstance(PLAYER_NAME);
        
        playerManager.addPlayer(player1);
        assertThrows(AddedDuplicatedPlayerException.class, () -> playerManager.addPlayer(player2));
    }

    @Test
    @DisplayName("Player manager should handle null player addition")
    void testAddNullPlayer() {
        assertThrows(NullPointerException.class, () -> playerManager.addPlayer(null));
    }

    @Test
    @DisplayName("Player manager should handle player removal")
    void testRemovePlayer() {
        Player player = DefaultPlayer.newInstance(PLAYER_NAME);
        playerManager.addPlayer(player);
        
        playerManager.removePlayerByIdentity(PLAYER_NAME);
        assertEquals(0, playerManager.getPlayerCount());
        assertFalse(playerManager.containsPlayerIdentity(PLAYER_NAME));
    }

    @Test
    @DisplayName("Player manager should handle non-existent player removal")
    void testRemoveNonExistentPlayer() {
        assertThrows(RemovedNonExistentPlayerException.class, 
            () -> playerManager.removePlayerByIdentity("NonExistentPlayer"));
    }

    @Test
    @DisplayName("Player manager should handle player lookup")
    void testPlayerLookup() {
        Player player = DefaultPlayer.newInstance(PLAYER_NAME);
        playerManager.addPlayer(player);
        
        assertTrue(playerManager.containsPlayerIdentity(PLAYER_NAME));
        assertEquals(player, playerManager.getPlayerByIdentity(PLAYER_NAME));
        
        List<Player> players = playerManager.getReadonlyPlayersList();
        assertEquals(1, players.size());
        assertEquals(player, players.get(0));
    }

    @Test
    @DisplayName("Player manager should handle non-existent player lookup")
    void testNonExistentPlayerLookup() {
        assertFalse(playerManager.containsPlayerIdentity("NonExistentPlayer"));
        assertNull(playerManager.getPlayerByIdentity("NonExistentPlayer"));
    }

    @Test
    @DisplayName("Player manager should handle player iteration")
    void testPlayerIteration() {
        Player player1 = DefaultPlayer.newInstance(PLAYER_NAME + "1");
        Player player2 = DefaultPlayer.newInstance(PLAYER_NAME + "2");
        
        playerManager.addPlayer(player1);
        playerManager.addPlayer(player2);
        
        @SuppressWarnings("unchecked")
        Iterator<Player> iterator = playerManager.getPlayerIterator();
        int count = 0;
        while (iterator.hasNext()) {
            Player player = iterator.next();
            assertTrue(player.equals(player1) || player.equals(player2));
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Player manager should handle clear operation")
    void testClear() {
        Player player1 = DefaultPlayer.newInstance(PLAYER_NAME + "1");
        Player player2 = DefaultPlayer.newInstance(PLAYER_NAME + "2");
        
        playerManager.addPlayer(player1);
        playerManager.addPlayer(player2);
        
        playerManager.clear();
        assertEquals(0, playerManager.getPlayerCount());
        assertTrue(playerManager.getReadonlyPlayersList().isEmpty());
    }

    @Test
    @DisplayName("Player manager should handle clear operation on empty manager")
    void testClearEmptyManager() {
        assertDoesNotThrow(() -> playerManager.clear());
        assertEquals(0, playerManager.getPlayerCount());
    }

    @Test
    @DisplayName("Player manager should handle player with session")
    void testPlayerWithSession() {
        Player player = DefaultPlayer.newInstance(PLAYER_NAME, session);
        playerManager.addPlayer(player);
        
        assertTrue(player.containsSession());
        assertEquals(session, player.getSession());
    }

    @Test
    @DisplayName("Player manager should handle player without session")
    void testPlayerWithoutSession() {
        Player player = DefaultPlayer.newInstance(PLAYER_NAME);
        playerManager.addPlayer(player);
        
        assertFalse(player.containsSession());
        assertNull(player.getSession());
    }
}

