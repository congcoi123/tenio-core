package com.tenio.core.network.kcp.writer;

import com.tenio.core.network.kcp.kcp.Kcp;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

public class KcpOutputChannel implements KcpOutput<DatagramChannel> {

  private final DatagramChannel datagramChannel;
  private final SocketAddress remoteAddress;

  public KcpOutputChannel(DatagramChannel datagramChannel, SocketAddress remoteAddress) {
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
  public void out(Kcp kcp, ByteBuf data) {
    // send data to the client
    try {
      getWriter().send(data.nioBuffer(), getRemoteAddress());
      // update statistic data
//            getNetworkWriterStatistic().updateWrittenBytes(writtenBytes);
//            getNetworkWriterStatistic().updateWrittenPackets(1);

      // update statistic data for session
//            session.addWrittenBytes(writtenBytes);
    } catch (IOException e) {
//            error(e, "Error occurred in writing on session: ", session.toString());
    }
  }
}
