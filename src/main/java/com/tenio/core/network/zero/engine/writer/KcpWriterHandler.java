package com.tenio.core.network.zero.engine.writer;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.network.zero.engine.KcpWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class KcpWriterHandler extends SystemLogger implements KcpWriter<DatagramChannel> {

  private static final int DEFAULT_BUFFER_SIZE = 1024;

  private final DatagramChannel datagramChannel;
  private final SocketAddress remoteAddress;
  private ByteBuffer byteBuffer;

  public KcpWriterHandler(DatagramChannel datagramChannel, SocketAddress remoteAddress) {
    this.datagramChannel = datagramChannel;
    this.remoteAddress = remoteAddress;
    byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
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
  public int send(byte[] binaries, int size) throws IOException {
    byteBuffer.clear();
    // buffer size is not enough, need to be allocated more bytes
    if (byteBuffer.capacity() < size) {
      debug("KCP CHANNEL SEND", "Allocate new buffer from ", byteBuffer.capacity(), " to ",
          size, " bytes");
      byteBuffer = ByteBuffer.allocate(size);
    }
    byteBuffer.put(binaries, 0, size);
    byteBuffer.flip();
    return getWriter().send(byteBuffer, getRemoteAddress());
  }

  @Override
  public String toString() {
    return "KcpWriterHandler{" +
        "datagramChannel=" + datagramChannel +
        ", remoteAddress=" + remoteAddress +
        '}';
  }
}
