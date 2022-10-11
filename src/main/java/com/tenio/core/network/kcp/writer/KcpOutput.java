package com.tenio.core.network.kcp.writer;

import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.SocketAddress;

public interface KcpOutput<T> {

  InetAddress getLocalAddress();

  SocketAddress getRemoteAddress();

  int getPort();

  T getWriter();

  int out(ByteBuf data);
}
