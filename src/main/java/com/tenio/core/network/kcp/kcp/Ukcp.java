package com.tenio.core.network.kcp.kcp;

import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.kcp.configuration.KcpConfiguration;
import com.tenio.core.network.kcp.executor.MessageExecutor;
import com.tenio.core.network.kcp.executor.task.ReadTask;
import com.tenio.core.network.kcp.executor.task.WriteTask;
import com.tenio.core.network.kcp.writer.KcpOutput;
import com.tenio.core.network.zero.handler.KcpIoHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Ukcp {

  private static final InternalLogger log = InternalLoggerFactory.getInstance(Ukcp.class);


  private final IKcp kcp;
  private final Queue<ByteBuf> writeBuffer;
  private final Queue<ByteBuf> readBuffer;
  private final MessageExecutor iMessageExecutor;
  private final KcpIoHandler kcpListener;
  private final long timeoutMillis;
  public final Session session;
  private final AtomicBoolean writeProcessing = new AtomicBoolean(false);
  private final AtomicBoolean readProcessing = new AtomicBoolean(false);
  private final AtomicInteger readBufferIncr = new AtomicInteger(-1);
  private final AtomicInteger writeBufferIncr = new AtomicInteger(-1);
  private final WriteTask writeTask = new WriteTask(this);
  private final ReadTask readTask = new ReadTask(this);
  private boolean fastFlush = true;
  private long tsUpdate = -1;
  private boolean active;
  private boolean controlReadBufferSize = false;

  private boolean controlWriteBufferSize = false;


  /**
   * 上次收到消息时间
   **/
  private long lastRecieveTime = System.currentTimeMillis();


  /**
   * Creates a new instance.
   *
   * @param output output for kcp
   */
  public Ukcp(int conv, KcpOutput output, KcpIoHandler kcpListener,
              MessageExecutor iMessageExecutor,
              KcpConfiguration kcpConfiguration, Session session) {
    this.timeoutMillis = kcpConfiguration.getTimeoutMillis();
    this.kcp = new Kcp(conv, output);
    this.active = true;
    this.kcpListener = kcpListener;
    this.iMessageExecutor = iMessageExecutor;
    this.session = session;
    this.writeBuffer = new LinkedBlockingQueue<>();
    this.readBuffer = new LinkedBlockingQueue<>();
    this.session.setUkcp(this);

    if (kcpConfiguration.getReadBufferSize() != -1) {
      this.controlReadBufferSize = true;
      this.readBufferIncr.set(kcpConfiguration.getReadBufferSize() / kcpConfiguration.getMtu());
    }

    if (kcpConfiguration.getWriteBufferSize() != -1) {
      this.controlWriteBufferSize = true;
      this.writeBufferIncr.set(kcpConfiguration.getWriteBufferSize() / kcpConfiguration.getMtu());
    }

    kcp.setReserved(0);
    initKcpConfig(kcpConfiguration);
  }

  private void initKcpConfig(KcpConfiguration kcpConfiguration) {
    kcp.nodelay(kcpConfiguration.isNodelay(), kcpConfiguration.getInterval(),
        kcpConfiguration.getFastresend(), kcpConfiguration.isNocwnd());
    kcp.setSndWnd(kcpConfiguration.getSndwnd());
    kcp.setRcvWnd(kcpConfiguration.getRcvwnd());
    kcp.setMtu(kcpConfiguration.getMtu());
    kcp.setStream(kcpConfiguration.isStream());
    kcp.setAckNoDelay(kcpConfiguration.isAckNoDelay());
    kcp.setAckMaskSize(kcpConfiguration.getAckMaskSize());
    this.fastFlush = kcpConfiguration.isFastFlush();
  }


  /**
   * Receives ByteBufs.
   *
   * @param bufList received ByteBuf will be add to the list
   */
  public void receive(List<ByteBuf> bufList) {
    kcp.recv(bufList);
  }


  public ByteBuf mergeReceive() {
    return kcp.mergeRecv();
  }


  public void input(ByteBuf data, long current) throws IOException {
    input(data, true, current);
  }

  private void input(ByteBuf data, boolean regular, long current) throws IOException {
    int ret = kcp.input(data, regular, current);
    switch (ret) {
      case -1:
        throw new IOException("No enough bytes of head");
      case -2:
        throw new IOException("No enough bytes of data");
      case -3:
        throw new IOException("Mismatch cmd");
      case -4:
        throw new IOException("Conv inconsistency");
      default:
        break;
    }
  }


  /**
   * Sends a Bytebuf.
   *
   * @param buf
   * @throws IOException
   */
  public void send(ByteBuf buf) throws IOException {
    int ret = kcp.send(buf);
    switch (ret) {
      case -2:
        throw new IOException("Too many fragments");
      default:
        break;
    }
  }


  /**
   * Returns {@code true} if there are bytes can be received.
   *
   * @return
   */
  public boolean canRecv() {
    return kcp.canRecv();
  }


  public long getLastRecieveTime() {
    return lastRecieveTime;
  }

  public void setLastRecieveTime(long lastRecieveTime) {
    this.lastRecieveTime = lastRecieveTime;
  }

  /**
   * Returns {@code true} if the kcp can send more bytes.
   *
   * @param curCanSend last state of canSend
   * @return {@code true} if the kcp can send more bytes
   */
  public boolean canSend(boolean curCanSend) {
    int max = kcp.getSndWnd() * 2;
    int waitSnd = kcp.waitSnd();
    if (curCanSend) {
      return waitSnd < max;
    } else {
      int threshold = Math.max(1, max / 2);
      return waitSnd < threshold;
    }
  }

  /**
   * Udpates the kcp.
   *
   * @param current current time in milliseconds
   * @return the next time to update
   */
  protected long update(long current) {
    kcp.update(current);
    long nextTsUp = check(current);
    setTsUpdate(nextTsUp);

    return nextTsUp;
  }

  public long flush(long current) {
    return kcp.flush(false, current);
  }

  /**
   * Determines when should you invoke udpate.
   *
   * @param current current time in milliseconds
   * @return
   * @see Kcp#check(long)
   */
  protected long check(long current) {
    return kcp.check(current);
  }

  /**
   * Returns {@code true} if the kcp need to flush.
   *
   * @return {@code true} if the kcp need to flush
   */
  public boolean checkFlush() {
    return kcp.checkFlush();
  }

  /**
   * Returns conv of kcp.
   *
   * @return conv of kcp
   */
  public int getConv() {
    return kcp.getConv();
  }

  /**
   * Set the conv of kcp.
   *
   * @param conv the conv of kcp
   */
  public void setConv(int conv) {
    kcp.setConv(conv);
  }


  /**
   * Returns update interval.
   *
   * @return update interval
   */
  public int getInterval() {
    return kcp.getInterval();
  }


  public boolean isStream() {
    return kcp.isStream();
  }


  /**
   * Sets the {@link ByteBufAllocator} which is used for the kcp to allocate buffers.
   *
   * @param allocator the allocator is used for the kcp to allocate buffers
   * @return this object
   */
  public Ukcp setByteBufAllocator(ByteBufAllocator allocator) {
    kcp.setByteBufAllocator(allocator);
    return this;
  }

  public boolean isFastFlush() {
    return fastFlush;
  }

  public void read(ByteBuf byteBuf) {
    if (controlReadBufferSize) {
      int readBufferSize = readBufferIncr.getAndUpdate(operand -> {
        if (operand == 0) {
          return operand;
        }
        return --operand;
      });
      if (readBufferSize == 0) {
        //TODO 这里做的不对 应该丢弃队列最早的那个消息包  这样子丢弃有一定的概率会卡死 以后优化
        byteBuf.release();
        log.error("conv {} address {} readBuffer is full", kcp.getConv(),
            kcp.getOutput().getRemoteAddress());
        return;
      }
    }
    this.readBuffer.offer(byteBuf);
    notifyReadEvent();
  }

  /**
   * 发送有序可靠消息
   * 线程安全的
   *
   * @param byteBuf 发送后需要手动调用 {@link ByteBuf#release()}
   * @return true发送成功  false缓冲区满了
   */
  public boolean write(ByteBuf byteBuf) {
    if (controlWriteBufferSize) {
      int bufferSize = writeBufferIncr.getAndUpdate(operand -> {
        if (operand == 0) {
          return operand;
        }
        return --operand;
      });
      if (bufferSize == 0) {
        //log.error("conv {} address {} writeBuffer is full",kcp.getConv(),((KcpUser)kcp.getUser()).getRemoteAddress());
        return false;
      }
    }
    byteBuf = byteBuf.retainedDuplicate();
    writeBuffer.offer(byteBuf);
    notifyWriteEvent();
    return true;
  }


  public AtomicInteger getReadBufferIncr() {
    return readBufferIncr;
  }


  /**
   * 主动关闭连接调用
   */
  public void close() {
    this.iMessageExecutor.execute(() -> internalClose());
  }

  private void notifyReadEvent() {
    if (readProcessing.compareAndSet(false, true)) {
      this.iMessageExecutor.execute(this.readTask);
    }
  }

  public void notifyWriteEvent() {
    if (writeProcessing.compareAndSet(false, true)) {
      this.iMessageExecutor.execute(this.writeTask);
    }
  }


  public long getTsUpdate() {
    return tsUpdate;
  }

  public Ukcp setTsUpdate(long tsUpdate) {
    this.tsUpdate = tsUpdate;
    return this;
  }

  public Queue<ByteBuf> getReadBuffer() {
    return readBuffer;
  }

  public Queue<ByteBuf> getWriteBuffer() {
    return writeBuffer;
  }

  public KcpIoHandler getKcpListener() {
    return kcpListener;
  }

  public boolean isActive() {
    return active;
  }


  public void internalClose() {
    if (!active) {
      return;
    }
    this.active = false;
    notifyReadEvent();
    kcpListener.channelInactiveIn(this.session);
    //关闭之前尽量把消息都发出去
    notifyWriteEvent();
    kcp.flush(false, System.currentTimeMillis());
    //连接删除
    session.setUkcp(null);
    release();
  }

  void release() {
    kcp.setState(-1);
    kcp.release();
    for (; ; ) {
      ByteBuf byteBuf = writeBuffer.poll();
      if (byteBuf == null) {
        break;
      }
      byteBuf.release();
    }
    for (; ; ) {
      ByteBuf byteBuf = readBuffer.poll();
      if (byteBuf == null) {
        break;
      }
      byteBuf.release();
    }
  }

  public AtomicBoolean getWriteProcessing() {
    return writeProcessing;
  }


  public AtomicBoolean getReadProcessing() {
    return readProcessing;
  }

  protected MessageExecutor getiMessageExecutor() {
    return iMessageExecutor;
  }

  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  public AtomicInteger getWriteBufferIncr() {
    return writeBufferIncr;
  }

  public boolean isControlReadBufferSize() {
    return controlReadBufferSize;
  }


  public boolean isControlWriteBufferSize() {
    return controlWriteBufferSize;
  }


  @Override
  public String toString() {
    return "Ukcp(" +
        "getConv=" + kcp.getConv() +
        ", state=" + kcp.getState() +
        ", active=" + active +
        ')';
  }
}
