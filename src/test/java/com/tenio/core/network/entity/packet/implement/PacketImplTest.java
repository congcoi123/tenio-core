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

package com.tenio.core.network.entity.packet.implement;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.Session;

@DisplayName("Unit Test Cases For PacketImpl")
class PacketImplTest {

  private PacketImpl packet;

  @BeforeEach
  void setUp() {
    packet = (PacketImpl) PacketImpl.newInstance();
  }

  @Test
  @DisplayName("New instance should create packet with default values")
  void newInstanceShouldCreatePacketWithDefaultValues() {
    assertAll("newInstanceShouldCreatePacketWithDefaultValues",
        () -> assertNotNull(packet),
        () -> assertNotNull(packet.getId()),
        () -> assertNotNull(packet.getCreatedTime()),
        () -> assertEquals(TransportType.UNKNOWN, packet.getTransportType()),
        () -> assertEquals(ResponsePriority.NORMAL, packet.getPriority()),
        () -> assertFalse(packet.isEncrypted()),
        () -> assertFalse(packet.isFragmented()),
        () -> assertFalse(packet.isMarkedAsLast())
    );
  }

  @Test
  @DisplayName("Set data should store binary data")
  void setDataShouldStoreBinaryData() {
    byte[] data = new byte[] {1, 2, 3};
    packet.setData(data);
    
    assertAll("setDataShouldStoreBinaryData",
        () -> assertArrayEquals(data, packet.getData()),
        () -> assertEquals(data.length, packet.getOriginalSize())
    );
  }

  @Test
  @DisplayName("Set transport type should update type")
  void setTransportTypeShouldUpdateType() {
    packet.setTransportType(TransportType.TCP);
    
    assertAll("setTransportTypeShouldUpdateType",
        () -> assertEquals(TransportType.TCP, packet.getTransportType()),
        () -> assertTrue(packet.isTcp()),
        () -> assertFalse(packet.isUdp()),
        () -> assertFalse(packet.isWebSocket())
    );
  }

  @Test
  @DisplayName("Set priority should update priority")
  void setPriorityShouldUpdatePriority() {
    packet.setPriority(ResponsePriority.GUARANTEED_QUICKEST);
    assertEquals(ResponsePriority.GUARANTEED_QUICKEST, packet.getPriority());
  }

  @Test
  @DisplayName("Set encrypted should update encryption status")
  void setEncryptedShouldUpdateEncryptionStatus() {
    packet.setEncrypted(true);
    assertTrue(packet.isEncrypted());
  }

  @Test
  @DisplayName("Set recipients should store session list")
  void setRecipientsShouldStoreSessionList() {
    Collection<Session> recipients = new ArrayList<>();
    packet.setRecipients(recipients);
    assertEquals(recipients, packet.getRecipients());
  }

  @Test
  @DisplayName("Set fragment buffer should update fragmentation status")
  void setFragmentBufferShouldUpdateFragmentationStatus() {
    byte[] fragmentData = new byte[] {4, 5, 6};
    packet.setFragmentBuffer(fragmentData);
    
    assertAll("setFragmentBufferShouldUpdateFragmentationStatus",
        () -> assertArrayEquals(fragmentData, packet.getFragmentBuffer()),
        () -> assertTrue(packet.isFragmented())
    );
  }

  @Test
  @DisplayName("Set marked as last should update last status")
  void setMarkedAsLastShouldUpdateLastStatus() {
    packet.setMarkedAsLast(true);
    assertTrue(packet.isMarkedAsLast());
  }

  @Test
  @DisplayName("Clone should create deep copy")
  void cloneShouldCreateDeepCopy() {
    byte[] data = new byte[] {1, 2, 3};
    packet.setData(data);
    packet.setTransportType(TransportType.TCP);
    packet.setPriority(ResponsePriority.GUARANTEED_QUICKEST);
    packet.setEncrypted(true);
    
    Packet clonedPacket = packet.clone();
    
    assertAll("cloneShouldCreateDeepCopy",
        () -> assertEquals(packet.getId(), clonedPacket.getId()),
        () -> assertEquals(packet.getCreatedTime(), clonedPacket.getCreatedTime()),
        () -> assertArrayEquals(packet.getData(), clonedPacket.getData()),
        () -> assertEquals(packet.getTransportType(), clonedPacket.getTransportType()),
        () -> assertEquals(packet.getPriority(), clonedPacket.getPriority()),
        () -> assertEquals(packet.isEncrypted(), clonedPacket.isEncrypted())
    );
  }

  @Test
  @DisplayName("Equals should compare by ID")
  void equalsShouldCompareById() {
    PacketImpl packet1 = (PacketImpl) PacketImpl.newInstance();
    PacketImpl packet2 = (PacketImpl) PacketImpl.newInstance();
    
    assertAll("equalsShouldCompareById",
        () -> assertEquals(packet1, packet1),
        () -> assertFalse(packet1.equals(packet2)),
        () -> assertFalse(packet1.equals(null)),
        () -> assertFalse(packet1.equals(new Object()))
    );
  }

  @Test
  @DisplayName("Compare to should order by ID")
  void compareToShouldOrderById() {
    PacketImpl packet1 = (PacketImpl) PacketImpl.newInstance();
    PacketImpl packet2 = (PacketImpl) PacketImpl.newInstance();
    
    assertTrue(packet1.compareTo(packet2) < 0);
  }
}

