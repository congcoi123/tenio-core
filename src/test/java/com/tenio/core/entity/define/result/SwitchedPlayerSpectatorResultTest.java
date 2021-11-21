package com.tenio.core.entity.define.result;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwitchedPlayerSpectatorResultTest {
  @Test
  void testValueOf() {
    assertEquals("SUCCESS", SwitchedPlayerSpectatorResult.valueOf("SUCCESS").toString());
  }
}

