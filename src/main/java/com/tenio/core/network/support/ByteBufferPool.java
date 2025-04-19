/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.support;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A pool of ByteBuffers that manages the allocation and reuse of direct ByteBuffers.
 * This implementation uses a bucketing strategy where ByteBuffers are grouped by their size,
 * and each bucket maintains a queue of SoftReferences to allow garbage collection when memory is low.
 * The pool helps reduce memory allocation overhead and garbage collection pressure by reusing buffers.
 *
 * @since 0.6.2
 */
public final class ByteBufferPool {

  /** Default maximum number of buffers to keep in each size bucket. */
  public static final int DEFAULT_MAX_POOL_SIZE_PER_BUCKET = 50;
  /** Default buffer size in bytes when no specific size is requested. */
  public static final int DEFAULT_BUFFER_SIZE = 1024;

  private final Map<Integer, Queue<SoftReference<ByteBuffer>>> poolMap;
  private final int maxPoolSizePerBucket;
  private final int bufferSize;

  /**
   * Creates a new ByteBufferPool with the specified maximum pool size per bucket.
   *
   * @param maxPoolSizePerBucket the maximum number of buffers to keep in each size bucket
   * @param bufferSize the buffer size
   */
  public ByteBufferPool(int maxPoolSizePerBucket, int bufferSize) {
    this.maxPoolSizePerBucket = maxPoolSizePerBucket;
    this.bufferSize = bufferSize;
    poolMap = new ConcurrentHashMap<>();
  }

  /**
   * Creates a new ByteBufferPool with the default maximum pool size per bucket.
   */
  public ByteBufferPool() {
    this(DEFAULT_MAX_POOL_SIZE_PER_BUCKET, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Acquires a ByteBuffer of at least the requested size. If a buffer of the appropriate size
   * is available in the pool, it will be reused. Otherwise, a new direct ByteBuffer will be allocated.
   * The returned buffer will be cleared and ready for use.
   *
   * @param requestedSize the minimum size of the buffer in bytes
   * @return a ByteBuffer of at least the requested size
   */
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

  /**
   * Acquires a ByteBuffer with the default buffer size.
   *
   * @return a ByteBuffer with the default size
   */
  public ByteBuffer acquire() {
    return acquire(bufferSize);
  }

  /**
   * Releases a ByteBuffer back to the pool for reuse. The buffer will be added to the appropriate
   * size bucket if there is space available. If the bucket is full, the buffer will be discarded.
   *
   * @param buffer the ByteBuffer to release back to the pool
   */
  public void release(ByteBuffer buffer) {
    int normalizedSize = normalizeSize(buffer.capacity());
    poolMap.computeIfAbsent(normalizedSize, s -> new ConcurrentLinkedQueue<>());
    Queue<SoftReference<ByteBuffer>> pool = poolMap.get(normalizedSize);

    if (pool.size() < maxPoolSizePerBucket) {
      pool.offer(new SoftReference<>(buffer));
    }
  }

  /**
   * Returns the number of available buffers of the specified size in the pool.
   *
   * @param size the size of buffers to count
   * @return the number of available buffers of the specified size
   */
  public int available(int size) {
    int normalizedSize = normalizeSize(size);
    Queue<SoftReference<ByteBuffer>> pool = poolMap.get(normalizedSize);
    if (pool == null) {
      return 0;
    }

    return (int) pool.stream().filter(ref -> ref.get() != null).count();
  }

  /**
   * Normalizes the requested buffer size to the next power of two that is at least
   * as large as the requested size, but not smaller than the default buffer size.
   *
   * @param size the requested buffer size
   * @return the normalized buffer size
   */
  private int normalizeSize(int size) {
    if (size <= 0) {
      return bufferSize;
    }
    int normalized = Integer.highestOneBit(size - 1) << 1;
    return Math.max(bufferSize, normalized);
  }
}
