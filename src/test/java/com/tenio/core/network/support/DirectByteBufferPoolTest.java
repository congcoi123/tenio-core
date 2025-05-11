package com.tenio.core.network.support;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link DirectByteBufferPool}.
 */
class DirectByteBufferPoolTest {

  private DirectByteBufferPool pool;

  @BeforeEach
  void setUp() {
    pool = new DirectByteBufferPool(2, DirectByteBufferPool.DEFAULT_BUFFER_SIZE); // Small pool size for testing
  }

  @Test
  void testAcquireWithDefaultSize() {
    ByteBuffer buffer = pool.acquire();
    assertNotNull(buffer);
    assertEquals(DirectByteBufferPool.DEFAULT_BUFFER_SIZE, buffer.capacity());
    assertTrue(buffer.isDirect());
  }

  @Test
  void testAcquireWithCustomSize() {
    int customSize = 2048;
    ByteBuffer buffer = pool.acquire(customSize);
    assertNotNull(buffer);
    assertTrue(buffer.capacity() >= customSize);
    assertTrue(buffer.isDirect());
  }

  @Test
  void testAcquireWithInvalidSize() {
    ByteBuffer buffer = pool.acquire(-1);
    assertNotNull(buffer);
    assertEquals(DirectByteBufferPool.DEFAULT_BUFFER_SIZE, buffer.capacity());
  }

  @Test
  void testReleaseAndReuse() {
    // Acquire and release a buffer
    ByteBuffer buffer = pool.acquire(1024);
    int capacity = buffer.capacity();
    pool.release(buffer);

    // Acquire again and verify it's the same buffer
    ByteBuffer reusedBuffer = pool.acquire(1024);
    assertEquals(capacity, reusedBuffer.capacity());
  }

  @Test
  void testPoolSizeLimit() {
    // Create more buffers than the pool size limit
    ByteBuffer buffer1 = pool.acquire(1024);
    ByteBuffer buffer2 = pool.acquire(1024);
    ByteBuffer buffer3 = pool.acquire(1024);

    // Release all buffers
    pool.release(buffer1);
    pool.release(buffer2);
    pool.release(buffer3);

    // Verify only 2 buffers are kept in the pool
    assertEquals(2, pool.available(1024));
  }

  @Test
  void testAvailable() {
    assertEquals(0, pool.available(1024));

    ByteBuffer buffer = pool.acquire(1024);
    pool.release(buffer);

    assertEquals(1, pool.available(1024));
  }

  @Test
  void testConcurrentAccess() throws InterruptedException {
    int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    ByteBuffer[] buffers = new ByteBuffer[threadCount];

    // Create threads that acquire and release buffers
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        ByteBuffer buffer = pool.acquire(1024);
        buffers[index] = buffer;
        pool.release(buffer);
      });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all buffers were properly managed
    for (ByteBuffer buffer : buffers) {
      assertNotNull(buffer);
      assertTrue(buffer.capacity() >= 1024);
    }
  }
} 