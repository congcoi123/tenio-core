package com.tenio.core.network.kcp.executor.netty;

import com.tenio.core.network.kcp.executor.KcpTask;
import com.tenio.core.network.kcp.executor.MessageExecutor;
import io.netty.channel.EventLoop;
import java.io.IOException;

public class NettyMessageExecutor implements MessageExecutor {

  private final EventLoop eventLoop;

  public NettyMessageExecutor(EventLoop eventLoop) {
    this.eventLoop = eventLoop;
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isFull() {
    return false;
  }

  @Override
  public void execute(KcpTask kcpTask) {
    eventLoop.execute(() -> {
      try {
        kcpTask.execute();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}
