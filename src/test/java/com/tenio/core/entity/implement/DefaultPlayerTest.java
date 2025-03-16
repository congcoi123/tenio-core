package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.PlayerState;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Unit tests for the DefaultPlayer class.
 */
@DisplayName("Unit Tests For DefaultPlayer")
class DefaultPlayerTest {

    private DefaultPlayer player;
    private static final String PLAYER_NAME = "TestPlayer";
    private static final int MAX_IDLE_TIME = 60;
    private static final int MAX_IDLE_TIME_NEVER_DEPORTED = 300;

    @BeforeEach
    void setUp() {
        player = (DefaultPlayer) DefaultPlayer.newInstance(PLAYER_NAME);
        player.configureMaxIdleTimeInSeconds(MAX_IDLE_TIME);
        player.configureMaxIdleTimeNeverDeportedInSeconds(MAX_IDLE_TIME_NEVER_DEPORTED);
    }

  @Test
    @DisplayName("New instance should be properly initialized")
  void testNewInstance() {
        assertEquals(PLAYER_NAME, player.getIdentity());
        assertFalse(player.isLoggedIn());
        assertFalse(player.isActivated());
        assertFalse(player.isNeverDeported());
        assertFalse(player.containsSession());
        assertEquals(PlayerRoleInRoom.SPECTATOR, player.getRoleInRoom());
    }

    @Test
    @DisplayName("Player should handle session management correctly")
    void testSessionManagement() {
        Session session = mock(Session.class);
        player.setSession(session);
        
        assertTrue(player.containsSession());
        assertTrue(player.getSession().isPresent());
        assertEquals(session, player.getSession().get());
        
        player.setSession(null);
        assertFalse(player.containsSession());
        assertTrue(player.getSession().isEmpty());
    }

    @Test
    @DisplayName("Player should handle state transitions correctly")
    void testStateTransitions() {
        PlayerState mockState = mock(PlayerState.class);
        player.setState(mockState);
        assertEquals(mockState, player.getState());
        assertTrue(player.isState(mockState));
        
        PlayerState newState = mock(PlayerState.class);
        assertTrue(player.transitionState(mockState, newState));
        assertEquals(newState, player.getState());
        assertFalse(player.transitionState(mockState, newState)); // Should fail as current state is different
    }

    @Test
    @DisplayName("Player should handle role transitions correctly")
    void testRoleTransitions() {
        assertEquals(PlayerRoleInRoom.SPECTATOR, player.getRoleInRoom());
        
        player.setRoleInRoom(PlayerRoleInRoom.PARTICIPANT);
        assertEquals(PlayerRoleInRoom.PARTICIPANT, player.getRoleInRoom());
        
        assertTrue(player.transitionRole(PlayerRoleInRoom.PARTICIPANT, PlayerRoleInRoom.SPECTATOR));
        assertEquals(PlayerRoleInRoom.SPECTATOR, player.getRoleInRoom());
        assertFalse(player.transitionRole(PlayerRoleInRoom.PARTICIPANT, PlayerRoleInRoom.SPECTATOR)); // Should fail
    }

    @Test
    @DisplayName("Player should handle property management correctly")
    void testPropertyManagement() {
        String key = "testKey";
        String value = "testValue";
        
        assertFalse(player.containsProperty(key));
        player.setProperty(key, value);
        assertTrue(player.containsProperty(key));
        assertEquals(value, player.getProperty(key));
        
        player.removeProperty(key);
        assertFalse(player.containsProperty(key));
        assertNull(player.getProperty(key));
        
        // Test multiple properties
        player.setProperty("key1", "value1");
        player.setProperty("key2", "value2");
        player.clearProperties();
        assertFalse(player.containsProperty("key1"));
        assertFalse(player.containsProperty("key2"));
    }

    @Test
    @DisplayName("Player should handle activity tracking correctly")
    void testActivityTracking() {
        long currentTime = System.currentTimeMillis();
        player.setLastReadTime(currentTime);
        assertEquals(currentTime, player.getLastReadTime());
        assertEquals(currentTime, player.getLastActivityTime());
        
        long newTime = currentTime + 1000;
        player.setLastWriteTime(newTime);
        assertEquals(newTime, player.getLastWriteTime());
        assertEquals(newTime, player.getLastActivityTime());
    }

    @Test
    @DisplayName("Player should handle idle state correctly")
    void testIdleState() {
        player.setLastReadTime(System.currentTimeMillis() - (MAX_IDLE_TIME + 1) * 1000L);
        assertTrue(player.isIdle());
        
        player.setLastReadTime(System.currentTimeMillis());
        assertFalse(player.isIdle());
    }

    @Test
    @DisplayName("Player should handle never deported state correctly")
    void testNeverDeportedState() {
        player.setNeverDeported(true);
        assertTrue(player.isNeverDeported());
        
        player.setLastReadTime(System.currentTimeMillis() - (MAX_IDLE_TIME_NEVER_DEPORTED + 1) * 1000L);
        assertTrue(player.isIdleNeverDeported());
        
        player.setLastReadTime(System.currentTimeMillis());
        assertFalse(player.isIdleNeverDeported());
  }

  @Test
    @DisplayName("Player should handle update notifications correctly")
    void testUpdateNotifications() {
        Consumer<Player.Field> mockConsumer = mock(Consumer.class);
        player.onUpdateListener(mockConsumer);
        
        player.setActivated(true);
        verify(mockConsumer).accept(Player.Field.ACTIVATION);
        
        player.setRoleInRoom(PlayerRoleInRoom.PARTICIPANT);
        verify(mockConsumer).accept(Player.Field.ROLE_IN_ROOM);
        
        player.setProperty("test", "value");
        verify(mockConsumer).accept(Player.Field.PROPERTY);
  }

  @Test
    @DisplayName("Player should handle cleanup correctly")
    void testCleanup() {
        Session session = mock(Session.class);
        player.setSession(session);
        player.setActivated(true);
        player.setProperty("test", "value");
        
        player.clean();
        
        assertFalse(player.isActivated());
        assertFalse(player.containsSession());
        assertFalse(player.containsProperty("test"));
  }
}

