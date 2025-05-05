package com.backblaze.erasure;

/**
 * Mock implementation of the ReedSolomon class for testing purposes.
 * This is used to resolve the dependency issue with the kcp-base library.
 */
public class ReedSolomon {
    
    /**
     * Creates a new ReedSolomon instance.
     *
     * @param dataShards The number of data shards
     * @param parityShards The number of parity shards
     * @return A new ReedSolomon instance
     */
    public static ReedSolomon create(int dataShards, int parityShards) {
        return new ReedSolomon();
    }
    
    /**
     * Encodes the given data.
     *
     * @param shards The data to encode
     */
    public void encodeParity(byte[][] shards, int offset, int byteCount) {
        // Mock implementation - does nothing
    }
    
    /**
     * Reconstructs the data.
     *
     * @param shards The data to reconstruct
     * @param shardPresent Indicates which shards are present
     * @param offset The offset in the data
     * @param byteCount The number of bytes
     * @return Whether the data was successfully reconstructed
     */
    public boolean isParityCorrect(byte[][] shards, int offset, int byteCount) {
        return true;
    }
    
    /**
     * Reconstructs the data.
     *
     * @param shards The data to reconstruct
     * @param shardPresent Indicates which shards are present
     * @param offset The offset in the data
     * @param byteCount The number of bytes
     * @return Whether the data was successfully reconstructed
     */
    public boolean reconstruct(byte[][] shards, boolean[] shardPresent, int offset, int byteCount) {
        return true;
    }
} 