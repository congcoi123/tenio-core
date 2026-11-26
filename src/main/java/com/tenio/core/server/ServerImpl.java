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
import com.tenio.common.utility.TimeUtility;
import com.tenio.core.admin.AdminWindow;
import com.tenio.core.admin.OperationalHistory;
import com.tenio.core.admin.PerformanceHistory;
import com.tenio.core.admin.ServerInspection;
import com.tenio.core.api.ServerApi;
import com.tenio.core.api.implement.ServerApiImpl;
import com.tenio.core.bootstrap.BootstrapHandler;
import com.tenio.core.bootstrap.injector.ProfileSelection;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.client.ClientCommandPolicy;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.configuration.ConfigurationSource;
import com.tenio.core.configuration.CoreConfiguration;
import com.tenio.core.configuration.DefaultCoreConfiguration;
import com.tenio.core.configuration.constant.CoreConstant;
import com.tenio.core.configuration.define.CoreConfigurationType;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.manager.ChannelManager;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.manager.RoomManager;
import com.tenio.core.entity.manager.implement.ChannelManagerImpl;
import com.tenio.core.entity.manager.implement.PlayerManagerImpl;
import com.tenio.core.entity.manager.implement.RoomManagerImpl;
import com.tenio.core.entity.replication.StateReplication;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.Network;
import com.tenio.core.network.NetworkImpl;
import com.tenio.core.network.codec.compression.BinaryPacketCompressor;
import com.tenio.core.network.codec.decoder.BinaryPacketDecoder;
import com.tenio.core.network.codec.decoder.BinaryPacketDecoderImpl;
import com.tenio.core.network.codec.encoder.BinaryPacketEncoder;
import com.tenio.core.network.codec.encoder.BinaryPacketEncoderImpl;
import com.tenio.core.network.codec.encryption.BinaryPacketEncryptor;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.entity.inbound.policy.RequestPolicy;
import com.tenio.core.network.entity.outbound.Response;
import com.tenio.core.network.entity.outbound.packet.policy.DefaultOutboundQueuePolicy;
import com.tenio.core.network.entity.outbound.packet.policy.OutboundQueuePolicy;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.security.filter.DefaultConnectionFilter;
import com.tenio.core.network.zero.engine.manager.DatagramChannelManager;
import com.tenio.core.network.zero.engine.reader.policy.DatagramPacketPolicy;
import com.tenio.core.network.zero.engine.reader.policy.DefaultDatagramPacketPolicy;
import com.tenio.core.recovery.RecoveryManager;
import com.tenio.core.recovery.RecoveryPolicy;
import com.tenio.core.scheduler.ScheduledJobs;
import com.tenio.core.scheduler.job.MaintenanceJob;
import com.tenio.core.scheduler.job.MetricReportJob;
import com.tenio.core.scheduler.job.PerformanceJob;
import com.tenio.core.server.core.ZeroProcessor;
import com.tenio.core.server.core.ZeroProcessorImpl;
import com.tenio.core.server.setting.ConfigurationAssessment;

import java.awt.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * This class manages the workflow of the current server. The instruction's orders are important,
 * event subscribes must be set last and all configuration values should be confirmed.
 */
public final class ServerImpl extends SystemLogger implements Server {

    private final EventManager eventManager;
    private final RoomManager roomManager;
    private final PlayerManager playerManager;
    private final ChannelManager channelManager;
    private final DatagramChannelManager datagramChannelManager;
    private final ZeroProcessor zeroProcessor;
    private final Network network;
    private final ServerApi serverApi;
    private final AtomicBoolean stopping;
    private final CountDownLatch stopped;
    private ScheduledJobs scheduledJobs;
    private RecoveryManager recoveryManager;
    private PerformanceHistory performanceHistory;
    private volatile StateReplication stateReplication;
    private ServerInspection inspection;
    private volatile ConnectionFilter connectionFilter;
    private ClientCommandManager clientCommandManager;
    private SystemCommandManager systemCommandManager;
    private Configuration configuration;
    private AdminWindow adminWindow;
    private long startedTime;
    private String serverName;

