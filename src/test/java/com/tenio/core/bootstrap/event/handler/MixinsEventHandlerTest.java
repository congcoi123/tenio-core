package com.tenio.core.bootstrap.event.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.handler.implement.MixinsEventHandler;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.handler.event.EventFetchedBandwidthInfo;
import com.tenio.core.handler.event.EventFetchedCcuInfo;
import com.tenio.core.handler.event.EventServerException;
import com.tenio.core.handler.event.EventServerInitialization;
import com.tenio.core.handler.event.EventServerTeardown;
import com.tenio.core.handler.event.EventSystemMonitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test Cases For MixinsEventHandler")
class MixinsEventHandlerTest {

  @Mock
  private EventManager eventManager;
  
  @InjectMocks
  private MixinsEventHandler mixinsEventHandler;

  @Test
  @DisplayName("Initialize should register all event handlers")
  void testInitialize() {
    // Act
    assertDoesNotThrow(() -> mixinsEventHandler.initialize(eventManager));
    
    // Assert - verify that the event manager's on method is not called for null event handlers
    verify(eventManager, times(0)).on(ServerEvent.SERVER_INITIALIZATION, null);
    verify(eventManager, times(0)).on(ServerEvent.SERVER_EXCEPTION, null);
    verify(eventManager, times(0)).on(ServerEvent.SERVER_TEARDOWN, null);
    verify(eventManager, times(0)).on(ServerEvent.FETCHED_BANDWIDTH_INFO, null);
    verify(eventManager, times(0)).on(ServerEvent.FETCHED_CCU_INFO, null);
    verify(eventManager, times(0)).on(ServerEvent.SYSTEM_MONITORING, null);
  }
  
  @Test
  @DisplayName("Initialize should register event handlers when they are not null")
  void testInitializeWithNonNullEventHandlers() throws Exception {
    // Arrange
    EventServerInitialization eventServerInitialization = mock(EventServerInitialization.class);
    EventServerException eventServerException = mock(EventServerException.class);
    EventServerTeardown eventServerTeardown = mock(EventServerTeardown.class);
    EventFetchedBandwidthInfo eventFetchedBandwidthInfo = mock(EventFetchedBandwidthInfo.class);
    EventFetchedCcuInfo eventFetchedCcuInfo = mock(EventFetchedCcuInfo.class);
    EventSystemMonitoring eventSystemMonitoring = mock(EventSystemMonitoring.class);
    
    // Use reflection to set the private fields
    setPrivateField(mixinsEventHandler, "eventServerInitialization", eventServerInitialization);
    setPrivateField(mixinsEventHandler, "eventServerException", eventServerException);
    setPrivateField(mixinsEventHandler, "eventServerTeardown", eventServerTeardown);
    setPrivateField(mixinsEventHandler, "eventFetchedBandwidthInfo", eventFetchedBandwidthInfo);
    setPrivateField(mixinsEventHandler, "eventFetchedCcuInfo", eventFetchedCcuInfo);
    setPrivateField(mixinsEventHandler, "eventSystemMonitoring", eventSystemMonitoring);
    
    // Act
    mixinsEventHandler.initialize(eventManager);
    
    // Assert
    verify(eventManager, times(1)).on(ServerEvent.SERVER_INITIALIZATION, null);
    verify(eventManager, times(1)).on(ServerEvent.SERVER_EXCEPTION, null);
    verify(eventManager, times(1)).on(ServerEvent.SERVER_TEARDOWN, null);
    verify(eventManager, times(1)).on(ServerEvent.FETCHED_BANDWIDTH_INFO, null);
    verify(eventManager, times(1)).on(ServerEvent.FETCHED_CCU_INFO, null);
    verify(eventManager, times(1)).on(ServerEvent.SYSTEM_MONITORING, null);
  }
  
  /**
   * Helper method to set private fields using reflection.
   */
  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = MixinsEventHandler.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
