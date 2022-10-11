package com.tenio.core.network.kcp.executor;

import java.io.IOException;

public interface KcpTask {

  void execute() throws IOException;
}
