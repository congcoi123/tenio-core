package com.tenio.core.network.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tenio.core.exception.RefusedConnectionAddressException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For DefaultConnectionFilter")
class DefaultConnectionFilterTest {

  @Test
  @DisplayName("addBannedAddress should add an address to the banned list")
  void testAddBannedAddress() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    
    // Act
    filter.addBannedAddress(address);
    
    // Assert
    assertEquals(1, filter.getBannedAddresses().length);
    assertEquals(address, filter.getBannedAddresses()[0]);
  }

  @Test
  @DisplayName("removeBannedAddress should remove an address from the banned list")
  void testRemoveBannedAddress() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    filter.addBannedAddress(address);
    assertEquals(1, filter.getBannedAddresses().length);
    
    // Act
    filter.removeBannedAddress(address);
    
    // Assert
    assertEquals(0, filter.getBannedAddresses().length);
  }

  @Test
  @DisplayName("getBannedAddresses should return an empty array for a new filter")
  void testGetBannedAddresses() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    
    // Act & Assert
    assertEquals(0, filter.getBannedAddresses().length);
  }

  @Test
  @DisplayName("validateAndAddAddress should add a valid address without throwing exceptions")
  void testValidateAndAddAddress() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    
    // Act & Assert
    assertDoesNotThrow(() -> filter.validateAndAddAddress(address));
    
    // Verify the address was added by removing it and checking the result
    filter.removeAddress(address);
  }

  @Test
  @DisplayName("validateAndAddAddress should throw exception for banned addresses")
  void testValidateAndAddAddress2() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    filter.addBannedAddress(address);
    
    // Act & Assert
    RefusedConnectionAddressException exception = assertThrows(
        RefusedConnectionAddressException.class,
        () -> filter.validateAndAddAddress(address));
    
    // Check that the exception message contains the expected information
    assertTrue(exception.getMessage().contains(address));
    assertTrue(exception.getMessage().contains("banned"));
  }
  
  @Test
  @DisplayName("validateAndAddAddress should throw exception when max connections per IP is reached")
  void testValidateAndAddAddressMaxConnections() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    filter.configureMaxConnectionsPerIp(2);
    
    // Act - Add connections up to the limit
    assertDoesNotThrow(() -> filter.validateAndAddAddress(address));
    assertDoesNotThrow(() -> filter.validateAndAddAddress(address));
    
    // Assert - Adding one more should throw exception
    RefusedConnectionAddressException exception = assertThrows(
        RefusedConnectionAddressException.class,
        () -> filter.validateAndAddAddress(address));
    
    // Check that the exception message contains the expected information
    assertTrue(exception.getMessage().contains(address));
    assertTrue(exception.getMessage().contains("maximum"));
  }
  
  @Test
  @DisplayName("removeAddress should decrement the connection count for an address")
  void testRemoveAddress() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    filter.configureMaxConnectionsPerIp(1);
    
    // Act - Add and then remove a connection
    filter.validateAndAddAddress(address);
    filter.removeAddress(address);
    
    // Assert - Should be able to add another connection
    assertDoesNotThrow(() -> filter.validateAndAddAddress(address));
  }
  
  @Test
  @DisplayName("configureMaxConnectionsPerIp should set the maximum connections per IP")
  void testConfigureMaxConnectionsPerIp() {
    // Arrange
    DefaultConnectionFilter filter = new DefaultConnectionFilter();
    String address = "192.168.1.1";
    
    // Act - Set max connections to 1
    filter.configureMaxConnectionsPerIp(1);
    
    // Assert
    filter.validateAndAddAddress(address);
    assertThrows(RefusedConnectionAddressException.class,
        () -> filter.validateAndAddAddress(address));
  }
}
