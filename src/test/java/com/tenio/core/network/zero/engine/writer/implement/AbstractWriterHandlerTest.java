package com.tenio.core.network.zero.engine.writer.implement;

import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.engine.manager.SessionTicketsQueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractWriterHandlerTest {

    private AbstractWriterHandler handler;
    private SessionTicketsQueueManager queueManager;
    private NetworkWriterStatistic writerStatistic;

    @BeforeEach
    void setUp() {
        handler = new AbstractWriterHandler() {
            @Override
            public void send(com.tenio.core.network.entity.packet.PacketQueue packetQueue, com.tenio.core.network.entity.session.Session session, com.tenio.core.network.entity.packet.Packet packet) {
                // no-op for test
            }
        };
        queueManager = mock(SessionTicketsQueueManager.class);
        writerStatistic = mock(NetworkWriterStatistic.class);
    }

    @Test
    void testSetAndGetSessionTicketsQueueManager() {
        BlockingQueue<Session> queue = new LinkedBlockingQueue<>();
        when(queueManager.getQueueByElementId(42L)).thenReturn(queue);
        handler.setSessionTicketsQueueManager(queueManager);
        assertEquals(queue, handler.getSessionTicketsQueue(42L));
    }

    @Test
    void testSetAndGetNetworkWriterStatistic() {
        handler.setNetworkWriterStatistic(writerStatistic);
        assertEquals(writerStatistic, handler.getNetworkWriterStatistic());
    }

    @Test
    void testAllocateAndGetBuffer() {
        handler.allocateBuffer(128);
        ByteBuffer buffer = handler.getBuffer();
        assertNotNull(buffer);
        assertEquals(128, buffer.capacity());
    }
} 