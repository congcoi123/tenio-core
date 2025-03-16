package com.tenio.core.network.entity.session.implement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.Session.AssociatedState;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.zero.codec.packet.PacketReadState;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import kcp.Ukcp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Tests For SessionImpl")
class SessionImplTest {

    private SessionImpl session;
    private PacketQueue packetQueue;
    private Channel webSocketChannel;
    private SocketChannel socketChannel;
    private ConnectionFilter connectionFilter;
    private SessionManager sessionManager;
    private DatagramChannel datagramChannel;
    private Ukcp kcpChannel;

    @BeforeEach
    void setUp() {
        packetQueue = mock(PacketQueue.class);
        webSocketChannel = mock(Channel.class);
        socketChannel = mock(SocketChannel.class);
        connectionFilter = mock(ConnectionFilter.class);
        sessionManager = mock(SessionManager.class);
        datagramChannel = mock(DatagramChannel.class);
        kcpChannel = mock(Ukcp.class);
        
        session = (SessionImpl) SessionImpl.newInstance();
        session.configurePacketQueue(packetQueue);
        session.configureConnectionFilter(connectionFilter);
        session.configureSessionManager(sessionManager);
    }

    @Test
    @DisplayName("New instance should be properly initialized")
    void testNewInstance() {
        assertNotNull(session);
        assertFalse(session.isTcp());
        assertFalse(session.containsUdp());
        assertFalse(session.isWebSocket());
        assertFalse(session.isActivated());
        assertTrue(session.isAssociatedToPlayer(AssociatedState.NONE));
    }

    @Test
    @DisplayName("Session should handle TCP transport type")
    void testTcpTransport() {
        when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
        when(socketChannel.socket().getRemoteSocketAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        session.configureSocketChannel(socketChannel);
        assertTrue(session.isTcp());
        assertFalse(session.containsUdp());
        assertFalse(session.isWebSocket());
    }

    @Test
    @DisplayName("Session should handle WebSocket transport type")
    void testWebSocketTransport() {
        when(webSocketChannel.isActive()).thenReturn(true);
        when(webSocketChannel.remoteAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        session.configureWebSocketChannel(webSocketChannel);
        assertFalse(session.isTcp());
        assertFalse(session.containsUdp());
        assertTrue(session.isWebSocket());
    }

    @Test
    @DisplayName("Session should handle packet queue configuration")
    void testPacketQueueConfiguration() {
        PacketQueue newQueue = mock(PacketQueue.class);
        session.configurePacketQueue(newQueue);
        assertEquals(newQueue, session.fetchPacketQueue());
    }

    @Test
    @DisplayName("Session should handle connection filter")
    void testConnectionFilter() {
        ConnectionFilter newFilter = mock(ConnectionFilter.class);
        session.configureConnectionFilter(newFilter);
        assertNotNull(session);
    }

    @Test
    @DisplayName("Session should handle disconnection")
    void testDisconnection() throws Exception {
        when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
        when(socketChannel.socket().getRemoteSocketAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        session.configureSocketChannel(socketChannel);
        session.close(ConnectionDisconnectMode.UNKNOWN, PlayerDisconnectMode.UNKNOWN);
        verify(socketChannel.socket()).close();
    }

    @Test
    @DisplayName("Session should handle client address and port")
    void testClientAddressAndPort() {
        when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
        when(socketChannel.socket().getRemoteSocketAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        session.configureSocketChannel(socketChannel);
        assertEquals("localhost", session.getClientAddress());
        assertEquals(8080, session.getClientPort());
    }

    @Test
    @DisplayName("Session should handle last activity time")
    void testLastActivityTime() {
        long currentTime = System.currentTimeMillis();
        session.setLastReadTime(currentTime);
        assertEquals(currentTime, session.getLastActivityTime());
    }

    @Test
    @DisplayName("Session should handle null channel disconnection")
    void testNullChannelDisconnection() throws Exception {
        assertDoesNotThrow(() -> session.close(ConnectionDisconnectMode.UNKNOWN, PlayerDisconnectMode.UNKNOWN));
    }

    @Test
    @DisplayName("Session should handle null packet queue")
    void testNullPacketQueue() {
        session.configurePacketQueue(null);
        assertNull(session.fetchPacketQueue());
    }

    @Test
    @DisplayName("Session should handle associated state transitions")
    void testAssociatedStateTransitions() {
        assertTrue(session.isAssociatedToPlayer(AssociatedState.NONE));
        session.setAssociatedToPlayer(AssociatedState.DOING);
        assertTrue(session.isAssociatedToPlayer(AssociatedState.DOING));
        assertTrue(session.transitionAssociatedState(AssociatedState.DOING, AssociatedState.DONE));
        assertTrue(session.isAssociatedToPlayer(AssociatedState.DONE));
    }

    @Test
    @DisplayName("Session should handle orphan state")
    void testOrphanState() {
        assertTrue(session.isOrphan());
        session.setAssociatedToPlayer(AssociatedState.DONE);
        assertFalse(session.isOrphan());
    }

    @Test
    @DisplayName("Session should handle packet read state")
    void testPacketReadState() {
        session.setPacketReadState(PacketReadState.WAIT_NEW_PACKET);
        assertEquals(PacketReadState.WAIT_NEW_PACKET, session.getPacketReadState());
    }

    @Test
    @DisplayName("Session should handle datagram channel configuration")
    void testDatagramChannelConfiguration() {
        when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
        when(socketChannel.socket().getRemoteSocketAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        session.configureSocketChannel(socketChannel);
        session.configureDatagramChannel(datagramChannel, 1);
        assertTrue(session.containsUdp());
        assertEquals(1, session.getUdpConveyId());
        assertEquals(datagramChannel, session.fetchDatagramChannel());
    }

    @Test
    @DisplayName("Session should handle KCP channel configuration")
    void testKcpChannelConfiguration() {
        when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
        when(socketChannel.socket().getRemoteSocketAddress()).thenReturn(new InetSocketAddress("localhost", 8080));
        session.configureSocketChannel(socketChannel);
        session.setKcpChannel(kcpChannel);
        assertTrue(session.containsKcp());
        assertEquals(kcpChannel, session.getKcpChannel());
    }

    @Test
    @DisplayName("Session should handle idle time configuration")
    void testIdleTimeConfiguration() {
        session.configureMaxIdleTimeInSeconds(60);
        session.setLastReadTime(System.currentTimeMillis() - 61000); // 61 seconds ago
        assertTrue(session.isIdle());
    }

    @Test
    @DisplayName("Session should handle activation state")
    void testActivationState() {
        assertFalse(session.isActivated());
        session.activate();
        assertTrue(session.isActivated());
    }

    @Test
    @DisplayName("Session should handle session manager events")
    void testSessionManagerEvents() throws Exception {
        session.remove();
        verify(sessionManager).removeSession(session);
        
        session.activate();
        session.close(ConnectionDisconnectMode.UNKNOWN, PlayerDisconnectMode.UNKNOWN);
        verify(sessionManager).emitEvent(eq(ServerEvent.SESSION_WILL_BE_CLOSED), any(), any(), any());
    }

    @Test
    @DisplayName("Session should handle selection key configuration")
    void testSelectionKeyConfiguration() {
        SelectionKey selectionKey = mock(SelectionKey.class);
        session.configureSelectionKey(selectionKey);
        assertEquals(selectionKey, session.fetchSelectionKey());
    }
}
