package com.tenio.core.network.zero.codec.packet;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PacketReadStateTest {

  @Test
  void testAllEnumValues() {
    for (PacketReadState state : PacketReadState.values()) {
      assertEquals(state.name(), state.toString());
    }
  }
} 