package com.tenio.core.network.kcp.executor.netty;

import com.tenio.core.network.kcp.executor.MessageExecutor;
import com.tenio.core.network.kcp.executor.MessageExecutorPool;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyMessageExecutorPool implements MessageExecutorPool {

  protected static final AtomicInteger index = new AtomicInteger();
  private final EventLoopGroup eventExecutors;

  public NettyMessageExecutorPool(int workSize) {
    eventExecutors = new DefaultEventLoopGroup(workSize, r -> {
      return new Thread(r, "nettyMessageExecutorPool-" + index.incrementAndGet());
    });
  }

  @Override
  public MessageExecutor getMessageExecutor() {
    return new NettyMessageExecutor(eventExecutors.next());
  }

  @Override
  public void stop() {
    if (!eventExecutors.isShuttingDown()) {
      eventExecutors.shutdownGracefully();
    }
  }
}
