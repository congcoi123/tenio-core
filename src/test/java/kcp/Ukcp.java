package kcp;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of the Ukcp class for testing purposes.
 * This is used to resolve the dependency issue with the kcp-base library.
 */
public class Ukcp {
    
    private InetSocketAddress remoteAddress;
    private Object user;
    private boolean active = true;
    
    /**
     * Sets the remote address.
     *
     * @param remoteAddress The remote address
     */
    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
    
    /**
     * Gets the remote address.
     *
     * @return The remote address
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }
    
    /**
     * Sets the user object.
     *
     * @param user The user object
     */
    public void setUser(Object user) {
        this.user = user;
    }
    
    /**
     * Gets the user object.
     *
     * @return The user object
     */
    public Object getUser() {
        return user;
    }
    
    /**
     * Checks if the connection is active.
     *
     * @return Whether the connection is active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Closes the connection.
     */
    public void close() {
        active = false;
    }
    
    /**
     * Sends data.
     *
     * @param data The data to send
     */
    public void send(byte[] data) {
        // Mock implementation - does nothing
    }
    
    /**
     * Receives data.
     *
     * @return The received data
     */
    public List<byte[]> receive() {
        return new ArrayList<>();
    }
    
    /**
     * Updates the connection.
     */
    public void update() {
        // Mock implementation - does nothing
    }
    
    /**
     * Sends a message.
     *
     * @param buf The buffer to send
     * @return The number of bytes sent
     */
    public int write(byte[] buf) {
        return 0;
    }
    
    /**
     * Sends a message with an offset and length.
     *
     * @param buf The buffer to send
     * @param offset The offset in the buffer
     * @param length The length of data to send
     * @return The number of bytes sent
     */
    public int write(byte[] buf, int offset, int length) {
        return 0;
    }
    
    /**
     * Gets the conversation ID.
     *
     * @return The conversation ID
     */
    public long getConv() {
        return 0;
    }
} 