package com.tenio.core.bootstrap.event.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.event.Subscriber;
import com.tenio.core.event.handler.implement.PlayerEventHandler;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.handler.event.EventDisconnectPlayer;
import com.tenio.core.handler.event.EventPlayerLoggedinResult;
import com.tenio.core.handler.event.EventPlayerReconnectRequestHandle;
import com.tenio.core.handler.event.EventPlayerReconnectedResult;
import com.tenio.core.handler.event.EventReceivedMessageFromPlayer;
import com.tenio.core.handler.event.EventSendMessageToPlayer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test Cases For PlayerEventHandler")
class PlayerEventHandlerTest {

  @Mock
  private EventManager eventManager;
  
  @InjectMocks
  private PlayerEventHandler playerEventHandler;

  @Test
  @DisplayName("Initialize should register all event handlers")
  void testInitialize() {
    // Act
    assertDoesNotThrow(() -> playerEventHandler.initialize(eventManager));
    
    // Assert - verify that the event manager's on method is not called at all when all event handlers are null
    verify(eventManager, times(0)).on(any(ServerEvent.class), any(Subscriber.class));
  }
  
  @Test
  @DisplayName("Initialize should register event handlers when they are not null")
  @SuppressWarnings("unchecked")
  void testInitializeWithNonNullEventHandlers() throws Exception {
    // Arrange
    EventPlayerLoggedinResult<Player> eventPlayerLoggedInResult = mock(EventPlayerLoggedinResult.class);
    EventPlayerReconnectRequestHandle<Player> eventPlayerReconnectRequestHandle = 
        mock(EventPlayerReconnectRequestHandle.class);
    EventPlayerReconnectedResult<Player> eventPlayerReconnectedResult = 
        mock(EventPlayerReconnectedResult.class);
    EventReceivedMessageFromPlayer<Player> eventReceivedMessageFromPlayer = 
        mock(EventReceivedMessageFromPlayer.class);
    EventSendMessageToPlayer<Player> eventSendMessageToPlayer = mock(EventSendMessageToPlayer.class);
    EventDisconnectPlayer<Player> eventDisconnectPlayer = mock(EventDisconnectPlayer.class);
    
    // Use reflection to set the private fields
    setPrivateField(playerEventHandler, "eventPlayerLoggedInResult", eventPlayerLoggedInResult);
    setPrivateField(playerEventHandler, "eventPlayerReconnectRequestHandle", 
        eventPlayerReconnectRequestHandle);
    setPrivateField(playerEventHandler, "eventPlayerReconnectedResult", 
        eventPlayerReconnectedResult);
    setPrivateField(playerEventHandler, "eventReceivedMessageFromPlayer", 
        eventReceivedMessageFromPlayer);
    setPrivateField(playerEventHandler, "eventSendMessageToPlayer", eventSendMessageToPlayer);
    setPrivateField(playerEventHandler, "eventDisconnectPlayer", eventDisconnectPlayer);
    
    // Act
    playerEventHandler.initialize(eventManager);
    
    // Assert - verify that the event manager's on method is called with the correct events and non-null subscribers
    verify(eventManager, times(1)).on(ServerEvent.PLAYER_LOGGEDIN_RESULT, any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.PLAYER_RECONNECT_REQUEST_HANDLE, any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.PLAYER_RECONNECTED_RESULT, any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER, any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.SEND_MESSAGE_TO_PLAYER, any(Subscriber.class));
    verify(eventManager, times(1)).on(ServerEvent.DISCONNECT_PLAYER, any(Subscriber.class));
  }
  
  /**
   * Helper method to set private fields using reflection.
   */
  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = PlayerEventHandler.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
