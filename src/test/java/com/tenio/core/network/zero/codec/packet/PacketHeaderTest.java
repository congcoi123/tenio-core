/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.network.zero.codec.packet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For PacketHeader")
class PacketHeaderTest {

  @Test
  @DisplayName("New instance should create header with all flags false")
  void newInstanceShouldCreateHeaderWithAllFlagsFalse() {
    var header = PacketHeader.newInstance(false, false, false, false);
    
    assertAll("newInstanceShouldCreateHeaderWithAllFlagsFalse",
        () -> assertNotNull(header),
        () -> assertFalse(header.isBinary()),
        () -> assertFalse(header.isCompressed()),
        () -> assertFalse(header.isBigSized()),
        () -> assertFalse(header.isEncrypted())
    );
  }

  @Test
  @DisplayName("New instance should create header with all flags true")
  void newInstanceShouldCreateHeaderWithAllFlagsTrue() {
    var header = PacketHeader.newInstance(true, true, true, true);
    
    assertAll("newInstanceShouldCreateHeaderWithAllFlagsTrue",
        () -> assertNotNull(header),
        () -> assertTrue(header.isBinary()),
        () -> assertTrue(header.isCompressed()),
        () -> assertTrue(header.isBigSized()),
        () -> assertTrue(header.isEncrypted())
    );
  }

  @Test
  @DisplayName("New instance should create header with mixed flags")
  void newInstanceShouldCreateHeaderWithMixedFlags() {
    var header = PacketHeader.newInstance(true, false, true, false);
    
    assertAll("newInstanceShouldCreateHeaderWithMixedFlags",
        () -> assertNotNull(header),
        () -> assertTrue(header.isBinary()),
        () -> assertFalse(header.isCompressed()),
        () -> assertTrue(header.isBigSized()),
        () -> assertFalse(header.isEncrypted())
    );
  }

  @Test
  @DisplayName("ToString should include all flag values")
  void toStringShouldIncludeAllFlagValues() {
    var header = PacketHeader.newInstance(true, false, true, false);
    var expected = "PacketHeader{binary=true, compressed=false, bigSized=true, encrypted=false}";
    
    assertEquals(expected, header.toString());
  }
} 