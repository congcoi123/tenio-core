package com.tenio.core.entity.setting.strategy.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.entity.implement.DefaultPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Unit Tests For DefaultRoomPlayerSlotGeneratedStrategy")
class DefaultRoomPlayerSlotGeneratedStrategyTest {

    private DefaultRoomPlayerSlotGeneratedStrategy strategy;
    private Room room;
    private static final String PLAYER_NAME = "TestPlayer";
    private static final int MAX_PARTICIPANTS = 4;

    @BeforeEach
    void setUp() {
        strategy = new DefaultRoomPlayerSlotGeneratedStrategy();
        room = mock(Room.class);
        when(room.getMaxParticipants()).thenReturn(MAX_PARTICIPANTS);
        strategy.setRoom(room);
    }

    @Test
    @DisplayName("Strategy should be properly initialized")
    void testInitialization() {
        assertNotNull(strategy);
        assertEquals(room, strategy.getRoom());
    }

    @Test
    @DisplayName("Strategy should handle null room")
    void testNullRoom() {
        strategy = new DefaultRoomPlayerSlotGeneratedStrategy();
        assertThrows(NullPointerException.class, () -> strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should return first free slot")
    void testGetFirstFreeSlot() {
        assertEquals(0, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle slot freeing")
    void testFreeSlot() {
        int slot = strategy.getFreePlayerSlotInRoom();
        strategy.freeSlotWhenPlayerLeft(slot);
        assertEquals(slot, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle invalid slot freeing")
    void testFreeInvalidSlot() {
        assertThrows(IllegalArgumentException.class, () -> strategy.freeSlotWhenPlayerLeft(-1));
        assertThrows(IllegalArgumentException.class, () -> strategy.freeSlotWhenPlayerLeft(MAX_PARTICIPANTS));
    }

    @Test
    @DisplayName("Strategy should handle sequential slot allocation")
    void testSequentialSlotAllocation() {
        for (int i = 0; i < MAX_PARTICIPANTS; i++) {
            assertEquals(i, strategy.getFreePlayerSlotInRoom());
        }
    }

    @Test
    @DisplayName("Strategy should handle slot reuse")
    void testSlotReuse() {
        int slot1 = strategy.getFreePlayerSlotInRoom();
        int slot2 = strategy.getFreePlayerSlotInRoom();
        strategy.freeSlotWhenPlayerLeft(slot1);
        assertEquals(slot1, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle room capacity")
    void testRoomCapacity() {
        // Fill all slots
        for (int i = 0; i < MAX_PARTICIPANTS; i++) {
            strategy.getFreePlayerSlotInRoom();
        }
        
        // Try to get one more slot
        assertEquals(Room.NIL_SLOT, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle room capacity changes")
    void testRoomCapacityChanges() {
        // Fill all slots
        for (int i = 0; i < MAX_PARTICIPANTS; i++) {
            strategy.getFreePlayerSlotInRoom();
        }
        
        // Change room capacity
        when(room.getMaxParticipants()).thenReturn(MAX_PARTICIPANTS + 1);
        assertEquals(MAX_PARTICIPANTS, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle multiple slot operations")
    void testMultipleSlotOperations() {
        int slot1 = strategy.getFreePlayerSlotInRoom();
        int slot2 = strategy.getFreePlayerSlotInRoom();
        int slot3 = strategy.getFreePlayerSlotInRoom();
        
        strategy.freeSlotWhenPlayerLeft(slot2);
        assertEquals(slot2, strategy.getFreePlayerSlotInRoom());
        
        strategy.freeSlotWhenPlayerLeft(slot1);
        assertEquals(slot1, strategy.getFreePlayerSlotInRoom());
        
        strategy.freeSlotWhenPlayerLeft(slot3);
        assertEquals(slot3, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle initialization without room")
    void testInitializationWithoutRoom() {
        strategy = new DefaultRoomPlayerSlotGeneratedStrategy();
        assertThrows(NullPointerException.class, () -> strategy.initialize());
        assertNull(strategy.getRoom());
    }

    @Test
    @DisplayName("Strategy should return default free slot")
    void testGetFreePlayerSlotInRoom() {
        assertEquals(0, strategy.getFreePlayerSlotInRoom());
    }

    @ParameterizedTest
    @DisplayName("Strategy should handle slot freeing for various slot numbers")
    @ValueSource(ints = {-1, 0, 1, 100, Integer.MAX_VALUE})
    void testFreeSlotWhenPlayerLeft(int slotNumber) {
        if (slotNumber < 0 || slotNumber >= MAX_PARTICIPANTS) {
            assertThrows(IllegalArgumentException.class, () -> strategy.freeSlotWhenPlayerLeft(slotNumber));
        } else {
            assertDoesNotThrow(() -> strategy.freeSlotWhenPlayerLeft(slotNumber));
        }
    }

    @ParameterizedTest
    @DisplayName("Strategy should handle slot taking for various slot numbers")
    @ValueSource(ints = {-1, 0, 1, 100, Integer.MAX_VALUE})
    void testTryTakeSlot(int slotNumber) {
        if (slotNumber < 0 || slotNumber >= MAX_PARTICIPANTS) {
            assertThrows(IllegalArgumentException.class, () -> strategy.tryTakeSlot(slotNumber));
        } else {
            assertDoesNotThrow(() -> strategy.tryTakeSlot(slotNumber));
        }
    }

    @Test
    @DisplayName("Strategy should handle room setting")
    void testRoomSetting() {
        Room newRoom = mock(Room.class);
        strategy.setRoom(newRoom);
        assertEquals(newRoom, strategy.getRoom());
    }

    @Test
    @DisplayName("Strategy should handle null room setting")
    void testNullRoomSetting() {
        strategy.setRoom(null);
        assertNull(strategy.getRoom());
    }

    @Test
    @DisplayName("Strategy should maintain state after multiple operations")
    void testMultipleOperations() {
        strategy.tryTakeSlot(1);
        strategy.tryTakeSlot(2);
        strategy.freeSlotWhenPlayerLeft(1);
        assertEquals(0, strategy.getFreePlayerSlotInRoom());
    }

    @Test
    @DisplayName("Strategy should handle consecutive initializations")
    void testConsecutiveInitializations() {
        strategy.initialize();
        Room newRoom = mock(Room.class);
        when(newRoom.getMaxParticipants()).thenReturn(MAX_PARTICIPANTS);
        strategy.setRoom(newRoom);
        strategy.initialize();
        assertEquals(newRoom, strategy.getRoom());
    }

    @Test
    @DisplayName("Strategy should handle slot taking")
    void testTryTakeSlot() {
        // Try taking valid slots
        assertDoesNotThrow(() -> strategy.tryTakeSlot(0));
        assertDoesNotThrow(() -> strategy.tryTakeSlot(MAX_PARTICIPANTS - 1));
        
        // Try taking invalid slots
        assertThrows(IllegalArgumentException.class, () -> strategy.tryTakeSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> strategy.tryTakeSlot(MAX_PARTICIPANTS));
        
        // Try taking already taken slot
        assertThrows(IllegalArgumentException.class, () -> strategy.tryTakeSlot(0));
    }
}

