package kcp;

import java.net.InetSocketAddress;

/**
 * Mock implementation of the KcpClient class for testing purposes.
 * This is used to resolve the dependency issue with the kcp-base library.
 */
public class KcpClient {
    
    /**
     * Creates a new KcpClient instance.
     */
    public KcpClient() {
        // Mock implementation
    }
    
    /**
     * Connects to a remote address.
     *
     * @param address The remote address
     * @return A Ukcp instance
     */
    public Ukcp connect(InetSocketAddress address) {
        Ukcp ukcp = new Ukcp();
        ukcp.setRemoteAddress(address);
        return ukcp;
    }
    
    /**
     * Starts the client.
     */
    public void start() {
        // Mock implementation - does nothing
    }
    
    /**
     * Stops the client.
     */
    public void stop() {
        // Mock implementation - does nothing
    }
} 