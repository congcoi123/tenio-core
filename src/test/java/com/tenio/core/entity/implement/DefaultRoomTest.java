package com.tenio.core.entity.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.RoomState;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.setting.strategy.RoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import com.tenio.core.exception.PlayerJoinedRoomException;
import com.tenio.core.exception.RemovedNonExistentPlayerException;
import com.tenio.core.exception.SwitchedPlayerRoleInRoomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Iterator;
import java.util.Optional;

/**
 * Unit tests for the DefaultRoom class.
 */
@DisplayName("Unit Tests For DefaultRoom")
class DefaultRoomTest {

    private DefaultRoom room;
    private PlayerManager playerManager;
    private RoomCredentialValidatedStrategy credentialStrategy;
    private RoomPlayerSlotGeneratedStrategy slotStrategy;
    private static final String ROOM_NAME = "TestRoom";
    private static final String ROOM_PASSWORD = "password123";
    private static final int MAX_PARTICIPANTS = 10;
    private static final int MAX_SPECTATORS = 5;

    @BeforeEach
    void setUp() {
        room = (DefaultRoom) DefaultRoom.newInstance();
        playerManager = mock(PlayerManager.class);
        credentialStrategy = mock(RoomCredentialValidatedStrategy.class);
        slotStrategy = mock(RoomPlayerSlotGeneratedStrategy.class);
        
        room.configurePlayerManager(playerManager);
        room.configureRoomCredentialValidatedStrategy(credentialStrategy);
        room.configurePlayerSlotGeneratedStrategy(slotStrategy);
        
        room.setName(ROOM_NAME);
        room.setMaxParticipants(MAX_PARTICIPANTS);
        room.setMaxSpectators(MAX_SPECTATORS);
        room.setRoomRemoveMode(RoomRemoveMode.WHEN_EMPTY);
    }

  @Test
    @DisplayName("New instance should be properly initialized")
  void testNewInstance() {
        assertTrue(room.getId() > 0);
        assertEquals(ROOM_NAME, room.getName());
        assertEquals(MAX_PARTICIPANTS, room.getMaxParticipants());
        assertEquals(MAX_SPECTATORS, room.getMaxSpectators());
        assertEquals(RoomRemoveMode.WHEN_EMPTY, room.getRoomRemoveMode());
        assertTrue(room.isEmpty());
        assertFalse(room.isFull());
        assertTrue(room.isPublic());
    }

    @Test
    @DisplayName("Room should handle state transitions correctly")
    void testStateTransitions() {
        RoomState mockState = mock(RoomState.class);
        room.setState(mockState);
        assertEquals(mockState, room.getState());
        assertTrue(room.isState(mockState));
        
        RoomState newState = mock(RoomState.class);
        assertTrue(room.transitionState(mockState, newState));
        assertEquals(newState, room.getState());
        assertFalse(room.transitionState(mockState, newState)); // Should fail as current state is different
    }

    @Test
    @DisplayName("Room should handle property management correctly")
    void testPropertyManagement() {
        String key = "testKey";
        String value = "testValue";
        
        assertFalse(room.containsProperty(key));
        room.setProperty(key, value);
        assertTrue(room.containsProperty(key));
        assertEquals(value, room.getProperty(key));
        
        room.removeProperty(key);
        assertFalse(room.containsProperty(key));
        assertNull(room.getProperty(key));
        
        // Test multiple properties
        room.setProperty("key1", "value1");
        room.setProperty("key2", "value2");
        room.clearProperties();
        assertFalse(room.containsProperty("key1"));
        assertFalse(room.containsProperty("key2"));
    }

    @Test
    @DisplayName("Room should handle password protection correctly")
    void testPasswordProtection() {
        room.setPassword(ROOM_PASSWORD);
        assertFalse(room.isPublic());
        assertEquals(ROOM_PASSWORD, room.getPassword());
        
        Player player = DefaultPlayer.newInstance("TestPlayer");
        assertThrows(PlayerJoinedRoomException.class, () -> 
            room.addPlayer(player, "wrongPassword", false, Room.DEFAULT_SLOT));
        
        // Should not throw exception with correct password
        assertDoesNotThrow(() -> 
            room.addPlayer(player, ROOM_PASSWORD, false, Room.DEFAULT_SLOT));
    }

