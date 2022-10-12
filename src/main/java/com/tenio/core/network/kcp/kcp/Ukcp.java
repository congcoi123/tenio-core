package com.tenio.core.network.kcp.kcp;

import com.tenio.common.utility.TimeUtility;
import com.tenio.core.configuration.kcp.KcpProfile;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.kcp.writer.KcpWriter;
import com.tenio.core.network.zero.handler.KcpIoHandler;
import java.io.IOException;

public class Ukcp extends Kcp {

  private final Session session;
  private final KcpIoHandler kcpIoHandler;
  private final KcpWriter<?> kcpWriter;

  public Ukcp(long conv, KcpProfile profile, Session session, KcpIoHandler kcpIoHandler,
              KcpWriter<?> kcpWriter) {
    super(conv);
    this.session = session;
    this.session.setUkcp(this);
    this.kcpIoHandler = kcpIoHandler;
    this.kcpWriter = kcpWriter;

    NoDelay(profile.getNodelay(), profile.getUpdateInterval(), profile.getFastResend(),
        profile.getCongestionControl());
  }

  public void input(byte[] binaries) {
    Input(binaries);
  }

  @Override
  protected void output(byte[] buffer, int size) {
    try {
      kcpWriter.write(buffer, size);
    } catch (IOException exception) {
      kcpIoHandler.sessionException(session, exception);
    }
  }

  public void send(byte[] binaries) {
    Send(binaries);
  }

  public void receive(byte[] binaries) {
    int receive = Recv(binaries);
    if (receive > 0) {
      kcpIoHandler.sessionRead(session, binaries);
    }
  }

  public void update() {
    Update(TimeUtility.currentTimeMillis());
  }

  public KcpIoHandler getKcpIoHandler() {
    return kcpIoHandler;
  }
}
