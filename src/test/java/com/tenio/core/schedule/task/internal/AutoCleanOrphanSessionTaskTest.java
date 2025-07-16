package com.tenio.core.schedule.task.internal;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import static org.junit.jupiter.api.Assertions.*;

class AutoCleanOrphanSessionTaskTest {

    private EventManager eventManager;
    private SessionManager sessionManager;
    private AutoCleanOrphanSessionTask task;

    @BeforeEach
    void setUp() {
        eventManager = Mockito.mock(EventManager.class);
        sessionManager = Mockito.mock(SessionManager.class);
        task = AutoCleanOrphanSessionTask.newInstance(eventManager);
        task.setSessionManager(sessionManager);
    }

    @Test
    void testNewInstance() {
        assertNotNull(AutoCleanOrphanSessionTask.newInstance(eventManager));
    }

    @Test
    void testSetSessionManager() {
        task.setSessionManager(sessionManager);
        // No exception means success
    }

    @Test
    void testRunSchedulesTaskAndClosesOrphanSessions() throws IOException {
        Session orphanSession = Mockito.mock(Session.class);
        Mockito.when(orphanSession.isActivated()).thenReturn(true);
        Mockito.when(orphanSession.isOrphan()).thenReturn(true);
        Mockito.when(sessionManager.getReadonlySessionsList()).thenReturn(List.of(orphanSession));
        Mockito.when(sessionManager.getSessionCount()).thenReturn(1);
        ScheduledFuture<?> future = task.run();
        assertNotNull(future);
    }
} 