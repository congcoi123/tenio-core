import com.tenio.core.utility.ThreadUtility;

public class SystemCommandManager {

  protected void initialize() {
    // Create a virtual thread executor for command processing
    executorService = ThreadUtility.newVirtualThreadExecutor("command-worker-%d");
  }
} 