    @Test
    @DisplayName("Room should handle player capacity correctly")
    void testPlayerCapacity() {
        assertEquals(MAX_PARTICIPANTS + MAX_SPECTATORS, room.getCapacity());
        
        // Add maximum participants
        for (int i = 0; i < MAX_PARTICIPANTS; i++) {
            Player player = DefaultPlayer.newInstance("Player" + i);
            room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        }
        
        // Adding one more participant should throw exception
        Player extraPlayer = DefaultPlayer.newInstance("ExtraPlayer");
        assertThrows(PlayerJoinedRoomException.class, () ->
            room.addPlayer(extraPlayer, null, false, Room.DEFAULT_SLOT));
        
        // But should be able to add a spectator
        assertDoesNotThrow(() ->
            room.addPlayer(extraPlayer, null, true, Room.NIL_SLOT));
    }

    @Test
    @DisplayName("Room should handle player role switching correctly")
    void testPlayerRoleSwitching() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        room.addPlayer(player, null, true, Room.NIL_SLOT); // Add as spectator
        assertEquals(PlayerRoleInRoom.SPECTATOR, player.getRoleInRoom());
        
        room.switchSpectatorToParticipant(player, Room.DEFAULT_SLOT);
        assertEquals(PlayerRoleInRoom.PARTICIPANT, player.getRoleInRoom());
        
