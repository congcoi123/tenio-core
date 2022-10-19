package com.tenio.core.network.entity.packet.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import org.junit.jupiter.api.Test;

class PacketQueueImplTest {
  @Test
  void testNewInstance() {
    PacketQueueImpl actualNewInstanceResult = PacketQueueImpl.newInstance();
    actualNewInstanceResult.setMaxSize(3);
    actualNewInstanceResult.setPacketQueuePolicy(mock(PacketQueuePolicy.class));
    assertEquals(3, actualNewInstanceResult.getMaxSize());
    assertEquals(0, actualNewInstanceResult.getSize());
    assertEquals("PacketQueue{queue=[], packetQueuePolicy=Mock for PacketQueuePolicy, hashCode: 1124360095, maxSize=3, size=0}", actualNewInstanceResult.toString());
  }

  @Test
  void testGetPercentageUsed() {
    assertEquals(0.0f, PacketQueueImpl.newInstance().getPercentageUsed());
  }

  @Test
  void testGetPercentageUsed2() {
    PacketQueueImpl newInstanceResult = PacketQueueImpl.newInstance();
    newInstanceResult.setMaxSize(3);
    assertEquals(0.0f, newInstanceResult.getPercentageUsed());
  }

  @Test
  void testClear() {
    PacketQueueImpl newInstanceResult = PacketQueueImpl.newInstance();
    newInstanceResult.clear();
    assertEquals("PacketQueue{queue=[], packetQueuePolicy=null, maxSize=0, size=0}", newInstanceResult.toString());
    assertTrue(newInstanceResult.isEmpty());
  }
}

