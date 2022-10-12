package com.tenio.core.network.kcp.writer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;

public interface KcpWriter<T> {

  InetAddress getLocalAddress();

  SocketAddress getRemoteAddress();

  int getPort();

  T getWriter();

  int write(byte[] binaries, int size) throws IOException;
}
