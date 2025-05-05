package com.tenio.core.entity.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.setting.strategy.RoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomPlayerSlotGeneratedStrategy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For InitialRoomSetting")
class InitialRoomSettingTest {
  
  @Test
  @DisplayName("Builder.build() should create a default InitialRoomSetting")
  void testBuilderBuild() {
    InitialRoomSetting actualBuildResult = InitialRoomSetting.Builder.newInstance().build();
    assertEquals(0, actualBuildResult.getMaxParticipants());
    assertFalse(actualBuildResult.isActivated());
    assertEquals(RoomRemoveMode.WHEN_EMPTY, actualBuildResult.getRoomRemoveMode());
    assertTrue(actualBuildResult
        .getRoomPlayerSlotGeneratedStrategy() instanceof DefaultRoomPlayerSlotGeneratedStrategy);
    assertTrue(actualBuildResult
        .getRoomCredentialValidatedStrategy() instanceof DefaultRoomCredentialValidatedStrategy);
    assertEquals(0, actualBuildResult.getMaxSpectators());
    assertNull(actualBuildResult.getPassword());
    assertNull(actualBuildResult.getName());
    assertNull(actualBuildResult.getProperties());
  }

  @Test
  @DisplayName("Builder.newInstance() should create a builder with configurable properties")
  void testBuilderNewInstance() {
    // Arrange
    String roomName = "TestRoom";
    String password = "password123";
    int maxParticipants = 10;
    int maxSpectators = 5;
    Map<String, Object> properties = new HashMap<>();
    properties.put("key1", "value1");
    properties.put("key2", 123);
    
    // Act
    InitialRoomSetting.Builder builder = InitialRoomSetting.Builder.newInstance();
    builder.setName(roomName)
           .setPassword(password)
           .setMaxParticipants(maxParticipants)
           .setMaxSpectators(maxSpectators)
           .setActivated(true)
           .setRoomRemoveMode(RoomRemoveMode.NEVER_REMOVE)
           .setProperties(properties);
    
    InitialRoomSetting setting = builder.build();
    
    // Assert
    assertEquals(roomName, setting.getName());
    assertEquals(password, setting.getPassword());
    assertEquals(maxParticipants, setting.getMaxParticipants());
    assertEquals(maxSpectators, setting.getMaxSpectators());
    assertTrue(setting.isActivated());
    assertEquals(RoomRemoveMode.NEVER_REMOVE, setting.getRoomRemoveMode());
    assertEquals(properties, setting.getProperties());
  }

  @Test
  @DisplayName("Builder.newInstance() should create a builder with default values")
  void testBuilderNewInstance2() {
    // Act
    InitialRoomSetting.Builder builder = InitialRoomSetting.Builder.newInstance();
    InitialRoomSetting setting = builder.build();
    
    // Assert
    assertNull(setting.getName());
    assertNull(setting.getPassword());
    assertEquals(0, setting.getMaxParticipants());
    assertEquals(0, setting.getMaxSpectators());
    assertFalse(setting.isActivated());
    assertEquals(RoomRemoveMode.WHEN_EMPTY, setting.getRoomRemoveMode());
    assertNull(setting.getProperties());
    assertTrue(setting.getRoomCredentialValidatedStrategy() instanceof DefaultRoomCredentialValidatedStrategy);
    assertTrue(setting.getRoomPlayerSlotGeneratedStrategy() instanceof DefaultRoomPlayerSlotGeneratedStrategy);
  }

  @Test
  @DisplayName("setRoomCredentialValidatedStrategy should set the strategy interface")
  void testBuilderSetRoomCredentialValidatedStrategy() {
    InitialRoomSetting.Builder builder = InitialRoomSetting.Builder.newInstance();
    assertSame(builder, builder.setRoomCredentialValidatedStrategy(RoomCredentialValidatedStrategy.class));
    
    // The build should fail when using the interface directly, but we're just testing the builder return value
  }

  @Test
  @DisplayName("setRoomCredentialValidatedStrategy should set a concrete strategy implementation")
  void testBuilderSetRoomCredentialValidatedStrategy2() {
    // Arrange
    InitialRoomSetting.Builder builder = InitialRoomSetting.Builder.newInstance();
    
    // Act
    builder.setRoomCredentialValidatedStrategy(DefaultRoomCredentialValidatedStrategy.class);
    InitialRoomSetting setting = builder.build();
    
    // Assert
    assertNotNull(setting.getRoomCredentialValidatedStrategy());
    assertTrue(setting.getRoomCredentialValidatedStrategy() instanceof DefaultRoomCredentialValidatedStrategy);
  }

  @Test
  @DisplayName("setRoomPlayerSlotGeneratedStrategy should set the strategy interface")
  void testBuilderSetRoomPlayerSlotGeneratedStrategy() {
    InitialRoomSetting.Builder builder = InitialRoomSetting.Builder.newInstance();
    assertSame(builder, builder.setRoomPlayerSlotGeneratedStrategy(RoomPlayerSlotGeneratedStrategy.class));
    
    // The build should fail when using the interface directly, but we're just testing the builder return value
  }
  
  @Test
  @DisplayName("setRoomPlayerSlotGeneratedStrategy should set a concrete strategy implementation")
  void testBuilderSetRoomPlayerSlotGeneratedStrategy2() {
    // Arrange
    InitialRoomSetting.Builder builder = InitialRoomSetting.Builder.newInstance();
    
    // Act
    builder.setRoomPlayerSlotGeneratedStrategy(DefaultRoomPlayerSlotGeneratedStrategy.class);
    InitialRoomSetting setting = builder.build();
    
    // Assert
    assertNotNull(setting.getRoomPlayerSlotGeneratedStrategy());
    assertTrue(setting.getRoomPlayerSlotGeneratedStrategy() instanceof DefaultRoomPlayerSlotGeneratedStrategy);
  }
  
  @Test
  @DisplayName("toString should return a string representation with all properties")
  void testToString() {
    // Arrange
    String roomName = "TestRoom";
    String password = "password123";
    int maxParticipants = 10;
    int maxSpectators = 5;
    
    InitialRoomSetting setting = InitialRoomSetting.Builder.newInstance()
        .setName(roomName)
        .setPassword(password)
        .setMaxParticipants(maxParticipants)
        .setMaxSpectators(maxSpectators)
        .setActivated(true)
        .build();
    
    // Act
    String result = setting.toString();
    
    // Assert
    assertTrue(result.contains(roomName));
    assertTrue(result.contains(password));
    assertTrue(result.contains(String.valueOf(maxParticipants)));
    assertTrue(result.contains(String.valueOf(maxSpectators)));
    assertTrue(result.contains("activated=true"));
  }
}

