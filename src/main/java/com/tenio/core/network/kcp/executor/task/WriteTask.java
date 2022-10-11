package com.tenio.core.network.kcp.executor.task;

import com.tenio.core.network.kcp.executor.KcpTask;
import com.tenio.core.network.kcp.kcp.Ukcp;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
public class WriteTask implements KcpTask {


  private final Ukcp ukcp;
  private NetworkWriterStatistic writerStatistic;

  public WriteTask(Ukcp ukcp) {
    this.ukcp = ukcp;
  }

  @Override
  public void execute() {
    Ukcp ukcp = this.ukcp;
    try {
      //查看连接状态
      if (!ukcp.isActive()) {
        return;
      }
      //从发送缓冲区到kcp缓冲区
      Queue<ByteBuf> queue = ukcp.getWriteBuffer();
      int writeCount = 0;
      long writeBytes = 0;
      while (ukcp.canSend(false)) {
        ByteBuf byteBuf = queue.poll();
        if (byteBuf == null) {
          break;
        }
        writeCount++;
        try {
          writeBytes += byteBuf.readableBytes();
          ukcp.send(byteBuf);
          byteBuf.release();
        } catch (IOException e) {
          ukcp.getKcpListener().sessionException(ukcp.session, e);
          return;
        }
      }
      if (ukcp.isControlWriteBufferSize()) {
        ukcp.getWriteBufferIncr().addAndGet(writeCount);
      }
      //如果有发送 则检测时间
      if (!ukcp.canSend(false) || (ukcp.checkFlush() && ukcp.isFastFlush())) {
        long now = System.currentTimeMillis();
        long next = ukcp.flush(now);
        ukcp.setTsUpdate(now + next);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      release();
    }
  }

  public void release() {
    ukcp.getWriteProcessing().set(false);
  }
}
