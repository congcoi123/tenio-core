package com.tenio.core.network.kcp.writer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;

public class KcpWriterSocket implements KcpWriter<DatagramSocket> {

  private final DatagramSocket datagramSocket;
  private final InetAddress localAddress;
  private final int port;

  public KcpWriterSocket(DatagramSocket datagramSocket, InetAddress localAddress, int port) {
    this.datagramSocket = datagramSocket;
    this.localAddress = localAddress;
    this.port = port;
  }

  @Override
  public InetAddress getLocalAddress() {
    return localAddress;
  }

  @Override
  public SocketAddress getRemoteAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public DatagramSocket getWriter() {
    return datagramSocket;
  }

  @Override
  public int write(byte[] binaries, int size) throws IOException {
    var request = new DatagramPacket(binaries, size, getLocalAddress(), getPort());
    datagramSocket.send(request);
    return size;
  }
}
