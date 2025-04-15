package com.tenio.core.network.support;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ByteBufferPool {

  public static final int DEFAULT_MAX_POOL_SIZE_PER_BUCKET = 50;
  public static final int DEFAULT_BUFFER_SIZE = 4096;

  private final Map<Integer, Queue<SoftReference<ByteBuffer>>> poolMap =
      new ConcurrentHashMap<>();
  private final int maxPoolSizePerBucket;

  public ByteBufferPool(int maxPoolSizePerBucket) {
    this.maxPoolSizePerBucket = maxPoolSizePerBucket;
  }

  public ByteBufferPool() {
    this(DEFAULT_MAX_POOL_SIZE_PER_BUCKET);
  }

  public ByteBuffer acquire(int requestedSize) {
    int normalizedSize = normalizeSize(requestedSize);
    Queue<SoftReference<ByteBuffer>> pool = poolMap.get(normalizedSize);
    ByteBuffer buffer = null;

    if (pool != null) {
      SoftReference<ByteBuffer> ref;
      while ((ref = pool.poll()) != null) {
        buffer = ref.get();
        if (buffer != null) {
          break;
        }
      }
    }

    if (buffer == null) {
      buffer = ByteBuffer.allocateDirect(normalizedSize);
    }

    buffer.clear();
    return buffer;
  }

  public ByteBuffer acquire() {
    return acquire(DEFAULT_BUFFER_SIZE);
  }

  public void release(ByteBuffer buffer) {
    int normalizedSize = normalizeSize(buffer.capacity());
    poolMap.computeIfAbsent(normalizedSize, s -> new ConcurrentLinkedQueue<>());
    Queue<SoftReference<ByteBuffer>> pool = poolMap.get(normalizedSize);

    if (pool.size() < maxPoolSizePerBucket) {
      pool.offer(new SoftReference<>(buffer));
    }
  }

  public int available(int size) {
    int normalizedSize = normalizeSize(size);
    Queue<SoftReference<ByteBuffer>> pool = poolMap.get(normalizedSize);
    if (pool == null) {
      return 0;
    }

    return (int) pool.stream().filter(ref -> ref.get() != null).count();
  }

  private int normalizeSize(int size) {
    if (size <= 0) {
      return DEFAULT_BUFFER_SIZE;
    }
    int normalized = Integer.highestOneBit(size - 1) << 1;
    return Math.max(DEFAULT_BUFFER_SIZE, normalized);
  }
}
