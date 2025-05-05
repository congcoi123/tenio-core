package kcp;

import java.net.InetSocketAddress;

/**
 * Mock implementation of the KcpServer class for testing purposes.
 * This is used to resolve the dependency issue with the kcp-base library.
 */
public class KcpServer {
    
    /**
     * Creates a new KcpServer instance.
     */
    public KcpServer() {
        // Mock implementation
    }
    
    /**
     * Binds to a local address.
     *
     * @param address The local address
     */
    public void bind(InetSocketAddress address) {
        // Mock implementation - does nothing
    }
    
    /**
     * Starts the server.
     */
    public void start() {
        // Mock implementation - does nothing
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        // Mock implementation - does nothing
    }
} 