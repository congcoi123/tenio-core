package com.tenio.core.network.kcp.executor;

public interface MessageExecutorPool {

  MessageExecutor getMessageExecutor();

  void stop();
}
