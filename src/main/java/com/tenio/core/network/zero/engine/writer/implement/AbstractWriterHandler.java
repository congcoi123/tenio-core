/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.network.zero.engine.writer.implement;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.network.zero.engine.manager.SessionTicketsQueueManager;
import com.tenio.core.network.zero.engine.writer.WriterHandler;
import java.nio.ByteBuffer;
import java.util.Queue;

/**
 * The abstract writer handler.
 */
public abstract class AbstractWriterHandler extends SystemLogger implements WriterHandler {

  private SessionTicketsQueueManager sessionTicketsQueueManager;
  private NetworkWriterStatistic networkWriterStatistic;
  private ByteBuffer byteBuffer;

  @Override
  public Queue<Session> getSessionTicketsQueue(long sessionId) {
    return sessionTicketsQueueManager.getQueueByElementId(sessionId);
  }

  @Override
  public void setSessionTicketsQueueManager(SessionTicketsQueueManager sessionTicketsQueueManager) {
    this.sessionTicketsQueueManager = sessionTicketsQueueManager;
  }

  @Override
  public NetworkWriterStatistic getNetworkWriterStatistic() {
    return networkWriterStatistic;
  }

  @Override
  public void setNetworkWriterStatistic(NetworkWriterStatistic networkWriterStatistic) {
    this.networkWriterStatistic = networkWriterStatistic;
  }

  @Override
  public ByteBuffer getBuffer() {
    return byteBuffer;
  }

  @Override
  public void allocateBuffer(int capacity) {
    // Default write buffer is HEAP
    byteBuffer = ByteBuffer.allocate(capacity);
  }
}
