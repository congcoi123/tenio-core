package com.tenio.core.entity.manager.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.define.result.RoomCreatedResult;
import com.tenio.core.entity.implement.DefaultRoom;
import com.tenio.core.entity.setting.InitialRoomSetting;
import com.tenio.core.entity.setting.strategy.RoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.AddedDuplicatedRoomException;
import com.tenio.core.exception.CreatedRoomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Iterator;
import java.util.List;

@DisplayName("Unit Tests For RoomManagerImpl")
class RoomManagerImplTest {

    private RoomManagerImpl roomManager;
    private EventManager eventManager;
    private InitialRoomSetting roomSetting;
    private Player player;
    private static final String ROOM_NAME = "TestRoom";
    private static final String ROOM_PASSWORD = "TestPassword";
    private static final int MAX_ROOMS = 2;

    @BeforeEach
    void setUp() {
        eventManager = mock(EventManager.class);
        roomManager = (RoomManagerImpl) RoomManagerImpl.newInstance(eventManager);
        roomManager.configureMaxRooms(MAX_ROOMS);

        // Setup room setting
        roomSetting = mock(InitialRoomSetting.class);
        RoomCredentialValidatedStrategy credentialStrategy = mock(RoomCredentialValidatedStrategy.class);
        doNothing().when(credentialStrategy).validateName(anyString());
        doNothing().when(credentialStrategy).validatePassword(anyString());
        
        when(roomSetting.getName()).thenReturn(ROOM_NAME);
        when(roomSetting.getMaxParticipants()).thenReturn(10);
        when(roomSetting.getMaxSpectators()).thenReturn(5);
        when(roomSetting.getRoomRemoveMode()).thenReturn(RoomRemoveMode.WHEN_EMPTY);
        when(roomSetting.getRoomPlayerSlotGeneratedStrategy()).thenReturn(mock(RoomPlayerSlotGeneratedStrategy.class));
        when(roomSetting.getRoomCredentialValidatedStrategy()).thenReturn(credentialStrategy);
        when(roomSetting.getPassword()).thenReturn(ROOM_PASSWORD);

        player = mock(Player.class);
        when(player.getIdentity()).thenReturn("TestPlayer");
    }

  @Test
    @DisplayName("New instance should be properly initialized")
  void testNewInstance() {
        assertEquals(0, roomManager.getRoomCount());
        assertTrue(roomManager.getReadonlyRoomsList().isEmpty());
    }

    @Test
    @DisplayName("Room manager should handle null event manager")
    void testNewInstanceWithNullEventManager() {
        assertThrows(NullPointerException.class, () -> RoomManagerImpl.newInstance(null));
    }

    @Test
    @DisplayName("Room manager should handle room addition correctly")
    void testAddRoom() {
        Room room = DefaultRoom.newInstance();
        RoomCredentialValidatedStrategy credentialStrategy = mock(RoomCredentialValidatedStrategy.class);
        doNothing().when(credentialStrategy).validateName(anyString());
        doNothing().when(credentialStrategy).validatePassword(anyString());
        room.configureRoomCredentialValidatedStrategy(credentialStrategy);
        room.setName(ROOM_NAME);
        
        roomManager.addRoom(room);
        assertEquals(1, roomManager.getRoomCount());
        assertTrue(roomManager.containsRoomId(room.getId()));
        
        // Should not allow duplicate rooms
        assertThrows(AddedDuplicatedRoomException.class, () -> roomManager.addRoom(room));
    }

    @Test
    @DisplayName("Room manager should handle null room addition")
    void testAddNullRoom() {
        assertThrows(NullPointerException.class, () -> roomManager.addRoom(null));
    }

    @Test
    @DisplayName("Room manager should handle room creation with owner correctly")
    void testCreateRoomWithOwner() {
        Room room = roomManager.createRoomWithOwner(roomSetting, player);
        assertNotNull(room);
        assertEquals(ROOM_NAME, room.getName());
        assertTrue(room.getOwner().isPresent());
        assertEquals(player, room.getOwner().get());
        assertEquals(1, roomManager.getRoomCount());
    }

    @Test
    @DisplayName("Room manager should handle null owner in room creation")
    void testCreateRoomWithNullOwner() {
        assertThrows(NullPointerException.class, () -> roomManager.createRoomWithOwner(roomSetting, null));
    }

