package com.tenio.core.entity.setting.strategy.implement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For DefaultRoomCredentialValidatedStrategy")
class DefaultRoomCredentialValidatedStrategyTest {
  
  @Test
  @DisplayName("Constructor should create a valid instance")
  void testConstructor() {
    // Act & Assert
    assertDoesNotThrow(() -> new DefaultRoomCredentialValidatedStrategy());
  }
  
  @Test
  @DisplayName("validateName should accept any name without throwing exceptions")
  void testValidateName() {
    // Arrange
    DefaultRoomCredentialValidatedStrategy strategy = new DefaultRoomCredentialValidatedStrategy();
    
    // Act & Assert
    assertDoesNotThrow(() -> strategy.validateName(null));
    assertDoesNotThrow(() -> strategy.validateName(""));
    assertDoesNotThrow(() -> strategy.validateName("Room Name"));
    assertDoesNotThrow(() -> strategy.validateName("Room123"));
    assertDoesNotThrow(() -> strategy.validateName("!@#$%^&*()"));
  }
  
  @Test
  @DisplayName("validatePassword should accept any password without throwing exceptions")
  void testValidatePassword() {
    // Arrange
    DefaultRoomCredentialValidatedStrategy strategy = new DefaultRoomCredentialValidatedStrategy();
    
    // Act & Assert
    assertDoesNotThrow(() -> strategy.validatePassword(null));
    assertDoesNotThrow(() -> strategy.validatePassword(""));
    assertDoesNotThrow(() -> strategy.validatePassword("password123"));
    assertDoesNotThrow(() -> strategy.validatePassword("P@ssw0rd!"));
    assertDoesNotThrow(() -> strategy.validatePassword("!@#$%^&*()"));
  }
}

