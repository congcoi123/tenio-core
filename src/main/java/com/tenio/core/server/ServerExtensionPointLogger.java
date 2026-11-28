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
import com.tenio.common.logger.SystemLogger;
import com.tenio.core.bootstrap.BootstrapHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.client.ClientCommandPolicy;
import com.tenio.core.configuration.define.CoreConfigurationType;
import com.tenio.core.network.codec.compression.BinaryPacketCompressor;
import com.tenio.core.network.codec.encryption.BinaryPacketEncryptor;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.entity.inbound.policy.RequestPolicy;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.security.filter.DefaultConnectionFilter;
import com.tenio.core.network.zero.engine.reader.policy.DatagramPacketPolicy;
import com.tenio.core.network.zero.engine.reader.policy.DefaultDatagramPacketPolicy;
import com.tenio.core.recovery.RecoveryPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports the selected server extension points once during startup without creating fallback beans.
 */
final class ServerExtensionPointLogger extends SystemLogger {

    /**
     * Describes a selected bean, a built-in fallback class, or an inactive extension point.
     *
     * @param contract       extension-point interface
     * @param implementation selected application bean, or null
     * @param fallback       built-in fallback class, or null when no implementation is installed
     * @param inactive       description used when neither a bean nor fallback is available
     * @return one startup list entry without invoking the implementation's toString method
     */
    private static String describe(Class<?> contract, Object implementation, Class<?> fallback, String inactive) {
        var actual = implementation == null ? fallback : implementation.getClass();
        return contract.getSimpleName() + " -> "
                + (actual == null ? "<none> (" + inactive + ")"
                : actual.getName() + (actual == fallback ? " (default)" : ""));
    }

    /**
     * Logs resolved implementation classes and actionable default-policy warnings.
     *
     * @param configuration    loaded server configuration
     * @param bootstrapHandler initialized, profile-selected singleton bean registry
     * @param commands         registered client commands, or null when no manager is supplied
     */
    void log(Configuration configuration, BootstrapHandler bootstrapHandler, ClientCommandManager commands) {
        if (!isInfoEnabled() && !isWarnEnabled()) {
            return;
        }
        var datagramPolicy = bootstrapHandler.getBeanByClazz(DatagramPacketPolicy.class);
        var commandPolicy = bootstrapHandler.getBeanByClazz(ClientCommandPolicy.class);
        if (isInfoEnabled()) {
            var implementations = List.of(
                    describe(ConnectionFilter.class,
                            bootstrapHandler.getBeanByClazz(ConnectionFilter.class),
                            DefaultConnectionFilter.class, null),
                    describe(OutboundQueuePolicy.class,
                            bootstrapHandler.getBeanByClazz(OutboundQueuePolicy.class),
                            DefaultOutboundQueuePolicy.class, null),
                    describe(DatagramPacketPolicy.class, datagramPolicy,
                            DefaultDatagramPacketPolicy.class, null),
                    describe(RequestPolicy.class, bootstrapHandler.getBeanByClazz(RequestPolicy.class),
                            null, "no request preprocessing"),
                    describe(BinaryPacketCompressor.class,
                            bootstrapHandler.getBeanByClazz(BinaryPacketCompressor.class),
                            null, "compression disabled"),
                    describe(BinaryPacketEncryptor.class,
                            bootstrapHandler.getBeanByClazz(BinaryPacketEncryptor.class),
                            null, "packet encryption disabled"),
                    describe(ClientCommandPolicy.class, commandPolicy, null, "event-based routing"),
                    describe(RecoveryPolicy.class, bootstrapHandler.getBeanByClazz(RecoveryPolicy.class),
                            null, "recovery disabled")
            );
            info("SERVER EXTENSION POINTS", ServerImpl.commandListing(implementations)
                    + "\nRegister one application implementation with @Component or @Bean inside the bootstrap"
                    + " scan path to override a server extension point. Its active profile must match.");
        }
        if (isWarnEnabled()) {
            var warnings = new ArrayList<String>();
            var udp = (SocketConfiguration) configuration.get(CoreConfigurationType.NETWORK_UDP);
            if (udp != null && udp.port() > 0
                    && (datagramPolicy == null || datagramPolicy.getClass() == DefaultDatagramPacketPolicy.class)) {
                warnings.add("DatagramPacketPolicy: UDP port " + udp.port()
                        + " is configured without an application override. Using DefaultDatagramPacketPolicy"
                        + " (ZeroMap/MsgPackMap: integer 'u' and nested 'd'; ZeroArray: last integer is the convey ID)."
                        + " Check this default implementation or implement your own DatagramPacketPolicy.");
            }
            if (commandPolicy == null && commands != null && !commands.getHandlers().isEmpty()) {
                warnings.add("ClientCommandPolicy: client commands are registered without an application"
                        + " override. Using default EVENT routing; @ClientCommand handlers are not routed"
                        + " automatically. Check this default behavior or implement your own ClientCommandPolicy.");
            }
            if (!warnings.isEmpty()) {
                warn("[SERVER EXTENSION POINT WARNINGS] ", ServerImpl.commandListing(warnings));
            }
        }
    }
}
