package com.tenio.core.network.kcp;

import com.backblaze.erasure.FecAdapt;
import com.tenio.common.data.DataType;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.ServiceRuntimeException;
import com.tenio.core.manager.AbstractManager;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.kcp.handler.KcpHandler;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import kcp.ChannelConfig;
import kcp.KcpServer;

public class KcpServiceImpl extends AbstractManager implements KcpService {

  private DataType dataType;
  private SessionManager sessionManager;
  private NetworkReaderStatistic networkReaderStatistic;
  private NetworkWriterStatistic networkWriterStatistic;
  private SocketConfiguration socketConfiguration;
  private KcpServer kcpServer;

  private boolean initialized;

  private KcpServiceImpl(EventManager eventManager) {
    super(eventManager);
    initialized = false;
  }

  public static KcpService newInstance(EventManager eventManager) {
    return new KcpServiceImpl(eventManager);
  }

  @Override
  public void initialize() throws ServiceRuntimeException {
    initialized = true;
  }

  @Override
  public void start() throws ServiceRuntimeException {
    if (!initialized) {
      return;
    }

    ChannelConfig channelConfig = new ChannelConfig();
    channelConfig.nodelay(true, 40, 2, true);
    channelConfig.setSndwnd(512);
    channelConfig.setRcvwnd(512);
    channelConfig.setMtu(512);
    channelConfig.setFecAdapt(new FecAdapt(3, 1));
    channelConfig.setAckNoDelay(true);
    channelConfig.setTimeoutMillis(10000);
    channelConfig.setUseConvChannel(true);
    channelConfig.setCrc32Check(true);

    kcpServer = new KcpServer();
    kcpServer.init(new KcpHandler(
        eventManager,
        sessionManager,
        dataType,
        networkReaderStatistic
    ), channelConfig, socketConfiguration.port());

    if (isInfoEnabled()) {
      info("KCP CHANNEL", buildgen("Started at port: ", socketConfiguration.port()));
    }
  }

  @Override
  public void shutdown() {
    if (!initialized) {
      return;
    }

    kcpServer.stop();
  }

  @Override
  public boolean isActivated() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return "kcp-channel";
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSessionManager(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic) {
    this.networkReaderStatistic = networkReaderStatistic;
  }

  @Override
  public void setNetworkWriterStatistic(NetworkWriterStatistic networkWriterStatistic) {
    this.networkWriterStatistic = networkWriterStatistic;
  }

  @Override
  public void setKcpSocketConfiguration(SocketConfiguration socketConfiguration) {
    this.socketConfiguration = socketConfiguration;
  }

  @Override
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  @Override
  public void write(Packet packet) {
    var iterator = packet.getRecipients().iterator();
    while (iterator.hasNext()) {
      var session = iterator.next();
      if (packet.isMarkedAsLast()) {
        try {
          session.close(ConnectionDisconnectMode.DEFAULT, PlayerDisconnectMode.DEFAULT);
        } catch (IOException exception) {
          if (isErrorEnabled()) {
            error(exception, session.toString());
          }
        }
        return;
      }
      if (session.isActivated()) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(packet.getData());
        session.getKcpChannel().write(byteBuf);
        byteBuf.release();
        session.addWrittenBytes(packet.getOriginalSize());
        networkWriterStatistic.updateWrittenBytes(packet.getOriginalSize());
        networkWriterStatistic.updateWrittenPackets(1);
      } else {
        if (isDebugEnabled()) {
          debug("READ KCP CHANNEL", "Session is inactivated: ", session.toString());
        }
      }
    }
  }
}
