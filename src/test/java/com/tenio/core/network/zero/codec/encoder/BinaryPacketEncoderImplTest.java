package com.tenio.core.network.zero.codec.encoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.network.entity.packet.Packet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BinaryPacketEncoderImplTest {

  private BinaryPacketEncoderImpl encoder;

  @BeforeEach
  void setUp() {
    encoder = new BinaryPacketEncoderImpl();
  }

  @Test
  void testEncodeNullPacket() {
    assertThrows(NullPointerException.class, () -> encoder.encode(null));
  }

  @Test
  void testEncodeValidPacket() {
    Packet packet = mock(Packet.class);
    when(packet.getData()).thenReturn(new byte[]{1,2,3});
    assertNotNull(encoder.encode(packet));
  }
} 