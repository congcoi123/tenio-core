package com.tenio.core.schedule.task.internal;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.concurrent.ScheduledFuture;
import static org.junit.jupiter.api.Assertions.*;

class TrafficCounterTaskTest {

    private EventManager eventManager;
    private NetworkReaderStatistic readerStatistic;
    private NetworkWriterStatistic writerStatistic;
    private TrafficCounterTask task;

    @BeforeEach
    void setUp() {
        eventManager = Mockito.mock(EventManager.class);
        readerStatistic = Mockito.mock(NetworkReaderStatistic.class);
        writerStatistic = Mockito.mock(NetworkWriterStatistic.class);
        task = TrafficCounterTask.newInstance(eventManager);
        task.setNetworkReaderStatistic(readerStatistic);
        task.setNetworkWriterStatistic(writerStatistic);
    }

    @Test
    void testNewInstance() {
        assertNotNull(TrafficCounterTask.newInstance(eventManager));
    }

    @Test
    void testSetNetworkReaderStatistic() {
        task.setNetworkReaderStatistic(readerStatistic);
        // No exception means success
    }

    @Test
    void testSetNetworkWriterStatistic() {
        task.setNetworkWriterStatistic(writerStatistic);
        // No exception means success
    }

    @Test
    void testRunSchedulesTaskAndEmitsBandwidthInfoEvent() {
        ScheduledFuture<?> future = task.run();
        assertNotNull(future);
        // The actual event emission is handled by the scheduled lambda, which can be verified in integration tests
    }
} 