    private ServerImpl() {
        eventManager = EventManager.newInstance();
        roomManager = RoomManagerImpl.newInstance(eventManager);
        playerManager = PlayerManagerImpl.newInstance(eventManager);
        channelManager = ChannelManagerImpl.newInstance(eventManager);
        datagramChannelManager = DatagramChannelManager.newInstance();
        network = NetworkImpl.newInstance(eventManager);
        serverApi = ServerApiImpl.newInstance(this);
        zeroProcessor = ZeroProcessorImpl.newInstance(eventManager, serverApi, datagramChannelManager);
        stopping = new AtomicBoolean(false);
        stopped = new CountDownLatch(1);
    } // prevent creation manually

    /**
     * Preventing Singleton object instantiation from outside and creates multiple instance if two
     * thread access this method simultaneously.
     *
     * @return a new instance
     */
    public static Server getInstance() {
        return InstanceHolder.INSTANCE;
    }

    static <T extends Comparable<? super T>> String commandListing(Collection<T> commands) {
        return commandListing(commands, String::valueOf);
    }

    private static <T extends Comparable<? super T>> String commandListing(Collection<T> commands, Function<T, String> format) {
        var entries = new StringBuilder("[\n");
        if (commands.isEmpty()) {
            entries.append("<empty>\n");
        } else {
            commands.stream()
                    .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                    .forEach(command -> entries.append("  - ").append(format.apply(command)).append('\n'));
        }
        return entries.append(']').toString();
    }

    @Override
    public void start(BootstrapHandler bootstrapHandler, String[] params) throws Exception {
        start(bootstrapHandler, params, null);
    }

