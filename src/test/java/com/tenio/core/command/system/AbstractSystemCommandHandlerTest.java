package com.tenio.core.command.system;

import com.tenio.core.bootstrap.annotation.SystemCommand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

@SystemCommand(label = "TEST", description = "desc", usage = {"usage1", "usage2"}, isBackgroundRunning = true)
class TestSystemCommandHandler extends AbstractSystemCommandHandler {
    @Override
    public void execute(List<String> arguments) {}
}

class AbstractSystemCommandHandlerTest {
    @Test
    void testAnnotationBasedMethods() {
        TestSystemCommandHandler handler = new TestSystemCommandHandler();
        assertEquals("TEST", handler.getLabel());
        assertEquals("desc", handler.getDescription());
        assertArrayEquals(new String[]{"usage1", "usage2"}, handler.getUsage());
        assertTrue(handler.isRunningBackground());
    }

    @Test
    void testSetAndGetCommandManager() {
        TestSystemCommandHandler handler = new TestSystemCommandHandler();
        SystemCommandManager manager = mock(SystemCommandManager.class);
        handler.setCommandManager(manager);
        assertEquals(manager, handler.getCommandManager());
    }

    @Test
    void testSetCommandManagerNullThrows() {
        TestSystemCommandHandler handler = new TestSystemCommandHandler();
        assertThrows(IllegalArgumentException.class, () -> handler.setCommandManager(null));
    }

    @Test
    void testGetCommandManagerNotSetThrows() {
        TestSystemCommandHandler handler = new TestSystemCommandHandler();
        assertThrows(IllegalStateException.class, handler::getCommandManager);
    }

    @Test
    void testToString() {
        TestSystemCommandHandler handler = new TestSystemCommandHandler();
        assertTrue(handler.toString().contains("TEST"));
        assertTrue(handler.toString().contains("desc"));
        assertTrue(handler.toString().contains("usage1"));
    }
} 