package com.tenio.core.network.kcp.writer;

import com.tenio.core.network.kcp.kcp.Kcp;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.SocketAddress;

public interface KcpOutput<T> {

  InetAddress getLocalAddress();

  SocketAddress getRemoteAddress();

  int getPort();

  T getWriter();

  void out(Kcp kcp, ByteBuf data);
}