    @Override
    public void start(BootstrapHandler bootstrapHandler, String[] params, ConfigurationSource source) throws Exception {
        // record the started time
        startedTime = TimeUtility.currentTimeMillis();

        // create a new configuration instance
        Configuration configuration = bootstrapHandler.getConfigurationHandler().getConfiguration();
        if (configuration == null) {
            if (isInfoEnabled()) {
                info("CONFIGURATION", "No custom configuration class found. Use default configuration (DefaultCoreConfiguration).");
            }

            configuration = new DefaultCoreConfiguration();
        }

        // get the file path
        var file = params == null ? null : (params.length == 0 ? null : params[0]);

        // load the file
        if (source != null && configuration instanceof CoreConfiguration core) {
            core.loadPrepared(file, source);
        } else {
            configuration.load(file);
        }

        // verify profile
        var selectedProfiles = bootstrapHandler.getActiveProfiles();
        if (selectedProfiles != null
                && !selectedProfiles.isEmpty()
                && !selectedProfiles.equals(ProfileSelection.parseRequired(configuration.getString(CoreConfigurationType.PROFILE)))) {
            throw new IllegalStateException("Configuration profiles changed after bootstrap selection");
        }

        // Put the current configurations to the logger
        if (isInfoEnabled()) {
            info("CONFIGURATION", configuration.toString());
        }

        this.configuration = configuration;

        serverName = configuration.getString(CoreConfigurationType.SERVER_NAME);

        if (isInfoEnabled()) {
            info(serverName, "STATE", "STARTING");
        }

        // subscribing for processes and handlers
        zeroProcessor.subscribe(configuration.get(CoreConfigurationType.NETWORK_TCP) != null || configuration.get(CoreConfigurationType.NETWORK_WEBSOCKET) != null,
                configuration.get(CoreConfigurationType.NETWORK_UDP) != null
        );

        bootstrapHandler.getEventHandler().initialize(eventManager);

        // collect all subscribers, listen all the events
        eventManager.subscribe();

        var assessment = ConfigurationAssessment.newInstance(eventManager, configuration);
        assessment.assess();

        setupCommands(bootstrapHandler.getClientCommandManager(), bootstrapHandler.getSystemCommandManager());
        setupEntitiesManagementService(configuration);
        setupNetworkService(configuration, bootstrapHandler);
        setupInternalProcessorService(configuration, bootstrapHandler);
        setupScheduledJobs(bootstrapHandler);
        var recoveryPolicy = bootstrapHandler.getBeanByClazz(RecoveryPolicy.class);
        if (recoveryPolicy != null) {
            recoveryManager = new RecoveryManager(recoveryPolicy, player -> serverApi.logout(player, ConnectionDisconnectMode.IDLE, PlayerDisconnectMode.IDLE));
            zeroProcessor.configureRecovery(recoveryManager, recoveryPolicy);
            scheduledJobs.registerInterval("EXPIRE_RECOVERY_WINDOWS", recoveryManager::expire, 1);
        }

        new ServerExtensionPointLogger().log(configuration, bootstrapHandler, clientCommandManager);

        // admin monitoring tools
        var profiles = bootstrapHandler.getActiveProfiles();
        var sessionManager = network.getSessionManager();
        if (clientCommandManager != null && clientCommandManager.hasLimits()) {
            sessionManager.onSessionRemoved(clientCommandManager::releaseSession);
        }
        boolean historyEnabled = profiles != null && profiles.contains(CoreConstant.HISTORY_PROFILE);
        boolean monitorEnabled = profiles != null && profiles.contains(CoreConstant.MONITOR_PROFILE);
        boolean windowEnabled = monitorEnabled && !GraphicsEnvironment.isHeadless();
        var performance = bootstrapHandler.getBeanByClazz(PerformanceJob.class);
        if (clientCommandManager != null && (monitorEnabled || performance != null)) {
            clientCommandManager.enableExecutionMonitoring();
        }
        if (performance != null) {
            performanceHistory = new PerformanceHistory(Path.of("logs", "performance"), windowEnabled);
            performance.configure(this, network, performanceHistory);
        }
        if (monitorEnabled || historyEnabled || performanceHistory != null) {
            inspection = new ServerInspection(this, network, monitorEnabled);
            if (monitorEnabled) {
                sessionManager.enableQueueMonitoring();
            }
            if (historyEnabled) {
                var history = windowEnabled ? new OperationalHistory(CoreConstant.DEFAULT_OPERATIONAL_HISTORY_CAPACITY)
                        : OperationalHistory.toFile(Path.of("logs", "history"));
                sessionManager.setOperationalHistory(history);
                if (history.file() != null && isInfoEnabled()) {
                    info("HISTORY", "Writing operational history to ", history.file().toAbsolutePath());
                }
            }
        }

        initializeServices();
        startServices();

        // now it can be able to accept connections
        network.activate();
        zeroProcessor.activate();

        // it should wait for a while to let everything settles down
        int servicesTakeTime = Math.max(network.getMaximumStartingTimeInMilliseconds(), zeroProcessor.getMaximumStartingTimeInMilliseconds());
        int totalWaitingTime = servicesTakeTime + CoreConstant.DELAY_BEFORE_SERVER_IS_READY_IN_MILLISECONDS;
        Thread.sleep(totalWaitingTime);

        if (isInfoEnabled()) {
            info(serverName, "STATE", "RUNNING");
        }

        // emit "server initialization" event
        eventManager.emit(ServerEvent.SERVER_INITIALIZATION, serverName);

        // show admin tool when enabled
        if (windowEnabled) {
            adminWindow = new AdminWindow("[TenIO] Monitoring | " + serverName, inspection::query, (command, output) -> {
                var manager = bootstrapHandler.getSystemCommandManager();
                if (manager == null || manager.getHandler(command.split("\\s+", 2)[0].toLowerCase(Locale.ROOT)) == null) {
                    return false;
                }
                manager.invoke(command, output);
                return true;
            }, performanceHistory);
        }

        // keep the application alive
        startLifecycleThread();
    }

    private void initializeServices() {
        network.initialize();
        zeroProcessor.initialize();
    }

    private void startServices() {
        network.start();
        zeroProcessor.start();
        try {
            if (scheduledJobs != null) {
                scheduledJobs.start();
            }
        } catch (RuntimeException exception) {
            shutdownServices();
            throw exception;
        }
    }

    private void setupCommands(ClientCommandManager clientCommandManager, SystemCommandManager systemCommandManager) {
        this.clientCommandManager = clientCommandManager;
        this.systemCommandManager = systemCommandManager;

        if (isInfoEnabled()) {
            info("SERVER COMMANDS", commandListing(
                    systemCommandManager == null ? Set.of() : systemCommandManager.getHandlers().keySet()));
            String clientListing = clientCommandManager == null
                    ? commandListing(Set.<Short>of())
                    : commandListing(clientCommandManager.getHandlers().keySet(),
                    code -> clientCommandManager.describeCommand(code, true));
            info("CLIENT COMMANDS", clientListing);
        }
    }

    private void setupEntitiesManagementService(Configuration configuration) {
        playerManager.configureMaxIdleTimeInSeconds(configuration.getInt(CoreConfigurationType.PROP_MAX_PLAYER_IDLE_TIME));
        roomManager.configureMaxRooms(configuration.getInt(CoreConfigurationType.PROP_MAX_NUMBER_ROOMS));
    }

