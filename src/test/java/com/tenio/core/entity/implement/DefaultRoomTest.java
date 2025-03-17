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
import java.util.ArrayList;
import java.util.Arrays;

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

        // Default mock behavior
        when(playerManager.getPlayerCount()).thenReturn(0);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.<Player>of());
        when(playerManager.getPlayerIterator()).thenReturn(List.<Player>of().iterator());
        when(playerManager.containsPlayerIdentity(anyString())).thenReturn(false);
        when(playerManager.getPlayerByIdentity(anyString())).thenReturn(null);
        doNothing().when(playerManager).addPlayer(any(Player.class));
        doNothing().when(playerManager).removePlayerByIdentity(anyString());
        when(slotStrategy.getFreePlayerSlotInRoom()).thenReturn(0);
        doNothing().when(slotStrategy).tryTakeSlot(anyInt());
        doNothing().when(slotStrategy).freeSlotWhenPlayerLeft(anyInt());
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
        // Set up room capacity
        room.setCapacity(2, 1);

        // Create mock players
        Player participant1 = mock(Player.class);
        Player participant2 = mock(Player.class);
        Player participant3 = mock(Player.class);
        Player spectator1 = mock(Player.class);
        Player spectator2 = mock(Player.class);

        // Set up mock identities
        when(participant1.getIdentity()).thenReturn("P1");
        when(participant2.getIdentity()).thenReturn("P2");
        when(participant3.getIdentity()).thenReturn("P3");
        when(spectator1.getIdentity()).thenReturn("S1");
        when(spectator2.getIdentity()).thenReturn("S2");

        // Mock player manager behavior for initial state
        List<Player> playerList = new ArrayList<>();
        when(playerManager.getReadonlyPlayersList()).thenReturn(playerList);
        when(playerManager.getPlayerCount()).thenReturn(0);

        // Add first participant
        when(playerManager.getPlayerCount()).thenReturn(1);
        playerList.add(participant1);
        when(participant1.getRoleInRoom()).thenReturn(PlayerRoleInRoom.PARTICIPANT);
        room.addPlayer(participant1, false, -1);
        assertEquals(1, room.getParticipantCount());

        // Add second participant
        when(playerManager.getPlayerCount()).thenReturn(2);
        playerList.add(participant2);
        when(participant2.getRoleInRoom()).thenReturn(PlayerRoleInRoom.PARTICIPANT);
        room.addPlayer(participant2, false, -1);
        assertEquals(2, room.getParticipantCount());

        // Try to add third participant (should fail)
        assertThrows(PlayerJoinedRoomException.class, () -> room.addPlayer(participant3, false, -1));
        assertEquals(2, room.getParticipantCount());

        // Add spectator
        when(playerManager.getPlayerCount()).thenReturn(3);
        playerList.add(spectator1);
        when(spectator1.getRoleInRoom()).thenReturn(PlayerRoleInRoom.SPECTATOR);
        room.addPlayer(spectator1, true, -1);
        assertEquals(1, room.getSpectatorCount());

        // Try to add second spectator (should fail)
        assertThrows(PlayerJoinedRoomException.class, () -> room.addPlayer(spectator2, true, -1));
        assertEquals(1, room.getSpectatorCount());
    }

    @Test
    @DisplayName("Room should handle player role switching correctly")
    void testPlayerRoleSwitching() {
        Player player = DefaultPlayer.newInstance("TestPlayer");
        when(playerManager.containsPlayerIdentity(player.getIdentity())).thenReturn(true);
        when(playerManager.getPlayerByIdentity(player.getIdentity())).thenReturn(player);
        
        room.addPlayer(player, null, true, Room.NIL_SLOT); // Add as spectator
        player.setCurrentRoom(room);
        player.setRoleInRoom(PlayerRoleInRoom.SPECTATOR);
        
        room.switchSpectatorToParticipant(player, Room.DEFAULT_SLOT);
        assertEquals(PlayerRoleInRoom.PARTICIPANT, player.getRoleInRoom());
        
        room.switchParticipantToSpectator(player);
        assertEquals(PlayerRoleInRoom.SPECTATOR, player.getRoleInRoom());
    }

    @Test
    @DisplayName("Room should handle player removal correctly")
    void testPlayerRemoval() {
        Player player = mock(Player.class);
        when(player.getIdentity()).thenReturn("TestPlayer");
        
        // Mock player manager behavior for adding player
        when(playerManager.getPlayerCount()).thenReturn(1);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(player));
        when(player.getRoleInRoom()).thenReturn(PlayerRoleInRoom.PARTICIPANT);
        
        // Add player
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        assertEquals(1, room.getParticipantCount());
        
        // Mock player manager behavior for removing player
        when(playerManager.containsPlayerIdentity(player.getIdentity())).thenReturn(true);
        when(playerManager.getPlayerByIdentity(player.getIdentity())).thenReturn(player);
        when(playerManager.getPlayerCount()).thenReturn(0);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of());
        
        // Remove player
        room.removePlayer(player);
        verify(playerManager).removePlayerByIdentity(player.getIdentity());
        assertEquals(0, room.getParticipantCount());
        assertTrue(room.isEmpty());
    }

    @Test
    @DisplayName("Room should handle owner management correctly")
    void testOwnerManagement() {
        Player owner = DefaultPlayer.newInstance("Owner");
        when(playerManager.containsPlayerIdentity(owner.getIdentity())).thenReturn(false, true);
        when(slotStrategy.getFreePlayerSlotInRoom()).thenReturn(0);
        room.addPlayer(owner, null, false, Room.DEFAULT_SLOT);
        room.setOwner(owner);
        
        assertTrue(room.getOwner().isPresent());
        assertEquals(owner, room.getOwner().get());
        
        when(playerManager.containsPlayerIdentity(owner.getIdentity())).thenReturn(true);
        room.removePlayer(owner);
        assertFalse(room.getOwner().isPresent());
    }

    @Test
    @DisplayName("Room should provide correct player lists")
    void testPlayerLists() {
        Player participant = mock(Player.class);
        Player spectator = mock(Player.class);
        
        // Mock player manager behavior for participant
        when(playerManager.getPlayerCount()).thenReturn(1);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(participant));
        when(participant.getRoleInRoom()).thenReturn(PlayerRoleInRoom.PARTICIPANT);
        
        // Add participant
        room.addPlayer(participant, null, false, Room.DEFAULT_SLOT);
        assertEquals(1, room.getParticipantCount());
        assertEquals(0, room.getSpectatorCount());
        
        // Mock player manager behavior for spectator
        when(playerManager.getPlayerCount()).thenReturn(2);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(participant, spectator));
        when(spectator.getRoleInRoom()).thenReturn(PlayerRoleInRoom.SPECTATOR);
        
        // Add spectator
        room.addPlayer(spectator, null, true, Room.NIL_SLOT);
        assertEquals(1, room.getParticipantCount());
        assertEquals(1, room.getSpectatorCount());
        
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
        when(playerManager.containsPlayerIdentity(player.getIdentity())).thenReturn(true);
        when(playerManager.getPlayerByIdentity(player.getIdentity())).thenReturn(player);
        
        room.addPlayer(player, null, false, Room.DEFAULT_SLOT);
        player.setCurrentRoom(room);
        player.setRoleInRoom(PlayerRoleInRoom.PARTICIPANT);
        
        Optional<Player> foundPlayer = room.getPlayerByIdentity(player.getIdentity());
        assertTrue(foundPlayer.isPresent());
        assertEquals(player, foundPlayer.get());
        
        when(playerManager.containsPlayerIdentity("NonExistentPlayer")).thenReturn(false);
        when(playerManager.getPlayerByIdentity("NonExistentPlayer")).thenReturn(null);
        
        Optional<Player> notFoundPlayer = room.getPlayerByIdentity("NonExistentPlayer");
        assertFalse(notFoundPlayer.isPresent());
    }

    @Test
    @DisplayName("Room should handle player iteration correctly")
    void testPlayerIterator() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        
        // Mock player manager behavior for first player
        when(playerManager.getPlayerCount()).thenReturn(1);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(player1));
        when(playerManager.getPlayerIterator()).thenReturn(List.of(player1).iterator());
        when(player1.getRoleInRoom()).thenReturn(PlayerRoleInRoom.PARTICIPANT);
        
        // Add first player
        room.addPlayer(player1, null, false, Room.DEFAULT_SLOT);
        assertEquals(1, room.getParticipantCount());
        
        // Mock player manager behavior for second player
        when(playerManager.getPlayerCount()).thenReturn(2);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(player1, player2));
        when(playerManager.getPlayerIterator()).thenReturn(List.of(player1, player2).iterator());
        when(player2.getRoleInRoom()).thenReturn(PlayerRoleInRoom.PARTICIPANT);
        
        // Add second player
        room.addPlayer(player2, null, false, Room.DEFAULT_SLOT);
        assertEquals(2, room.getParticipantCount());
        
        Iterator<Player> iterator = room.getPlayerIterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test invalid role switches")
    void testInvalidRoleSwitches() {
        // Setup
        Player player = DefaultPlayer.newInstance("player1");
        when(playerManager.containsPlayerIdentity(player.getIdentity())).thenReturn(false);
        when(slotStrategy.getFreePlayerSlotInRoom()).thenReturn(0);
        room.addPlayer(player);
        
        // Set player as participant
        player.setRoleInRoom(PlayerRoleInRoom.PARTICIPANT);
        
        // Attempt to switch to participant again (should throw exception)
        assertThrows(SwitchedPlayerRoleInRoomException.class, () -> {
            room.switchSpectatorToParticipant(player);
        }, "Should throw exception when switching to the same role");
        
        // Set player as spectator
        player.setRoleInRoom(PlayerRoleInRoom.SPECTATOR);
        
        // Attempt to switch to spectator again (should throw exception)
        assertThrows(SwitchedPlayerRoleInRoomException.class, () -> {
            room.switchParticipantToSpectator(player);
        }, "Should throw exception when switching to the same role");
    }

    @Test
    @DisplayName("Room should handle non-existent player removal")
    void testNonExistentPlayerRemoval() {
        Player player = DefaultPlayer.newInstance("NonExistentPlayer");
        assertThrows(RemovedNonExistentPlayerException.class, () ->
            room.removePlayer(player));
    }

    @Test
    @DisplayName("Room should clear all players and reset properties")
    void testClear() {
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        
        when(player1.getIdentity()).thenReturn("Player1");
        when(player2.getIdentity()).thenReturn("Player2");
        when(player1.getPlayerSlotInCurrentRoom()).thenReturn(0);
        when(player2.getPlayerSlotInCurrentRoom()).thenReturn(1);
        
        when(playerManager.getPlayerCount()).thenReturn(2, 0);
        when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(player1, player2));
        when(playerManager.containsPlayerIdentity(player1.getIdentity())).thenReturn(true);
        when(playerManager.containsPlayerIdentity(player2.getIdentity())).thenReturn(true);
        when(slotStrategy.getFreePlayerSlotInRoom()).thenReturn(0, 1);
        
        // Add players to room
        room.addPlayer(player1, null, false, Room.DEFAULT_SLOT);
        room.addPlayer(player2, null, false, Room.DEFAULT_SLOT);
        
        // Clear room
        room.clear();
        
        // Verify
        verify(playerManager).removePlayerByIdentity(player1.getIdentity());
        verify(playerManager).removePlayerByIdentity(player2.getIdentity());
        verify(playerManager).clear();
        assertEquals(0, room.getParticipantCount());
        assertTrue(room.isEmpty());
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
        // Setup
        Player player = DefaultPlayer.newInstance("player1");
        when(playerManager.containsPlayerIdentity(player.getIdentity())).thenReturn(false, true);
        when(slotStrategy.getFreePlayerSlotInRoom()).thenReturn(0);
        
        // Test adding player
        room.addPlayer(player);
        verify(slotStrategy).getFreePlayerSlotInRoom();
        
        // Test removing player
        when(playerManager.containsPlayerIdentity(player.getIdentity())).thenReturn(true);
        room.removePlayer(player);
        verify(slotStrategy).freeSlotWhenPlayerLeft(0);
    }
}

