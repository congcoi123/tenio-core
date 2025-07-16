package com.tenio.core.network.zero.codec.decoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.zero.codec.packet.PacketReadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BinaryPacketDecoderImplTest {

  private BinaryPacketDecoderImpl decoder;
  private Session session;

  @BeforeEach
  void setUp() {
    decoder = new BinaryPacketDecoderImpl();
    session = mock(Session.class);
    when(session.getPacketReadState()).thenReturn(PacketReadState.WAIT_NEW_PACKET);
    when(session.getProcessedPacket()).thenReturn(mock(com.tenio.core.network.zero.codec.packet.ProcessedPacket.class));
    when(session.getPendingPacket()).thenReturn(mock(com.tenio.core.network.zero.codec.packet.PendingPacket.class));
  }

  @Test
  void testDecodeNullData() {
    assertDoesNotThrow(() -> decoder.decode(session, null));
  }

  @Test
  void testDecodeValidData() {
    byte[] data = new byte[]{1,2,3};
    assertDoesNotThrow(() -> decoder.decode(session, data));
  }
} 