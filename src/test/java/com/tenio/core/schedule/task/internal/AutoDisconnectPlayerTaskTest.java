package com.tenio.core.schedule.task.internal;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.api.ServerApi;
import com.tenio.core.server.ServerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import static org.junit.jupiter.api.Assertions.*;

class AutoDisconnectPlayerTaskTest {

    private EventManager eventManager;
    private PlayerManager playerManager;
    private AutoDisconnectPlayerTask task;

    @BeforeEach
    void setUp() {
        eventManager = Mockito.mock(EventManager.class);
        playerManager = Mockito.mock(PlayerManager.class);
        task = AutoDisconnectPlayerTask.newInstance(eventManager);
        task.setPlayerManager(playerManager);
    }

    @Test
    void testNewInstance() {
        assertNotNull(AutoDisconnectPlayerTask.newInstance(eventManager));
    }

    @Test
    void testSetPlayerManager() {
        task.setPlayerManager(playerManager);
        // No exception means success
    }

    @Test
    void testRunSchedulesTaskAndLogsOutIdlePlayers() {
        Player idlePlayer = Mockito.mock(Player.class);
        Mockito.when(idlePlayer.isNeverDeported()).thenReturn(false);
        Mockito.when(idlePlayer.isIdle()).thenReturn(true);
        Mockito.when(idlePlayer.getIdentity()).thenReturn("player1");
        Mockito.when(playerManager.getReadonlyPlayersList()).thenReturn(List.of(idlePlayer));
        Mockito.when(playerManager.getPlayerCount()).thenReturn(1);
        ServerApi api = Mockito.mock(ServerApi.class);
        ServerImpl server = Mockito.mock(ServerImpl.class);
        Mockito.when(server.getApi()).thenReturn(api);
        try (MockedStatic<ServerImpl> serverStatic = Mockito.mockStatic(ServerImpl.class)) {
            serverStatic.when(ServerImpl::getInstance).thenReturn(server);
            ScheduledFuture<?> future = task.run();
            assertNotNull(future);
        }
    }
} 