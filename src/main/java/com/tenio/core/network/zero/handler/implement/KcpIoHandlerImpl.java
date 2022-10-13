package com.tenio.core.network.zero.handler.implement;

import com.tenio.common.data.utility.ZeroUtility;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.data.ServerMessage;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.zero.handler.KcpIoHandler;

public final class KcpIoHandlerImpl extends AbstractIoHandler implements KcpIoHandler {

  private KcpIoHandlerImpl(EventManager eventManager) {
    super(eventManager);
  }

  public static KcpIoHandlerImpl newInstance(EventManager eventManager) {
    return new KcpIoHandlerImpl(eventManager);
  }

  @Override
  public void sessionRead(Session session, byte[] binary) {
    var data = ZeroUtility.binaryToCollection(binary);
    var message = ServerMessage.newInstance().setData(data);

    eventManager.emit(ServerEvent.SESSION_READ_MESSAGE, session, message);
  }

  @Override
  public void sessionException(Session session, Exception exception) {
    eventManager.emit(ServerEvent.SESSION_OCCURRED_EXCEPTION, session, exception);
  }

  @Override
  public void channelActiveIn(Session session) {
    debug("KCP CHANNEL", "Activated", session.getUkcp());
  }

  @Override
  public void channelInactiveIn(Session session) {
    debug("KCP CHANNEL", "Inactivated", session.getUkcp());
  }
}
