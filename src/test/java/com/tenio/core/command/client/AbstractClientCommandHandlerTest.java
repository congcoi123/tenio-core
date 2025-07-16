package com.tenio.core.command.client;

import com.tenio.common.data.DataCollection;
import com.tenio.core.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestClientCommandHandler extends AbstractClientCommandHandler<Player> {
    @Override
    public void execute(Player player, DataCollection message) {}
}

class AbstractClientCommandHandlerTest {
    @Test
    void testSetAndGetCommandManager() {
        TestClientCommandHandler handler = new TestClientCommandHandler();
        ClientCommandManager manager = mock(ClientCommandManager.class);
        handler.setCommandManager(manager);
        assertEquals(manager, handler.getCommandManager());
    }
} 