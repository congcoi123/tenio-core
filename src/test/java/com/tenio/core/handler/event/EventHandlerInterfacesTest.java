package com.tenio.core.handler.event;

import com.tenio.common.data.DataCollection;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerLeaveRoomMode;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.define.result.PlayerJoinedRoomResult;
import com.tenio.core.entity.define.result.PlayerLoginResult;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.exception.RefusedConnectionAddressException;
import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import com.tenio.core.entity.define.result.ConnectionEstablishedResult;

class EventHandlerInterfacesTest {

    @Test
    void testEventReceivedMessageFromPlayer() {
        EventReceivedMessageFromPlayer<Player> handler = (player, message) -> {};
        handler.handle(Mockito.mock(Player.class), Mockito.mock(DataCollection.class));
    }

    @Test
    void testEventSendMessageToPlayer() {
        EventSendMessageToPlayer<Player> handler = (player, message) -> {};
        handler.handle(Mockito.mock(Player.class), Mockito.mock(DataCollection.class));
    }

    @Test
    void testEventServerInitialization() {
        EventServerInitialization handler = (serverName, config) -> {};
        handler.handle("server", Mockito.mock(com.tenio.common.configuration.Configuration.class));
    }

    @Test
    void testEventDisconnectPlayer() {
        EventDisconnectPlayer<Player> handler = (player, mode) -> {};
        handler.handle(Mockito.mock(Player.class), PlayerDisconnectMode.IDLE);
    }

    @Test
    void testEventWriteMessageToConnection() {
        EventWriteMessageToConnection handler = (session, packet) -> {};
        handler.handle(Mockito.mock(Session.class), Mockito.mock(Packet.class));
    }

    @Test
    void testEventServerException() {
        EventServerException handler = throwable -> {};
        handler.handle(new Exception("test"));
    }

    @Test
    void testEventBroadcastToChannel() {
        EventBroadcastToChannel<Player> handler = (channel, player, message) -> {};
        handler.handle(Mockito.mock(com.tenio.core.entity.Channel.class), Mockito.mock(Player.class), Mockito.mock(DataCollection.class));
    }

    @Test
    void testEventRoomWillBeRemoved() {
        EventRoomWillBeRemoved<Room> handler = (room, mode) -> {};
        handler.handle(Mockito.mock(Room.class), RoomRemoveMode.WHEN_EMPTY);
    }

    @Test
    void testEventPlayerLoginResult() {
        EventPlayerLoginResult<Player> handler = (player, result) -> {};
        handler.handle(Mockito.mock(Player.class), PlayerLoginResult.SUCCESS);
    }

    @Test
    void testEventPlayerSubscribedChannel() {
        EventPlayerSubscribedChannel<Player> handler = (channel, player) -> {};
        handler.handle(Mockito.mock(com.tenio.core.entity.Channel.class), Mockito.mock(Player.class));
    }

    @Test
    void testEventChannelCreated() {
        EventChannelCreated handler = channel -> {};
        handler.handle(Mockito.mock(com.tenio.core.entity.Channel.class));
    }

    @Test
    void testEventServerTeardown() {
        EventServerTeardown handler = serverName -> {};
        handler.handle("server");
    }

    @Test
    void testEventFetchedCcuInfo() {
        EventFetchedCcuInfo handler = numberPlayers -> {};
        handler.handle(42);
    }

    @Test
    void testEventChannelWillBeRemoved() {
        EventChannelWillBeRemoved handler = channel -> {};
        handler.handle(Mockito.mock(com.tenio.core.entity.Channel.class));
    }

    @Test
    void testEventPlayerJoinedRoomResult() {
        EventPlayerJoinedRoomResult<Player, Room> handler = (player, room, result) -> {};
        handler.handle(Mockito.mock(Player.class), Mockito.mock(Room.class), PlayerJoinedRoomResult.SUCCESS);
    }

    @Test
    void testEventFetchedBandwidthInfo() {
        EventFetchedBandwidthInfo handler = (readBytes, readPackets, readDroppedPackets, writtenBytes, writtenPackets, writtenDroppedPacketsByPolicy, writtenDroppedPacketsByFull) -> {};
        handler.handle(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    @Test
    void testEventPlayerReconnectRequestHandle() {
        EventPlayerReconnectRequestHandle<Player> handler = (session, message) -> Optional.empty();
        assertTrue(handler.handle(Mockito.mock(Session.class), Mockito.mock(DataCollection.class)).isEmpty());
    }

    @Test
    void testEventPlayerBeforeLeaveRoom() {
        EventPlayerBeforeLeaveRoom<Player, Room> handler = (player, room, mode) -> {};
        handler.handle(Mockito.mock(Player.class), Mockito.mock(Room.class), PlayerLeaveRoomMode.SESSION_CLOSED);
    }

    @Test
    void testEventSocketConnectionRefused() {
        EventSocketConnectionRefused handler = (channel, exception) -> {};
        handler.handle(Mockito.mock(SocketChannel.class), Mockito.mock(RefusedConnectionAddressException.class));
    }

    @Test
    void testEventConnectionEstablishedResult() {
        EventConnectionEstablishedResult handler = (session, message, result) -> {};
        handler.handle(Mockito.mock(Session.class), Mockito.mock(DataCollection.class), ConnectionEstablishedResult.SUCCESS);
    }

    @Test
    void testEventPlayerUnsubscribedChannel() {
        EventPlayerUnsubscribedChannel<Player> handler = (channel, player) -> {};
        handler.handle(Mockito.mock(com.tenio.core.entity.Channel.class), Mockito.mock(Player.class));
    }

    @Test
    void testEventWebSocketConnectionRefused() {
        EventWebSocketConnectionRefused handler = (channel, exception) -> {};
        handler.handle(Mockito.mock(Channel.class), Mockito.mock(RefusedConnectionAddressException.class));
    }
} 