    private void setupNetworkService(Configuration configuration, BootstrapHandler bootstrapHandler)
            throws IllegalArgumentException, SecurityException {
        ConnectionFilter connectionFilter = bootstrapHandler.getBeanByClazz(ConnectionFilter.class);
        if (connectionFilter == null) {
            connectionFilter = new DefaultConnectionFilter();
        }
        network.setConnectionFilterClass(
                connectionFilter,
                configuration.getInt(CoreConfigurationType.NETWORK_PROP_MAX_CONNECTIONS_PER_IP)
        );
        this.connectionFilter = connectionFilter;

        var servletMap = bootstrapHandler.getServletMap();
        var httpConfiguration = configuration.get(CoreConfigurationType.NETWORK_HTTP);
        network.setHttpConfiguration(
                CoreConstant.DEFAULT_NUMBER_HTTP_WORKERS,
                httpConfiguration != null ? ((SocketConfiguration) httpConfiguration).port() : 0,
                httpConfiguration != null ? servletMap : null
        );

        var serverAddress = configuration.getString(CoreConfigurationType.SERVER_ADDRESS);

        network.setSocketAcceptorServerAddress(serverAddress);

        network.setSocketAcceptorBufferSize(configuration.getInt(CoreConfigurationType.NETWORK_PROP_SOCKET_ACCEPTOR_BUFFER_SIZE));
        network.setSocketAcceptorWorkers(CoreConstant.DEFAULT_ENGINE_THREAD_POOL_SIZE);

        var tcpSocketConfiguration = configuration.get(CoreConfigurationType.NETWORK_TCP) != null
                        ? (SocketConfiguration) configuration.get(CoreConfigurationType.NETWORK_TCP)
                        : null;
        var udpChannelConfiguration = configuration.get(CoreConfigurationType.NETWORK_UDP) != null
                        ? (SocketConfiguration) configuration.get(CoreConfigurationType.NETWORK_UDP)
                        : null;
        var webSocketConfiguration = configuration.get(CoreConfigurationType.NETWORK_WEBSOCKET) != null
                        ? (SocketConfiguration) configuration.get(CoreConfigurationType.NETWORK_WEBSOCKET)
                        : null;
        network.setSocketConfigurations(tcpSocketConfiguration, udpChannelConfiguration, webSocketConfiguration);

        if (udpChannelConfiguration != null) {
            datagramChannelManager.configureUdpPort(udpChannelConfiguration.port());
        }

        network.setSocketReaderBufferSize(configuration.getInt(CoreConfigurationType.NETWORK_PROP_SOCKET_READER_BUFFER_SIZE));
        network.setSocketReaderWorkers(configuration.getInt(CoreConfigurationType.WORKER_SOCKET_READER));

        network.setSocketWriterBufferSize(configuration.getInt(CoreConfigurationType.NETWORK_PROP_SOCKET_WRITER_BUFFER_SIZE));
        network.setSocketWriterWorkers(configuration.getInt(CoreConfigurationType.WORKER_SOCKET_WRITER));

        network.setWebSocketConsumerWorkers(configuration.getInt(CoreConfigurationType.WORKER_WEBSOCKET_CONSUMER));
        network.setWebSocketProducerWorkers(configuration.getInt(CoreConfigurationType.WORKER_WEBSOCKET_PRODUCER));

        network.setWebSocketReceiverBufferSize(configuration.getInt(CoreConfigurationType.NETWORK_PROP_WEBSOCKET_RECEIVER_BUFFER_SIZE));
        network.setWebSocketSenderBufferSize(configuration.getInt(CoreConfigurationType.NETWORK_PROP_WEBSOCKET_SENDER_BUFFER_SIZE));
        network.setWebSocketUsingSsl(configuration.getBoolean(CoreConfigurationType.NETWORK_PROP_WEBSOCKET_USING_SSL));

        OutboundQueuePolicy outboundQueuePolicy = bootstrapHandler.getBeanByClazz(OutboundQueuePolicy.class);
        if (outboundQueuePolicy == null) {
            outboundQueuePolicy = new DefaultOutboundQueuePolicy();
        }
        network.setSessionOutboundQueuePolicy(outboundQueuePolicy);
        network.setSessionInboundQueueSize(configuration.getInt(CoreConfigurationType.PROP_MAX_SESSION_REQUEST_QUEUE_SIZE));
        network.setSessionOutboundQueueSize(configuration.getInt(CoreConfigurationType.PROP_MAX_SESSION_RESPONSE_QUEUE_SIZE));
        network.setSessionSlowConsumingInboundQueueWarningThreshold(configuration.getInt(CoreConfigurationType.PROP_SLOW_CONSUMING_WARNING_SESSION_REQUEST_THRESHOLD));
        network.setSessionSlowConsumingOutboundQueueWarningThreshold(configuration.getInt(CoreConfigurationType.PROP_SLOW_CONSUMING_WARNING_SESSION_RESPONSE_THRESHOLD));

        DatagramPacketPolicy datagramPacketPolicy = bootstrapHandler.getBeanByClazz(DatagramPacketPolicy.class);
        if (datagramPacketPolicy == null) {
            datagramPacketPolicy = new DefaultDatagramPacketPolicy();
        }
        network.setDatagramPacketPolicy(datagramPacketPolicy);

        network.setSessionMaxIdleTimeInSeconds(configuration.getInt(CoreConfigurationType.PROP_MAX_PLAYER_IDLE_TIME));

        BinaryPacketCompressor binaryPacketCompressor = bootstrapHandler.getBeanByClazz(BinaryPacketCompressor.class);
        BinaryPacketEncryptor binaryPacketEncryptor = bootstrapHandler.getBeanByClazz(BinaryPacketEncryptor.class);
        BinaryPacketEncoder binaryPacketEncoder = new BinaryPacketEncoderImpl();
        BinaryPacketDecoder binaryPacketDecoder = new BinaryPacketDecoderImpl();

        binaryPacketEncoder.setCompressionThresholdBytes(configuration.getInt(CoreConfigurationType.NETWORK_PROP_PACKET_COMPRESSION_THRESHOLD_BYTES));
        binaryPacketEncoder.setCompressor(binaryPacketCompressor);
        binaryPacketEncoder.setEncryptor(binaryPacketEncryptor);

        binaryPacketDecoder.setCompressor(binaryPacketCompressor);
        binaryPacketDecoder.setEncryptor(binaryPacketEncryptor);

        network.setPacketDecoder(binaryPacketDecoder);
        network.setPacketEncoder(binaryPacketEncoder);
    }

