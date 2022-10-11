package com.tenio.core.network.kcp.writer;

import com.tenio.core.network.kcp.kcp.Kcp;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;

public class KcpOutputSocket implements KcpOutput<DatagramSocket> {

  private final DatagramSocket datagramSocket;
  private final InetAddress localAddress;
  private final int port;

  public KcpOutputSocket(DatagramSocket datagramSocket, InetAddress localAddress, int port) {
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
  public int out(ByteBuf data) {
    byte[] binary = new byte[data.readableBytes()];
    data.duplicate().readBytes(binary);
    data.release();
    var request = new DatagramPacket(binary, binary.length, getLocalAddress(), getPort());
    try {
      datagramSocket.send(request);
      return binary.length;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return 0;
  }
}
