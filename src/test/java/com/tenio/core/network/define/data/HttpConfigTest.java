package com.tenio.core.network.define.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HttpConfigTest {
  @Test
  void testConstructor() {
    HttpConfig actualHttpConfig = new HttpConfig("https://example.org/example", 8080);

    assertEquals("https://example.org/example", actualHttpConfig.name());
    assertEquals(8080, actualHttpConfig.port());
    assertEquals("HttpConfig[name=https://example.org/example, port=8080]",
        actualHttpConfig.toString());
  }
}
