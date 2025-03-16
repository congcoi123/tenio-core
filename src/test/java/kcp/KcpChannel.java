package kcp;

/**
 * Mock implementation of the KcpChannel class for testing purposes.
 * This is used to resolve the dependency issue with the kcp-base library.
 */
public class KcpChannel {
    
    private Ukcp ukcp;
    
    /**
     * Creates a new KcpChannel instance.
     */
    public KcpChannel() {
        // Mock implementation
    }
    
    /**
     * Creates a new KcpChannel instance with a Ukcp instance.
     *
     * @param ukcp The Ukcp instance
     */
    public KcpChannel(Ukcp ukcp) {
        this.ukcp = ukcp;
    }
    
    /**
     * Gets the Ukcp instance.
     *
     * @return The Ukcp instance
     */
    public Ukcp getUkcp() {
        return ukcp;
    }
    
    /**
     * Sets the Ukcp instance.
     *
     * @param ukcp The Ukcp instance
     */
    public void setUkcp(Ukcp ukcp) {
        this.ukcp = ukcp;
    }
} 