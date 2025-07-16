package com.tenio.core.network.zero.codec.packet;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PacketHeaderTypeTest {

  @Test
  void testAllEnumValues() {
    for (PacketHeaderType type : PacketHeaderType.values()) {
      assertEquals(type.name(), type.toString());
    }
  }
} 