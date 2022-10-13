package com.tenio.core.network.zero.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;

public interface KcpWriter<T> {

  InetAddress getLocalAddress();

  SocketAddress getRemoteAddress();

  int getPort();

  T getWriter();

  int send(byte[] binaries, int size) throws IOException;
}
