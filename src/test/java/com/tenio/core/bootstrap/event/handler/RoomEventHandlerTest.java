package com.tenio.core.bootstrap.event.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.event.Subscriber;
import com.tenio.core.event.handler.implement.RoomEventHandler;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.handler.event.EventPlayerAfterLeftRoom;
import com.tenio.core.handler.event.EventPlayerBeforeLeaveRoom;
import com.tenio.core.handler.event.EventPlayerJoinedRoomResult;
import com.tenio.core.handler.event.EventRoomCreatedResult;
import com.tenio.core.handler.event.EventRoomWillBeRemoved;
import com.tenio.core.handler.event.EventSwitchParticipantToSpectatorResult;
import com.tenio.core.handler.event.EventSwitchSpectatorToParticipantResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test Cases For RoomEventHandler")
class RoomEventHandlerTest {

  @Mock
  private EventManager eventManager;
  
  @InjectMocks
  private RoomEventHandler roomEventHandler;

  @Test
  @DisplayName("Initialize should register all event handlers")
  void testInitialize() {
    // Act
    assertDoesNotThrow(() -> roomEventHandler.initialize(eventManager));
    
    // Assert - verify that the event manager's on method is not called at all when all event handlers are null
    verify(eventManager, times(0)).on(org.mockito.ArgumentMatchers.any(ServerEvent.class), 
        org.mockito.ArgumentMatchers.any(Subscriber.class));
  }
  
  @Test
  @DisplayName("Initialize should register event handlers when they are not null")
  void testInitializeWithNonNullEventHandlers() throws Exception {
    // Arrange
    EventPlayerAfterLeftRoom<Player, Room> eventPlayerAfterLeftRoom = mock(EventPlayerAfterLeftRoom.class);
    EventPlayerBeforeLeaveRoom<Player, Room> eventPlayerBeforeLeaveRoom = mock(EventPlayerBeforeLeaveRoom.class);
    EventPlayerJoinedRoomResult<Player, Room> eventPlayerJoinedRoomResult = mock(EventPlayerJoinedRoomResult.class);
    EventRoomCreatedResult<Room> eventRoomCreatedResult = mock(EventRoomCreatedResult.class);
    EventRoomWillBeRemoved<Room> eventRoomWillBeRemoved = mock(EventRoomWillBeRemoved.class);
    EventSwitchParticipantToSpectatorResult<Player, Room> eventSwitchParticipantToSpectatorResult = 
        mock(EventSwitchParticipantToSpectatorResult.class);
    EventSwitchSpectatorToParticipantResult<Player, Room> eventSwitchSpectatorToParticipantResult = 
        mock(EventSwitchSpectatorToParticipantResult.class);
    
    // Use reflection to set the private fields
    setPrivateField(roomEventHandler, "eventPlayerAfterLeftRoom", eventPlayerAfterLeftRoom);
    setPrivateField(roomEventHandler, "eventPlayerBeforeLeaveRoom", eventPlayerBeforeLeaveRoom);
    setPrivateField(roomEventHandler, "eventPlayerJoinedRoomResult", eventPlayerJoinedRoomResult);
    setPrivateField(roomEventHandler, "eventRoomCreatedResult", eventRoomCreatedResult);
    setPrivateField(roomEventHandler, "eventRoomWillBeRemoved", eventRoomWillBeRemoved);
    setPrivateField(roomEventHandler, "eventSwitchParticipantToSpectatorResult", 
        eventSwitchParticipantToSpectatorResult);
    setPrivateField(roomEventHandler, "eventSwitchSpectatorToParticipantResult", 
        eventSwitchSpectatorToParticipantResult);
    
    // Act
    roomEventHandler.initialize(eventManager);
    
    // Assert - verify that the event manager's on method is called with the correct events and non-null subscribers
    verify(eventManager, times(1)).on(ServerEvent.PLAYER_AFTER_LEFT_ROOM, org.mockito.ArgumentMatchers.any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.PLAYER_BEFORE_LEAVE_ROOM, org.mockito.ArgumentMatchers.any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.PLAYER_JOINED_ROOM_RESULT, org.mockito.ArgumentMatchers.any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.ROOM_CREATED_RESULT, org.mockito.ArgumentMatchers.any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.ROOM_WILL_BE_REMOVED, org.mockito.ArgumentMatchers.any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.SWITCH_PARTICIPANT_TO_SPECTATOR, org.mockito.ArgumentMatchers.any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.SWITCH_SPECTATOR_TO_PARTICIPANT, org.mockito.ArgumentMatchers.any(Subscriber.class));
  }
  
  /**
   * Helper method to set private fields using reflection.
   */
  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = RoomEventHandler.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
