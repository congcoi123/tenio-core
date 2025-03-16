package com.backblaze.erasure;

/**
 * Mock implementation of the IFecEncode interface for testing purposes.
 * This is used to resolve the dependency issue with the kcp-base library.
 */
public interface IFecEncode {
    
    /**
     * Encodes data for FEC (Forward Error Correction).
     *
     * @param data The data to encode
     * @param offset The offset in the data
     * @param length The length of the data
     * @return The encoded data
     */
    byte[] encode(byte[] data, int offset, int length);
    
    /**
     * Releases resources.
     */
    void release();
} 