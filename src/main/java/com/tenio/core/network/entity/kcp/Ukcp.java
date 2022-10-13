package com.tenio.core.network.entity.kcp;

import com.tenio.common.utility.TimeUtility;
import com.tenio.core.configuration.kcp.KcpProfile;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.engine.KcpWriter;
import com.tenio.core.network.zero.handler.KcpIoHandler;
import java.io.IOException;

public class Ukcp extends Kcp {

  private final Session session;
  private final KcpIoHandler kcpIoHandler;
  private final KcpWriter<?> kcpWriter;
  private final NetworkWriterStatistic networkWriterStatistic;
  private byte[] buffer;

  public Ukcp(long conv, KcpProfile profile, Session session, KcpIoHandler kcpIoHandler,
              KcpWriter<?> kcpWriter, NetworkWriterStatistic networkWriterStatistic) {
    super(conv);
    this.session = session;
    this.session.setUkcp(this);
    this.kcpIoHandler = kcpIoHandler;
    this.kcpWriter = kcpWriter;
    this.networkWriterStatistic = networkWriterStatistic;

    SetNoDelay(profile.getNodelay(), profile.getUpdateInterval(), profile.getFastResend(),
        profile.getCongestionControl());
  }

  public void input(byte[] binaries) {
    Input(binaries);
  }

  @Override
  protected void Output(byte[] buffer, int size) {
    try {
      int writtenBytes = kcpWriter.send(buffer, size);
      // update statistic data
      networkWriterStatistic.updateWrittenBytes(writtenBytes);
      networkWriterStatistic.updateWrittenPackets(1);

      // update statistic data for session
      session.addWrittenBytes(writtenBytes);
    } catch (IOException exception) {
      kcpIoHandler.sessionException(session, exception);
    }
  }

  public void send(byte[] binaries) {
    Send(binaries);
  }

  public void receive() {
    int receive = Recv((buffer) -> this.buffer = buffer);
    if (receive > 0) {
      kcpIoHandler.sessionRead(session, buffer);
    }
  }

  public void update() {
    Update(TimeUtility.currentTimeMillis());
  }

  public KcpIoHandler getKcpIoHandler() {
    return kcpIoHandler;
  }

  @Override
  public String toString() {
    return "Ukcp{" +
        "session=" + session +
        ", kcpIoHandler=" + kcpIoHandler +
        ", kcpWriter=" + kcpWriter +
        '}';
  }
}