    private void setupInternalProcessorService(Configuration configuration, BootstrapHandler bootstrapHandler) {
        RequestPolicy requestPolicy = bootstrapHandler.getBeanByClazz(RequestPolicy.class);
        zeroProcessor.setRequestPolicy(requestPolicy);
        var commandPolicy = bootstrapHandler.getBeanByClazz(ClientCommandPolicy.class);
        if (commandPolicy != null) {
            zeroProcessor.configureClientCommands(clientCommandManager, commandPolicy);
        }
        boolean registeredCommands =
                clientCommandManager != null && !clientCommandManager.getHandlers().isEmpty();
        boolean commandMode = commandPolicy != null && registeredCommands;
        if (isInfoEnabled()) {
            info("CLIENT MESSAGE ROUTING",
                    commandMode
                            ? "COMMAND: ClientCommandPolicy -> ClientCommandHandler; EventReceivedMessageFromPlayer is"
                            + " disabled"
                            : "EVENT: EventReceivedMessageFromPlayer; automatic command routing is disabled");
            if (commandMode && eventManager.hasSubscriber(ServerEvent.RECEIVED_MESSAGE_FROM_PLAYER)) {
                info("CLIENT MESSAGE ROUTING WARNING",
                        "Both handlers are registered. COMMAND mode takes precedence; "
                                + "EventReceivedMessageFromPlayer will not receive gameplay messages");
            } else if (commandPolicy != null && !registeredCommands) {
                info("CLIENT MESSAGE ROUTING WARNING",
                        "No client commands are registered. EVENT mode remains active; the policy is not invoked");
            }
        }
        zeroProcessor.setMaxNumberPlayers(configuration.getInt(CoreConfigurationType.PROP_MAX_NUMBER_PLAYERS));
        zeroProcessor.setSessionManager(network.getSessionManager());
        zeroProcessor.setPlayerManager(playerManager);
        zeroProcessor.setThreadPoolSize(configuration.getInt(CoreConfigurationType.WORKER_PROCESSOR));
        zeroProcessor.setKeepPlayerOnDisconnection(configuration.getBoolean(CoreConfigurationType.PROP_KEEP_PLAYER_ON_DISCONNECTION));

        zeroProcessor.setNetworkReaderStatistic(network.getNetworkReaderStatistic());
        zeroProcessor.setNetworkWriterStatistic(network.getNetworkWriterStatistic());
    }

