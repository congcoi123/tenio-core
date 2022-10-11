package com.tenio.core.network.kcp.executor;

public interface MessageExecutor {

  void stop();

  boolean isFull();

  void execute(KcpTask kcpTask);
}
