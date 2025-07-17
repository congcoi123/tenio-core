package com.tenio.core.utility;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CommandUtilityTest {

  @Test
  void testAllEnumValues() {
    for (CommandUtility util : CommandUtility.values()) {
      assertEquals(util.name(), util.toString());
    }
  }

  @Test
  void testShowConsoleMessage() {
    assertDoesNotThrow(() -> CommandUtility.INSTANCE.showConsoleMessage("test message"));
  }
} 