    @Test
    @DisplayName("Room manager should handle null room settings")
    void testCreateRoomWithNullSettings() {
        assertThrows(NullPointerException.class, () -> roomManager.createRoomWithOwner(null, player));
    }

    @ParameterizedTest
    @DisplayName("Room manager should handle different maximum room configurations")
    @ValueSource(ints = {0, 1, 10, 100})
    void testMaxRoomConfiguration(int maxRooms) {
        roomManager.configureMaxRooms(maxRooms);
        for (int i = 0; i < maxRooms; i++) {
            when(roomSetting.getName()).thenReturn(ROOM_NAME + i);
            roomManager.createRoomWithOwner(roomSetting, player);
        }
        assertEquals(maxRooms, roomManager.getRoomCount());
        assertThrows(CreatedRoomException.class, () -> 
            roomManager.createRoomWithOwner(roomSetting, player));
    }

    @Test
    @DisplayName("Room manager should handle negative maximum room configuration")
    void testNegativeMaxRoomConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> roomManager.configureMaxRooms(-1));
    }

    @Test
    @DisplayName("Room manager should handle room lookup correctly")
    void testRoomLookup() {
        Room room = roomManager.createRoomWithOwner(roomSetting, player);
        
        assertTrue(roomManager.containsRoomId(room.getId()));
        assertTrue(roomManager.containsRoomName(ROOM_NAME));
        assertEquals(room, roomManager.getRoomById(room.getId()));
        
        List<Room> rooms = roomManager.getReadonlyRoomsListByName(ROOM_NAME);
        assertEquals(1, rooms.size());
        assertEquals(room, rooms.get(0));
    }

    @Test
    @DisplayName("Room manager should handle non-existent room lookup")
    void testNonExistentRoomLookup() {
        assertFalse(roomManager.containsRoomId(999L));
        assertFalse(roomManager.containsRoomName("NonExistentRoom"));
        assertNull(roomManager.getRoomById(999L));
        assertTrue(roomManager.getReadonlyRoomsListByName("NonExistentRoom").isEmpty());
    }

    @Test
    @DisplayName("Room manager should handle room iteration correctly")
    void testRoomIteration() {
        when(roomSetting.getName()).thenReturn(ROOM_NAME + "1");
        Room room1 = roomManager.createRoomWithOwner(roomSetting, player);
        when(roomSetting.getName()).thenReturn(ROOM_NAME + "2");
        Room room2 = roomManager.createRoomWithOwner(roomSetting, player);
        
        @SuppressWarnings("unchecked")
        Iterator<Room> iterator = roomManager.getRoomIterator();
        int count = 0;
        while (iterator.hasNext()) {
            Room room = iterator.next();
            assertTrue(room.equals(room1) || room.equals(room2));
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Room manager should handle room removal correctly")
    void testRoomRemoval() {
        Room room = roomManager.createRoomWithOwner(roomSetting, player);
        long roomId = room.getId();
        
        roomManager.removeRoomById(roomId);
        assertEquals(0, roomManager.getRoomCount());
        assertFalse(roomManager.containsRoomId(roomId));
    }

    @Test
    @DisplayName("Room manager should handle non-existent room removal")
    void testNonExistentRoomRemoval() {
        assertDoesNotThrow(() -> roomManager.removeRoomById(999L));
    }

    @Test
    @DisplayName("Room manager should handle clear operation correctly")
    void testClear() {
        when(roomSetting.getName()).thenReturn(ROOM_NAME + "1");
        roomManager.createRoomWithOwner(roomSetting, player);
        when(roomSetting.getName()).thenReturn(ROOM_NAME + "2");
        roomManager.createRoomWithOwner(roomSetting, player);
        
        roomManager.clear();
        assertEquals(0, roomManager.getRoomCount());
        assertTrue(roomManager.getReadonlyRoomsList().isEmpty());
    }

    @Test
    @DisplayName("Room manager should handle clear operation on empty manager")
    void testClearEmptyManager() {
        assertDoesNotThrow(() -> roomManager.clear());
        assertEquals(0, roomManager.getRoomCount());
  }
}

