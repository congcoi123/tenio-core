package com.tenio.core.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * A simple test for command managers without using any network components.
 */
public class SimpleCommandManagerTest {

    private Map<String, CommandHandler> systemCommands;
    private Map<Short, CommandHandler> clientCommands;

    @Before
    public void setUp() {
        systemCommands = new HashMap<>();
        clientCommands = new HashMap<>();
    }

    @Test
    public void testSystemCommandRegistration() {
        // Register a command
        CommandHandler handler = new TestCommandHandler();
        systemCommands.put("test", handler);
        
        // Verify it was registered
        assertEquals(1, systemCommands.size());
        assertNotNull(systemCommands.get("test"));
    }

    @Test
    public void testSystemCommandRetrieval() {
        // Register a command
        CommandHandler handler = new TestCommandHandler();
        systemCommands.put("test", handler);
        
        // Retrieve and verify
        CommandHandler retrievedHandler = systemCommands.get("test");
        assertNotNull(retrievedHandler);
        assertEquals(handler, retrievedHandler);
    }

    @Test
    public void testClientCommandRegistration() {
        // Register a command
        CommandHandler handler = new TestCommandHandler();
        clientCommands.put((short) 1, handler);
        
        // Verify it was registered
        assertEquals(1, clientCommands.size());
        assertNotNull(clientCommands.get((short) 1));
    }

    @Test
    public void testClientCommandRetrieval() {
        // Register a command
        CommandHandler handler = new TestCommandHandler();
        clientCommands.put((short) 1, handler);
        
        // Retrieve and verify
        CommandHandler retrievedHandler = clientCommands.get((short) 1);
        assertNotNull(retrievedHandler);
        assertEquals(handler, retrievedHandler);
    }

    @Test
    public void testCommandManagerClearing() {
        // Register commands
        CommandHandler handler = new TestCommandHandler();
        systemCommands.put("test", handler);
        clientCommands.put((short) 1, handler);
        
        // Verify they were registered
        assertEquals(1, systemCommands.size());
        assertEquals(1, clientCommands.size());
        
        // Clear the commands
        systemCommands.clear();
        clientCommands.clear();
        
        // Verify they were cleared
        assertEquals(0, systemCommands.size());
        assertEquals(0, clientCommands.size());
        assertNull(systemCommands.get("test"));
        assertNull(clientCommands.get((short) 1));
    }

    /**
     * A simple command handler for testing.
     */
    private static class TestCommandHandler implements CommandHandler {
        @Override
        public void execute(Object... params) {
            // Do nothing, this is just for testing
        }
    }

    /**
     * Simple interface for command handlers.
     */
    private interface CommandHandler {
        void execute(Object... params);
    }
} 