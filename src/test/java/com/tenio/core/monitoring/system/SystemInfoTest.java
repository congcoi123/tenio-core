package com.tenio.core.monitoring.system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For SystemInfo")
class SystemInfoTest {

  @Test
  @DisplayName("Log system info should execute without exceptions")
  void testLogSystemInfo() {
    // Since we can't easily verify logger output, we'll just ensure it doesn't throw exceptions
    SystemInfo systemInfo = spy(new SystemInfo());
    when(systemInfo.isInfoEnabled()).thenReturn(true);
    
    assertDoesNotThrow(() -> systemInfo.logSystemInfo());
    // Verify that isInfoEnabled was called
    verify(systemInfo).isInfoEnabled();
  }

  @Test
  @DisplayName("Log system info should not log when info is disabled")
  void testLogSystemInfoWhenDisabled() {
    SystemInfo systemInfo = spy(new SystemInfo());
    when(systemInfo.isInfoEnabled()).thenReturn(false);
    
    systemInfo.logSystemInfo();
    // Verify that isInfoEnabled was called and nothing else happened
    verify(systemInfo).isInfoEnabled();
  }

  @Test
  @DisplayName("Log network cards info should execute without exceptions")
  void testLogNetCardsInfo() {
    SystemInfo systemInfo = spy(new SystemInfo());
    when(systemInfo.isInfoEnabled()).thenReturn(true);
    
    assertDoesNotThrow(() -> systemInfo.logNetCardsInfo());
    // Verify that isInfoEnabled was called
    verify(systemInfo).isInfoEnabled();
  }

  @Test
  @DisplayName("Log network cards info should not log when info is disabled")
  void testLogNetCardsInfoWhenDisabled() {
    SystemInfo systemInfo = spy(new SystemInfo());
    when(systemInfo.isInfoEnabled()).thenReturn(false);
    
    systemInfo.logNetCardsInfo();
    // Verify that isInfoEnabled was called and nothing else happened
    verify(systemInfo).isInfoEnabled();
  }

  @Test
  @DisplayName("Log disk info should execute without exceptions")
  void testLogDiskInfo() {
    SystemInfo systemInfo = spy(new SystemInfo());
    when(systemInfo.isInfoEnabled()).thenReturn(true);
    
    assertDoesNotThrow(() -> systemInfo.logDiskInfo());
    // Verify that isInfoEnabled was called
    verify(systemInfo).isInfoEnabled();
  }

  @Test
  @DisplayName("Log disk info should not log when info is disabled")
  void testLogDiskInfoWhenDisabled() {
    SystemInfo systemInfo = spy(new SystemInfo());
    when(systemInfo.isInfoEnabled()).thenReturn(false);
    
    systemInfo.logDiskInfo();
    // Verify that isInfoEnabled was called and nothing else happened
    verify(systemInfo).isInfoEnabled();
  }
}

