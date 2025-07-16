package com.tenio.core.manager;

import com.tenio.core.event.implement.EventManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractManagerTest {
    @Test
    void testConstructorSetsEventManager() {
        EventManager eventManager = mock(EventManager.class);
        AbstractManager manager = new AbstractManager(eventManager) {};
        assertEquals(eventManager, manager.eventManager);
    }
} 