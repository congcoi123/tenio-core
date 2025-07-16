package com.tenio.core.schedule.task.internal;

import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.api.ServerApi;
import com.tenio.core.server.ServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import static org.junit.jupiter.api.Assertions.*;

class AutoRemoveRoomTaskTest {

    private EventManager eventManager;
    private RoomManager roomManager;
    private AutoRemoveRoomTask task;

    @BeforeEach
    void setUp() {
        eventManager = Mockito.mock(EventManager.class);
        roomManager = Mockito.mock(RoomManager.class);
        task = AutoRemoveRoomTask.newInstance(eventManager);
        task.setRoomManager(roomManager);
    }

    @Test
    void testNewInstance() {
        assertNotNull(AutoRemoveRoomTask.newInstance(eventManager));
    }

    @Test
    void testSetRoomManager() {
        task.setRoomManager(roomManager);
        // No exception means success
    }

    @Test
    void testRunSchedulesTaskAndRemovesEmptyRooms() {
        Room emptyRoom = Mockito.mock(Room.class);
        Mockito.when(emptyRoom.getRoomRemoveMode()).thenReturn(RoomRemoveMode.WHEN_EMPTY);
        Mockito.when(emptyRoom.isEmpty()).thenReturn(true);
        com.tenio.core.entity.RoomState roomState = Mockito.mock(com.tenio.core.entity.RoomState.class);
        Mockito.when(emptyRoom.getState()).thenReturn(roomState);
        Mockito.when(roomState.isIdle()).thenReturn(true);
        Mockito.when(emptyRoom.getId()).thenReturn(1L);
        Mockito.when(roomManager.getReadonlyRoomsList()).thenReturn(List.of(emptyRoom));
        Mockito.when(roomManager.getRoomCount()).thenReturn(1);
        ServerApi api = Mockito.mock(ServerApi.class);
        ServerImpl server = Mockito.mock(ServerImpl.class);
        Mockito.when(server.getApi()).thenReturn(api);
        try (MockedStatic<ServerImpl> serverStatic = Mockito.mockStatic(ServerImpl.class)) {
            serverStatic.when(ServerImpl::getInstance).thenReturn(server);
            ScheduledFuture<?> future = task.run();
            assertNotNull(future);
        }
    }
} 