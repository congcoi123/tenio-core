/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.codec.decoder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tenio.common.data.DataType;
import com.tenio.core.network.codec.compression.BinaryPacketCompressor;
import com.tenio.core.network.codec.encryption.BinaryPacketEncryptor;
import com.tenio.core.network.codec.packet.PacketHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.msgpack.core.MessageTypeException;

@DisplayName("Unit Test Cases For BinaryPacketDecoderImpl")
class BinaryPacketDecoderImplTest {

  private BinaryPacketDecoderImpl decoder;

  @BeforeEach
  void setUp() {
    decoder = new BinaryPacketDecoderImpl();
  }

  @Test
  @DisplayName("Decode null data should throw an exception")
  void testDecodeNullData() {
    assertThrows(NullPointerException.class, () -> decoder.decode(null));
  }

  @Test
  @DisplayName("Decode invalid data should throw an exception")
  void testDecodeInvalidData() {
    byte[] binaries = new byte[] {1, 2, 3};
    assertThrows(MessageTypeException.class, () -> decoder.decode(binaries));
  }

  @Test
  @DisplayName("decode(PacketHeader, null) returns null")
  void testDecodeWithPacketHeaderAndNullBinariesReturnsNull() {
    PacketHeader header = PacketHeader.newInstance(true, false, false, false, DataType.ZERO);
    assertNull(decoder.decode(header, null));
  }

  @Test
  @DisplayName("decode(PacketHeader, empty array) returns null")
  void testDecodeWithPacketHeaderAndEmptyBinariesReturnsNull() {
    PacketHeader header = PacketHeader.newInstance(true, false, false, false, DataType.ZERO);
    assertNull(decoder.decode(header, new byte[0]));
  }

  @Test
  @DisplayName("decode with compressed=true and no compressor throws IllegalStateException")
  void testDecodeCompressedWithoutCompressorThrows() {
    PacketHeader header = PacketHeader.newInstance(true, true, false, false, DataType.ZERO);
    assertThrows(IllegalStateException.class, () -> decoder.decode(header, new byte[]{1, 2, 3}));
  }

  @Test
  @DisplayName("decode with encrypted=true and no encryptor throws IllegalStateException")
  void testDecodeEncryptedWithoutEncryptorThrows() {
    PacketHeader header = PacketHeader.newInstance(true, false, false, true, DataType.ZERO);
    assertThrows(IllegalStateException.class, () -> decoder.decode(header, new byte[]{1, 2, 3}));
  }

  @Test
  @DisplayName("setCompressor stores the compressor for use in decode")
  void testSetCompressorIsUsedDuringDecode() {
    BinaryPacketCompressor compressor = mock(BinaryPacketCompressor.class);
    when(compressor.uncompress(new byte[]{1, 2, 3})).thenReturn(new byte[]{1, 2, 3});

    decoder.setCompressor(compressor);
    PacketHeader header = PacketHeader.newInstance(true, true, false, false, DataType.ZERO);

    // The uncompressed bytes go to DataUtility, which may throw - we just verify the compressor
    // is invoked; the downstream exception is expected
    assertThrows(Exception.class, () -> decoder.decode(header, new byte[]{1, 2, 3}));
    verify(compressor).uncompress(new byte[]{1, 2, 3});
  }

  @Test
  @DisplayName("setEncryptor stores the encryptor for use in decode")
  void testSetEncryptorIsUsedDuringDecode() {
    BinaryPacketEncryptor encryptor = mock(BinaryPacketEncryptor.class);
    when(encryptor.decrypt(new byte[]{1, 2, 3})).thenReturn(new byte[]{1, 2, 3});

    decoder.setEncryptor(encryptor);
    PacketHeader header = PacketHeader.newInstance(true, false, false, true, DataType.ZERO);

    // The decrypted bytes go to DataUtility, which may throw - we just verify the encryptor
    // is invoked; the downstream exception is expected
    assertThrows(Exception.class, () -> decoder.decode(header, new byte[]{1, 2, 3}));
    verify(encryptor).decrypt(new byte[]{1, 2, 3});
  }
}
