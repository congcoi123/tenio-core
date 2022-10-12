package com.tenio.core.network.kcp.writer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class KcpWriterChannel implements KcpWriter<DatagramChannel> {

  private final DatagramChannel datagramChannel;
  private final SocketAddress remoteAddress;

  public KcpWriterChannel(DatagramChannel datagramChannel, SocketAddress remoteAddress) {
    this.datagramChannel = datagramChannel;
    this.remoteAddress = remoteAddress;
  }

  @Override
  public InetAddress getLocalAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  @Override
  public int getPort() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DatagramChannel getWriter() {
    return datagramChannel;
  }

  @Override
  public int write(byte[] binaries, int size) throws IOException {
    return getWriter().send(ByteBuffer.wrap(binaries), getRemoteAddress());
  }

  @Override
  public String toString() {
    return "KcpWriterChannel{" +
        "datagramChannel=" + datagramChannel +
        ", remoteAddress=" + remoteAddress +
        '}';
  }
}
