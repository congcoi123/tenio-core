/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.kcp;

import com.backblaze.erasure.FecAdapt;
import com.tenio.common.data.DataType;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.ServiceRuntimeException;
import com.tenio.core.manager.AbstractManager;
import com.tenio.core.network.configuration.KcpConfiguration;
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

/**
 * The implementation for the KCP service.
 */
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
    channelConfig.setTimeoutMillis(KcpConfiguration.TIME_OUT_IN_MILLISECONDS);
    channelConfig.nodelay(true, 40, 2, true);
    channelConfig.setSndwnd(1024);
    channelConfig.setRcvwnd(1024);
    channelConfig.setMtu(1400);
    channelConfig.setFecAdapt(new FecAdapt(5, 1));
    channelConfig.setAckNoDelay(true);
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
