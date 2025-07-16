package com.tenio.core.network.utility;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocketUtilityTest {

    @Test
    void testCloseSocketChannelAndSelectionKey() throws IOException {
        SocketChannel socketChannel = mock(SocketChannel.class);
        SelectionKey selectionKey = mock(SelectionKey.class);
        when(socketChannel.isOpen()).thenReturn(true);
        when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
        doNothing().when(socketChannel).close();
        SocketUtility.closeSocket(socketChannel, selectionKey);
        verify(selectionKey).cancel();
        verify(socketChannel).close();
    }

    @Test
    void testCloseSocketChannelNulls() throws IOException {
        // Should not throw
        SocketUtility.closeSocket(null, null);
    }

    @Test
    void testCloseSocketNettyChannel() {
        Channel channel = mock(Channel.class);
        SocketUtility.closeSocket(channel);
        verify(channel).close();
    }

    @Test
    void testCloseSocketNettyChannelNull() {
        // Should not throw
        SocketUtility.closeSocket((Channel) null);
    }

    @Test
    void testCreateReaderBuffer() {
        ByteBuffer buffer = SocketUtility.createReaderBuffer(128);
        assertNotNull(buffer);
        assertTrue(buffer.isDirect());
        assertEquals(128, buffer.capacity());
    }

    @Test
    void testCreateWriterBuffer() {
        ByteBuffer buffer = SocketUtility.createWriterBuffer(256);
        assertNotNull(buffer);
        assertFalse(buffer.isDirect());
        assertEquals(256, buffer.capacity());
    }
} 