        room.switchParticipantToSpectator(player);
        assertEquals(PlayerRoleInRoom.SPECTATOR, player.getRoleInRoom());
    }

    @Test
    @DisplayName("Room should handle player removal correctly")
    void testPlayerRemoval() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        
        assertTrue(room.containsPlayerIdentity(player.getIdentity()));
        assertEquals(1, room.getPlayerCount());
        
        room.removePlayer(player);
        assertFalse(room.containsPlayerIdentity(player.getIdentity()));
        assertEquals(0, room.getPlayerCount());
        assertTrue(room.isEmpty());
    }

    @Test
    @DisplayName("Room should handle owner management correctly")
    void testOwnerManagement() {
        Player owner = DefaultPlayer.newInstance("Owner");
        room.addPlayer(owner, null, false, Room.DEFAULT_SLOT);
        room.setOwner(owner);
        
        assertTrue(room.getOwner().isPresent());
        assertEquals(owner, room.getOwner().get());
        
        room.removePlayer(owner);
        assertFalse(room.getOwner().isPresent());
    }

    @Test
    @DisplayName("Room should provide correct player lists")
    void testPlayerLists() {
        Player participant = DefaultPlayer.newInstance("Participant");
        Player spectator = DefaultPlayer.newInstance("Spectator");
        
        room.addPlayer(participant, null, false, Room.DEFAULT_SLOT);
        room.addPlayer(spectator, null, true, Room.NIL_SLOT);
        
        List<Player> participants = room.getReadonlyParticipantsList();
        List<Player> spectators = room.getReadonlySpectatorsList();
        
        assertEquals(1, participants.size());
        assertEquals(1, spectators.size());
        assertEquals(participant, participants.get(0));
        assertEquals(spectator, spectators.get(0));
    }

    @Test
    @DisplayName("Room should handle activation state changes correctly")
    void testActivationState() {
        assertFalse(room.isActivated());
        room.setActivated(true);
        assertTrue(room.isActivated());
        room.setActivated(false);
        assertFalse(room.isActivated());
    }

    @Test
    @DisplayName("Room should validate capacity with negative values")
    void testCapacityValidation() {
        assertThrows(IllegalArgumentException.class, () -> room.setMaxParticipants(-1));
        assertThrows(IllegalArgumentException.class, () -> room.setMaxSpectators(-1));
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(5, -1));
        assertThrows(IllegalArgumentException.class, () -> room.setCapacity(-1, -1));
    }

    @Test
    @DisplayName("Room should handle null values in credential validation")
    void testNullCredentialValidation() {
        doNothing().when(credentialStrategy).validateName(null);
        doNothing().when(credentialStrategy).validatePassword(null);
        
        assertDoesNotThrow(() -> room.setName(null));
        assertDoesNotThrow(() -> room.setPassword(null));
        
        verify(credentialStrategy).validateName(null);
        verify(credentialStrategy).validatePassword(null);
    }

    @Test
    @DisplayName("Room should handle player lookup correctly")
    void testPlayerLookup() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        
        Optional<Player> foundPlayer = room.getPlayerByIdentity(player.getIdentity());
        assertTrue(foundPlayer.isPresent());
        assertEquals(player, foundPlayer.get());
        
        Optional<Player> notFoundPlayer = room.getPlayerByIdentity("NonExistentPlayer");
        assertFalse(notFoundPlayer.isPresent());
    }

    @Test
    @DisplayName("Room should handle player iterator correctly")
    void testPlayerIterator() {
        Player player1 = DefaultPlayer.newInstance("Player1");
        Player player2 = DefaultPlayer.newInstance("Player2");
        room.addPlayer(player1, null, false, Room.DEFAULT_SLOT);
        room.addPlayer(player2, null, false, Room.DEFAULT_SLOT);
        
        int count = 0;
        @SuppressWarnings("unchecked")
        Iterator<Player> iterator = room.getPlayerIterator();
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Room should handle invalid role switches")
    void testInvalidRoleSwitches() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT); // Add as participant
        
        // Try to switch participant to participant (invalid)
        assertThrows(SwitchedPlayerRoleInRoomException.class, () ->
            room.switchSpectatorToParticipant(player, Room.DEFAULT_SLOT));
        
        room.switchParticipantToSpectator(player);
        
        // Try to switch spectator to spectator (invalid)
        assertThrows(SwitchedPlayerRoleInRoomException.class, () ->
            room.switchParticipantToSpectator(player));
    }

    @Test
    @DisplayName("Room should handle non-existent player removal")
    void testNonExistentPlayerRemoval() {
        Player player = DefaultPlayer.newInstance("NonExistentPlayer");
        assertThrows(RemovedNonExistentPlayerException.class, () ->
            room.removePlayer(player));
    }

    @Test
    @DisplayName("Room should handle clear operation correctly")
    void testClear() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        room.setProperty("test", "value");
        
        assertThrows(UnsupportedOperationException.class, () -> room.clear());
    }

    @Test
    @DisplayName("Room should handle null state transitions")
    void testNullStateTransitions() {
        room.setState(null);
        assertNull(room.getState());
        assertTrue(room.isState(null));
        
        RoomState newState = mock(RoomState.class);
        assertTrue(room.transitionState(null, newState));
        assertEquals(newState, room.getState());
    }

    @Test
    @DisplayName("Room should handle capacity updates correctly")
    void testCapacityUpdates() {
        room.setCapacity(20, 10);
        assertEquals(20, room.getMaxParticipants());
        assertEquals(10, room.getMaxSpectators());
        assertEquals(30, room.getCapacity());
        
        // Test individual updates
        room.setMaxParticipants(15);
        room.setMaxSpectators(8);
        assertEquals(15, room.getMaxParticipants());
        assertEquals(8, room.getMaxSpectators());
        assertEquals(23, room.getCapacity());
    }

    @Test
    @DisplayName("Room should handle player slot management correctly")
    void testPlayerSlotManagement() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        
        // Verify slot strategy interaction
        verify(slotStrategy).tryTakeSlot(Room.DEFAULT_SLOT);
        
        room.removePlayer(player);
        verify(slotStrategy).freeSlotWhenPlayerLeft(Room.DEFAULT_SLOT);
  }
}

