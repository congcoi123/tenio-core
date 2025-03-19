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
import java.util.Optional;

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
        assertTrue(player.getSession().isPresent());
        assertEquals(session, player.getSession().get());
    }

    @Test
    @DisplayName("Player manager should handle player without session")
    void testPlayerWithoutSession() {
        Player player = DefaultPlayer.newInstance(PLAYER_NAME);
        playerManager.addPlayer(player);
        
        assertFalse(player.containsSession());
        assertTrue(player.getSession().isEmpty());
    }

    @Test
    @DisplayName("Player manager should handle max idle time configuration")
    void testMaxIdleTimeConfiguration() {
        int maxIdleTime = 60;
        playerManager.configureMaxIdleTimeInSeconds(maxIdleTime);
        
        Player player = playerManager.createPlayer(PLAYER_NAME);
        assertTrue(player.isActivated());
        assertTrue(player.isLoggedIn());
    }

    @Test
    @DisplayName("Player manager should handle max idle time never deported configuration")
    void testMaxIdleTimeNeverDeportedConfiguration() {
        int maxIdleTimeNeverDeported = 120;
        playerManager.configureMaxIdleTimeNeverDeportedInSeconds(maxIdleTimeNeverDeported);
        
        Player player = playerManager.createPlayer(PLAYER_NAME);
        assertTrue(player.isActivated());
        assertTrue(player.isLoggedIn());
    }

    @Test
    @DisplayName("Player manager should handle null player in configure initial player")
    void testConfigureInitialPlayerWithNull() {
        assertThrows(NullPointerException.class, () -> playerManager.configureInitialPlayer(null));
    }

    @Test
    @DisplayName("Player manager should handle null session in create player with session")
    void testCreatePlayerWithNullSession() {
        assertThrows(NullPointerException.class, 
            () -> playerManager.createPlayerWithSession(PLAYER_NAME, null));
    }

    @Test
    @DisplayName("Player manager should handle multiple players with same name")
    void testMultiplePlayersWithSameName() {
        Player player1 = playerManager.createPlayer(PLAYER_NAME);
        assertThrows(AddedDuplicatedPlayerException.class, 
            () -> playerManager.createPlayer(PLAYER_NAME));
    }

    @Test
    @DisplayName("Player manager should handle player removal and re-addition")
    void testPlayerRemovalAndReAddition() {
        Player player = playerManager.createPlayer(PLAYER_NAME);
        playerManager.removePlayerByIdentity(PLAYER_NAME);
        assertFalse(playerManager.containsPlayerIdentity(PLAYER_NAME));
        
        Player newPlayer = playerManager.createPlayer(PLAYER_NAME);
        assertTrue(playerManager.containsPlayerIdentity(PLAYER_NAME));
        assertNotEquals(player, newPlayer);
    }

    @Test
    @DisplayName("Player manager should handle player list consistency")
    void testPlayerListConsistency() {
        Player player1 = playerManager.createPlayer(PLAYER_NAME + "1");
        Player player2 = playerManager.createPlayer(PLAYER_NAME + "2");
        
        List<Player> playerList = playerManager.getReadonlyPlayersList();
        assertEquals(2, playerList.size());
        assertTrue(playerList.contains(player1));
        assertTrue(playerList.contains(player2));
        
        playerManager.removePlayerByIdentity(player1.getIdentity());
        playerList = playerManager.getReadonlyPlayersList();
        assertEquals(1, playerList.size());
        assertFalse(playerList.contains(player1));
        assertTrue(playerList.contains(player2));
    }

    @Test
    @DisplayName("Player manager should handle clear with active players")
    void testClearWithActivePlayers() {
        playerManager.createPlayer(PLAYER_NAME + "1");
        playerManager.createPlayer(PLAYER_NAME + "2");
        assertEquals(2, playerManager.getPlayerCount());
        
        playerManager.clear();
        assertEquals(0, playerManager.getPlayerCount());
        assertTrue(playerManager.getReadonlyPlayersList().isEmpty());
    }

    @Test
    @DisplayName("Player manager should handle player with session state changes")
    void testPlayerWithSessionStateChanges() {
        Player player = playerManager.createPlayerWithSession(PLAYER_NAME, session);
        assertTrue(player.containsSession());
        assertTrue(player.getSession().isPresent());
        assertEquals(session, player.getSession().get());
        
        playerManager.removePlayerByIdentity(PLAYER_NAME);
        assertFalse(playerManager.containsPlayerIdentity(PLAYER_NAME));
    }

    @Test
    @DisplayName("Player manager should maintain count consistency")
    void testPlayerCountConsistency() {
        assertEquals(0, playerManager.getPlayerCount());
        
        Player player1 = playerManager.createPlayer(PLAYER_NAME + "1");
        assertEquals(1, playerManager.getPlayerCount());
        
        Player player2 = playerManager.createPlayer(PLAYER_NAME + "2");
        assertEquals(2, playerManager.getPlayerCount());
        
        playerManager.removePlayerByIdentity(player1.getIdentity());
        assertEquals(1, playerManager.getPlayerCount());
        
        playerManager.clear();
        assertEquals(0, playerManager.getPlayerCount());
  }
}

