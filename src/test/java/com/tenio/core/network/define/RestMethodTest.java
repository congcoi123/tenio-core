package com.tenio.core.network.define;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RestMethodTest {

  @Test
  void testAllEnumValues() {
    for (RestMethod method : RestMethod.values()) {
      assertNotNull(method.getValue());
      assertEquals(method, RestMethod.getByValue(method.getValue()));
      assertEquals(method.name(), method.toString());
    }
  }

  @Test
  void testGetByValueWithUnknown() {
    assertNull(RestMethod.getByValue("not-a-method"));
  }
} 