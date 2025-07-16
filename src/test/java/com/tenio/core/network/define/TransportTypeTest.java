package com.tenio.core.network.define;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TransportTypeTest {

  @Test
  void testAllEnumValues() {
    for (TransportType type : TransportType.values()) {
      assertNotNull(type.getValue());
      assertEquals(type, TransportType.getByValue(type.getValue()));
      assertEquals(type.name(), type.toString());
    }
  }

  @Test
  void testGetByValueWithUnknown() {
    assertEquals(TransportType.UNKNOWN, TransportType.getByValue("unknown"));
    assertNull(TransportType.getByValue("not-a-type"));
  }
} 