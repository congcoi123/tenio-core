package com.tenio.core.monitoring.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For SystemMonitoring")
class SystemMonitoringTest {

  @Test
  @DisplayName("New instance should create a valid SystemMonitoring object")
  void testNewInstance() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    assertNotNull(monitoring, "SystemMonitoring instance should not be null");
    
    // First call to getCpuUsage should return 0.0 as it initializes the baseline counters
    assertEquals(0.0, monitoring.getCpuUsage(), "First call to getCpuUsage should return 0.0");
  }

  @Test
  @DisplayName("Get CPU usage should return a valid percentage")
  void testGetCpuUsage() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    
    // First call initializes baseline counters
    monitoring.getCpuUsage();
    
    // Second call should return a valid CPU usage percentage
    double cpuUsage = monitoring.getCpuUsage();
    assertTrue(cpuUsage >= 0.0 && cpuUsage <= 1.0, 
        "CPU usage should be between 0.0 and 1.0, but was: " + cpuUsage);
  }

  @Test
  @DisplayName("Count running threads should return a positive number")
  void testCountRunningThreads() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    int runningThreads = monitoring.countRunningThreads();
    
    assertTrue(runningThreads > 0, 
        "Running threads count should be positive, but was: " + runningThreads);
  }

  @Test
  @DisplayName("Get total memory should return a positive value")
  void testGetTotalMemory() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    long totalMemory = monitoring.getTotalMemory();
    
    assertTrue(totalMemory > 0, 
        "Total memory should be positive, but was: " + totalMemory);
  }

  @Test
  @DisplayName("Get free memory should return a non-negative value")
  void testGetFreeMemory() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    long freeMemory = monitoring.getFreeMemory();
    
    assertTrue(freeMemory >= 0, 
        "Free memory should be non-negative, but was: " + freeMemory);
  }

  @Test
  @DisplayName("Get used memory should return a positive value")
  void testGetUsedMemory() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    long usedMemory = monitoring.getUsedMemory();
    
    assertTrue(usedMemory > 0, 
        "Used memory should be positive, but was: " + usedMemory);
  }
  
  @Test
  @DisplayName("Memory values should be consistent")
  void testMemoryValuesConsistency() {
    SystemMonitoring monitoring = SystemMonitoring.newInstance();
    long totalMemory = monitoring.getTotalMemory();
    long usedMemory = monitoring.getUsedMemory();
    long freeMemoryFromRuntime = Runtime.getRuntime().freeMemory();
    
    assertEquals(totalMemory - freeMemoryFromRuntime, usedMemory, 
        "Used memory should be equal to total memory minus free memory from Runtime");
  }
}

