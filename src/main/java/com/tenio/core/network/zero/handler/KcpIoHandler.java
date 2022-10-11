package com.tenio.core.network.zero.handler;

import com.tenio.core.network.entity.session.Session;

public interface KcpIoHandler extends BaseIoHandler {

  void channelActiveIn(Session session);

  void channelInactiveIn(Session session);
}
