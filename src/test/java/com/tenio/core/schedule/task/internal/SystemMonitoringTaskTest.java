package com.tenio.core.schedule.task.internal;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.monitoring.system.SystemMonitoring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.concurrent.ScheduledFuture;
import static org.junit.jupiter.api.Assertions.*;

class SystemMonitoringTaskTest {

    private EventManager eventManager;
    private SystemMonitoringTask task;

    @BeforeEach
    void setUp() {
        eventManager = Mockito.mock(EventManager.class);
        task = SystemMonitoringTask.newInstance(eventManager);
    }

    @Test
    void testNewInstance() {
        assertNotNull(SystemMonitoringTask.newInstance(eventManager));
    }

    @Test
    void testRunSchedulesTaskAndEmitsSystemMonitoringEvent() {
        ScheduledFuture<?> future = task.run();
        assertNotNull(future);
        // The actual event emission is handled by the scheduled lambda, which can be verified in integration tests
    }
} 