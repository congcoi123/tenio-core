/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.server;

import com.tenio.common.configuration.Configuration;
import com.tenio.core.bootstrap.BootstrapHandler;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.client.ClientCommandPolicy;
import com.tenio.core.configuration.define.CoreConfigurationType;
import com.tenio.core.network.codec.compression.BinaryPacketCompressor;
import com.tenio.core.network.codec.encryption.BinaryPacketEncryptor;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.define.TransportType;
import com.tenio.core.network.entity.inbound.policy.RequestPolicy;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.zero.engine.reader.policy.DatagramPacketPolicy;
import com.tenio.core.network.zero.engine.reader.policy.DefaultDatagramPacketPolicy;
import com.tenio.core.recovery.RecoveryPolicy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerExtensionPointLoggerTest {

    private final Configuration configuration = mock(Configuration.class);
    private final BootstrapHandler bootstrap = mock(BootstrapHandler.class);
    private final ClientCommandManager commands = mock(ClientCommandManager.class);
    private final ServerExtensionPointLogger logger = new ServerExtensionPointLogger();
    private LoggerContext context;
    private LoggerConfig previous;
    private LoggerConfig capture;
    private RecordingAppender appender;

    private static SocketConfiguration udp(int port) {
        return new SocketConfiguration("udp", TransportType.UDP, port, 1);
    }

    @BeforeEach
    void captureStartupLogs() {
        context = (LoggerContext) LogManager.getContext(false);
        var config = context.getConfiguration();
        previous = config.getLoggers().get(ServerExtensionPointLogger.class.getName());
        appender = new RecordingAppender();
        appender.start();
        capture = new LoggerConfig(ServerExtensionPointLogger.class.getName(), Level.INFO, false);
        capture.addAppender(appender, Level.ALL, null);
        config.removeLogger(ServerExtensionPointLogger.class.getName());
        config.addLogger(ServerExtensionPointLogger.class.getName(), capture);
        context.updateLoggers();
        when(commands.getHandlers()).thenReturn(Map.of());
    }

    @AfterEach
    void restoreLogging() {
        var config = context.getConfiguration();
        config.removeLogger(ServerExtensionPointLogger.class.getName());
        if (previous != null) {
            config.addLogger(ServerExtensionPointLogger.class.getName(), previous);
        }
        context.updateLoggers();
        appender.stop();
    }

    @Test
    void infoListsDefaultsInactiveServicesAndOverrideGuidance() {
        logger.log(configuration, bootstrap, commands);

        assertEquals(1, appender.events.size());
        var event = appender.events.getFirst();
        assertEquals(Level.INFO, event.getLevel());
        String text = event.getMessage().getFormattedMessage();
        assertTrue(text.startsWith("[SERVER EXTENSION POINTS] [\n"));
        assertEquals(8, text.lines().filter(line -> line.startsWith("  - ")).count());
        assertTrue(text.contains("DefaultConnectionFilter (default)"));
        assertTrue(text.contains("DefaultOutboundQueuePolicy (default)"));
        assertTrue(text.contains("DefaultDatagramPacketPolicy (default)"));
        assertTrue(text.contains("RequestPolicy -> <none> (no request preprocessing)"));
        assertTrue(text.contains("BinaryPacketCompressor -> <none> (compression disabled)"));
        assertTrue(text.contains("BinaryPacketEncryptor -> <none> (packet encryption disabled)"));
        assertTrue(text.contains("ClientCommandPolicy -> <none> (event-based routing)"));
        assertTrue(text.contains("RecoveryPolicy -> <none> (recovery disabled)"));
        assertTrue(text.contains("\n]\nRegister one application implementation with @Component or @Bean"));
        assertTrue(text.contains("inside the bootstrap scan path"));
        assertTrue(text.contains("Its active profile must match."));
    }

    @Test
    void infoReportsActualSelectedApplicationClassesForEveryExtensionPoint() {
        var implementations = Map.of(
                ConnectionFilter.class, install(ConnectionFilter.class),
                OutboundQueuePolicy.class, install(OutboundQueuePolicy.class),
                DatagramPacketPolicy.class, install(DatagramPacketPolicy.class),
                RequestPolicy.class, install(RequestPolicy.class),
                BinaryPacketCompressor.class, install(BinaryPacketCompressor.class),
                BinaryPacketEncryptor.class, install(BinaryPacketEncryptor.class),
                ClientCommandPolicy.class, install(ClientCommandPolicy.class),
                RecoveryPolicy.class, install(RecoveryPolicy.class));
        when(configuration.get(CoreConfigurationType.NETWORK_UDP)).thenReturn(udp(9001));
        declaredCommands();

        logger.log(configuration, bootstrap, commands);

        assertEquals(1, appender.events.size());
        String text = appender.events.getFirst().getMessage().getFormattedMessage();
        implementations.forEach((type, instance) ->
                assertTrue(text.contains(type.getSimpleName() + " -> " + instance.getClass().getName())));
        assertFalse(text.contains("(default)"));
        assertFalse(text.contains("<none>"));
    }

    @ParameterizedTest
    @CsvSource({
            "-1,false,false,false,0", "0,false,false,false,0", "9001,false,false,false,1",
            "9001,false,true,false,0", "-1,true,false,false,1", "-1,true,false,true,0",
            "9001,true,false,false,2", "9001,true,true,false,1", "9001,true,false,true,1",
            "9001,true,true,true,0"
    })
    void warningsAreConditional(int port, boolean hasCommands, boolean customDatagram,
                                boolean customCommands, int warningCount) {
        if (port >= 0) {
            when(configuration.get(CoreConfigurationType.NETWORK_UDP)).thenReturn(udp(port));
        }
        if (hasCommands) {
            declaredCommands();
        }
        if (customDatagram) {
            install(DatagramPacketPolicy.class);
        }
        if (customCommands) {
            install(ClientCommandPolicy.class);
        }

        logger.log(configuration, bootstrap, commands);

        var warnings = appender.events.stream().filter(event -> event.getLevel() == Level.WARN).toList();
        assertEquals(warningCount == 0 ? 0 : 1, warnings.size());
        if (warningCount > 0) {
            String text = warnings.getFirst().getMessage().getFormattedMessage();
            assertTrue(text.contains("[SERVER EXTENSION POINT WARNINGS] [\n"));
            assertEquals(warningCount, text.lines().filter(line -> line.startsWith("  - ")).count());
            assertTrue(text.contains("Check this default"));
            assertTrue(text.contains("implement your own"));
            assertTrue(text.contains("\n]"));
            assertEquals(port > 0 && !customDatagram, text.contains("DefaultDatagramPacketPolicy"));
            assertEquals(hasCommands && !customCommands, text.contains("default EVENT routing"));
        }
    }

    @Test
    void warningsRemainVisibleWhenInfoIsDisabled() {
        capture.setLevel(Level.WARN);
        context.updateLoggers();
        assertFalse(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        when(configuration.get(CoreConfigurationType.NETWORK_UDP)).thenReturn(udp(9001));
        declaredCommands();

        logger.log(configuration, bootstrap, commands);

        assertEquals(1, appender.events.size());
        assertEquals(Level.WARN, appender.events.getFirst().getLevel());
        String text = appender.events.getFirst().getMessage().getFormattedMessage();
        assertTrue(text.contains("integer 'u' and nested 'd'"));
        assertTrue(text.contains("last integer is the convey ID"));
        assertTrue(text.contains("@ClientCommand handlers are not routed automatically"));
    }

    @Test
    void registeringTheDefaultDatagramClassStillWarnsAboutDefaultBehavior() {
        when(configuration.get(CoreConfigurationType.NETWORK_UDP)).thenReturn(udp(9001));
        when(bootstrap.getBeanByClazz(DatagramPacketPolicy.class)).thenReturn(new DefaultDatagramPacketPolicy());

        logger.log(configuration, bootstrap, null);

        assertEquals(1, appender.events.stream().filter(event -> event.getLevel() == Level.WARN).count());
    }

    private <T> T install(Class<T> type) {
        var instance = mock(type);
        when(bootstrap.getBeanByClazz(type)).thenReturn(instance);
        return instance;
    }

    private void declaredCommands() {
        when(commands.getHandlers()).thenReturn(Map.of((short) 42, mock(AbstractClientCommandHandler.class)));
    }

    private static final class RecordingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private RecordingAppender() {
            super("ServerExtensionPointLoggerTest", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
