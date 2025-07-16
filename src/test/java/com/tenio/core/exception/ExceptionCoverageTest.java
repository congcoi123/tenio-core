package com.tenio.core.exception;

import com.tenio.core.command.system.AbstractSystemCommandHandler;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.result.PlayerJoinedRoomResult;
import com.tenio.core.entity.define.result.RoomCreatedResult;
import com.tenio.core.entity.define.result.SwitchedPlayerRoleInRoomResult;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.packet.PacketQueue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionCoverageTest {

    @Test
    void testPacketEncryptorException() {
        PacketEncryptorException ex = new PacketEncryptorException("encryption error");
        assertEquals("encryption error", ex.getMessage());
    }

    @Test
    void testConfigurationException() {
        ConfigurationException ex = new ConfigurationException("config error");
        assertEquals("config error", ex.getMessage());
    }

    @Test
    void testPacketCompressorException() {
        PacketCompressorException ex = new PacketCompressorException("compress error");
        assertEquals("compress error", ex.getMessage());
    }

    @Test
    void testServiceRuntimeException() {
        ServiceRuntimeException ex = new ServiceRuntimeException("service error");
        assertEquals("service error", ex.getMessage());
    }

    @Test
    void testNoImplementedClassFoundException() {
        NoImplementedClassFoundException ex = new NoImplementedClassFoundException(String.class);
        assertTrue(ex.getMessage().contains("Unable to find any implementation for the class: java.lang.String"));
    }

    @Test
    void testNotDefinedSubscribersException() {
        NotDefinedSubscribersException ex = new NotDefinedSubscribersException(String.class, Integer.class);
        assertTrue(ex.getMessage().contains("Need to implement interfaces: java.lang.String, java.lang.Integer"));
    }

    @Test
    void testChannelNotExistException() {
        ChannelNotExistException ex = new ChannelNotExistException();
        assertNull(ex.getMessage());
    }

    @Test
    void testAddedDuplicatedCommandException() {
        AbstractSystemCommandHandler handler = new AbstractSystemCommandHandler() {
            @Override
            public void execute(java.util.List<String> arguments) {}
        };
        AddedDuplicatedCommandException ex = new AddedDuplicatedCommandException("label", handler);
        assertTrue(ex.getMessage().contains("Unable to add label {label}, it already exists"));
    }

    @Test
    void testCreatedRoomException() {
        CreatedRoomException ex = new CreatedRoomException("room error", RoomCreatedResult.INVALID_NAME_OR_PASSWORD);
        assertEquals("room error", ex.getMessage());
        assertEquals(RoomCreatedResult.INVALID_NAME_OR_PASSWORD, ex.getResult());
    }

    @Test
    void testRefusedConnectionAddressException() {
        RefusedConnectionAddressException ex = new RefusedConnectionAddressException("reason", "127.0.0.1");
        assertTrue(ex.getMessage().contains("reason : 127.0.0.1"));
    }

    @Test
    void testAddedDuplicatedRoomException() {
        Room room = org.mockito.Mockito.mock(Room.class);
        org.mockito.Mockito.when(room.toString()).thenReturn("room1");
        AddedDuplicatedRoomException ex = new AddedDuplicatedRoomException(room);
        assertTrue(ex.getMessage().contains("Unable to add room: room1, it already exists"));
    }

    @Test
    void testPacketQueuePolicyViolationException() {
        Packet packet = org.mockito.Mockito.mock(Packet.class);
        org.mockito.Mockito.when(packet.toString()).thenReturn("packet1");
        PacketQueuePolicyViolationException ex = new PacketQueuePolicyViolationException(packet, 75.5f);
        assertTrue(ex.getMessage().contains("Dropped packet: [packet1], current packet queue usage: 75.500000%"));
    }

    @Test
    void testPlayerJoinedRoomException() {
        PlayerJoinedRoomException ex = new PlayerJoinedRoomException("join error", PlayerJoinedRoomResult.DUPLICATED_PLAYER);
        assertEquals("join error", ex.getMessage());
        assertEquals(PlayerJoinedRoomResult.DUPLICATED_PLAYER, ex.getResult());
    }

    @Test
    void testAddedDuplicatedPlayerException() {
        Player player = org.mockito.Mockito.mock(Player.class);
        org.mockito.Mockito.when(player.getIdentity()).thenReturn("player1");
        AddedDuplicatedPlayerException ex = new AddedDuplicatedPlayerException(player);
        assertTrue(ex.getMessage().contains("Unable to add player: player1, it already exists"));
        assertEquals(player, ex.getPlayer());
    }

    @Test
    void testDuplicatedBeanCreationException() throws Exception {
        DuplicatedBeanCreationException ex = new DuplicatedBeanCreationException(String.class, "beanName");
        assertTrue(ex.getMessage().contains("Duplicated bean creation with type: String, and name: beanName"));
    }

    @Test
    void testCreatedDuplicatedChannelException() {
        CreatedDuplicatedChannelException ex = new CreatedDuplicatedChannelException("channel1");
        assertTrue(ex.getMessage().contains("Unable to create channel with id: channel1, it already exists"));
    }

    @Test
    void testRequestQueueFullException() {
        RequestQueueFullException ex = new RequestQueueFullException(10);
        assertTrue(ex.getMessage().contains("Reached max queue size, the request was dropped. The current size: 10"));
    }

    @Test
    void testSwitchedPlayerRoleInRoomException() {
        SwitchedPlayerRoleInRoomException ex = new SwitchedPlayerRoleInRoomException("switch error", SwitchedPlayerRoleInRoomResult.PLAYER_WAS_NOT_IN_ROOM);
        assertEquals("switch error", ex.getMessage());
        assertEquals(SwitchedPlayerRoleInRoomResult.PLAYER_WAS_NOT_IN_ROOM, ex.getResult());
    }

    @Test
    void testAddedDuplicatedClientCommandException() {
        com.tenio.core.command.client.AbstractClientCommandHandler<Player> handler = new com.tenio.core.command.client.AbstractClientCommandHandler<>() {
            @Override
            public void execute(Player player, com.tenio.common.data.DataCollection message) {}
        };
        AddedDuplicatedClientCommandException ex = new AddedDuplicatedClientCommandException((short)1, handler);
        assertTrue(ex.getMessage().contains("Unable to add label {1}, it already exists"));
    }

    @Test
    void testIllegalDefinedAccessControlException() {
        IllegalDefinedAccessControlException ex = new IllegalDefinedAccessControlException();
        assertNull(ex.getMessage());
    }

    @Test
    void testPacketQueueFullException() {
        PacketQueueFullException ex = new PacketQueueFullException(5);
        assertTrue(ex.getMessage().contains("Reached max queue size, the packet was dropped. The current size: 5"));
    }

    @Test
    void testInvalidRestMappingClassException() {
        InvalidRestMappingClassException ex = new InvalidRestMappingClassException();
        assertTrue(ex.getMessage().contains("Invalid RestMapping class, it should return an instance of jakarta.servlet.http.HttpServlet."));
    }
} 