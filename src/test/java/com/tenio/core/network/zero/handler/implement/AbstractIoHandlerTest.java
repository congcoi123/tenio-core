package com.tenio.core.network.zero.handler.implement;

import com.tenio.common.data.DataType;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractIoHandlerTest {

    private AbstractIoHandler handler;
    private EventManager eventManager;
    private SessionManager sessionManager;
    private NetworkReaderStatistic readerStatistic;

    @BeforeEach
    void setUp() {
        eventManager = mock(EventManager.class);
        sessionManager = mock(SessionManager.class);
        readerStatistic = mock(NetworkReaderStatistic.class);
        handler = new AbstractIoHandler(eventManager) {};
    }

    @Test
    void testSetSessionManager() {
        handler.setSessionManager(sessionManager);
        assertEquals(sessionManager, handler.sessionManager);
    }

    @Test
    void testSetNetworkReaderStatistic() {
        handler.setNetworkReaderStatistic(readerStatistic);
        assertEquals(readerStatistic, handler.networkReaderStatistic);
    }

    @Test
    void testSetDataType() {
        handler.setDataType(DataType.ZERO);
        assertEquals(DataType.ZERO, handler.dataType);
    }
} 