    private void setupScheduledJobs(BootstrapHandler bootstrapHandler) {
        scheduledJobs = bootstrapHandler.getScheduledJobs();
        var maintenance = bootstrapHandler.getBeanByClazz(MaintenanceJob.class);
        if (maintenance != null) {
            maintenance.configure(network.getSessionManager(), configuration, scheduledJobs);
        }
        var metrics = bootstrapHandler.getBeanByClazz(MetricReportJob.class);
        if (metrics != null) {
            metrics.configure(network.getNetworkReaderStatistic(), network.getNetworkWriterStatistic());
        }
    }

    @Override
    public void shutdown() {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }

        if (isInfoEnabled()) {
            info(serverName, "STATE", "STOPPING");
        }

        try {
            stopServer();
        } finally {
            stopped.countDown();
        }
    }

    Thread startLifecycleThread() {
        return Thread.ofPlatform().daemon(false).name("tenio-server-lifecycle").start(this::awaitShutdown);
    }

    private void awaitShutdown() {
        try {
            stopped.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopServer() {
        if (clientCommandManager != null) {
            clientCommandManager.stopDispatching();
        }
        // emit "server shutdown" event
        try {
            eventManager.emit(ServerEvent.SERVER_TEARDOWN, serverName);
        } finally {
            shutdownServices();
        }
        if (isInfoEnabled()) {
            info(serverName, "STATE", "STOPPED");
        }
    }

    private void shutdownServices() {
        if (adminWindow != null) {
            adminWindow.close();
        }
        if (scheduledJobs != null) {
            scheduledJobs.shutdown();
        }
        if (recoveryManager != null) {
            recoveryManager.close();
            recoveryManager = null;
        }
        if (performanceHistory != null) {
            performanceHistory.close();
            performanceHistory = null;
        }
        var directory = roomManager.getDirectory();
        if (directory != null) {
            directory.close();
        }
        if (stateReplication != null) {
            stateReplication.close();
            stateReplication = null;
        }
        network.shutdown();
        var sessionManager = network.getSessionManager();
        if (sessionManager != null && sessionManager.getOperationalHistory() != null) {
            sessionManager.getOperationalHistory().close();
        }
        zeroProcessor.shutdown();
        if (clientCommandManager != null) {
            clientCommandManager.clear();
        }
        if (systemCommandManager != null) {
            systemCommandManager.shutdown();
        }
    }

    @Override
    public ServerApi getApi() {
        return serverApi;
    }

    @Override
    public ClientCommandManager getClientCommandManager() {
        return clientCommandManager;
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public RoomManager getRoomManager() {
        return roomManager;
    }

    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    @Override
    public DatagramChannelManager getDatagramChannelManager() {
        return datagramChannelManager;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public long getStartedTime() {
        return startedTime;
    }

    @Override
    public long getUptime() {
        return TimeUtility.currentTimeMillis() - startedTime;
    }

    @Override
    public ConnectionFilter getConnectionFilter() {
        return connectionFilter;
    }

    @Override
    public ServerInspection getInspection() {
        return inspection;
    }

    @Override
    public void write(Response response, boolean markedAsLast) {
        network.write(response, markedAsLast);
    }

    @Override
    public synchronized StateReplication enableStateReplication(int maximumEntities, int maximumBytes, int maximumSubscribers) {
        if (stateReplication != null) {
            throw new IllegalStateException("State replication already enabled");
        }
        stateReplication = new StateReplication(maximumEntities, maximumBytes, maximumSubscribers);
        return stateReplication;
    }

    @Override
    public StateReplication getStateReplication() {
        return stateReplication;
    }

    @Override
    public PerformanceHistory getPerformanceHistory() {
        return performanceHistory;
    }

    @Override
    public RecoveryManager getRecoveryManager() {
        return recoveryManager;
    }

    private static final class InstanceHolder {
        private static final Server INSTANCE = new ServerImpl();
    }
}
