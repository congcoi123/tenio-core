package com.tenio.core.network.entity.session.manager;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.security.filter.ConnectionFilter;
import io.netty.channel.Channel;
import kcp.Ukcp;

import java.lang.reflect.InvocationTargetException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

/**
 * A mock implementation of the SessionManager interface for testing purposes.
 */
public class MockSessionManager implements SessionManager {

    @Override
    public Iterator<Session> getSessionIterator() {
        return null;
    }

    @Override
    public Session createSocketSession(SocketChannel socketChannel, SelectionKey selectionKey) {
        return null;
    }

    @Override
    public void removeSessionBySocket(SocketChannel socketChannel) {
        // Do nothing
    }

    @Override
    public Session getSessionBySocket(SocketChannel socketChannel) {
        return null;
    }

    @Override
    public void addDatagramForSession(DatagramChannel datagramChannel, int udpConvey, Session session) {
        // Do nothing
    }

    @Override
    public Session getSessionByDatagram(int udpConvey) {
        return null;
    }

    @Override
    public void addKcpForSession(Ukcp kcpChannel, Session session) throws IllegalArgumentException {
        // Do nothing
    }

    @Override
    public Session getSessionByKcp(Ukcp kcpChannel) {
        return null;
    }

    @Override
    public void configureConnectionFilter(ConnectionFilter connectionFilter) {
        // Do nothing
    }

    @Override
    public Session createWebSocketSession(Channel webSocketChannel) {
        return null;
    }

    @Override
    public void removeSessionByWebSocket(Channel webSocketChannel) {
        // Do nothing
    }

    @Override
    public Session getSessionByWebSocket(Channel webSocketChannel) {
        return null;
    }

    @Override
    public void configurePacketQueuePolicy(Class<? extends PacketQueuePolicy> clazz)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        // Do nothing
    }

    @Override
    public void configurePacketQueueSize(int queueSize) {
        // Do nothing
    }

    @Override
    public void removeSession(Session session) {
        // Do nothing
    }

    @Override
    public List<Session> getReadonlySessionsList() {
        return null;
    }

    @Override
    public int getSessionCount() {
        return 0;
    }

    @Override
    public void configureMaxIdleTimeInSeconds(int seconds) {
        // Do nothing
    }

    @Override
    public void emitEvent(ServerEvent event, Object... params) {
        // Do nothing
    }
} 