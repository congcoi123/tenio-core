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

package com.tenio.core;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.bootstrap.Bootstrapper;
import com.tenio.core.bootstrap.annotation.Bootstrap;
import com.tenio.core.configuration.constant.CoreConstant;
import com.tenio.core.configuration.constant.Trademark;
import com.tenio.core.monitoring.system.SystemInfo;
import com.tenio.core.server.ServerImpl;
import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.apache.logging.log4j.util.Strings;

/**
 * The ApplicationLauncher is the main entry point for starting the TenIO server application.
 * It handles the initialization and bootstrapping of the server components using dependency injection.
 * 
 * <p>Key responsibilities:
 * <ul>
 * <li>Application initialization and startup</li>
 * <li>Dependency injection bootstrapping</li>
 * <li>Command-line argument processing</li>
 * <li>Server configuration loading</li>
 * <li>Component scanning and initialization</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * public class MyGameServer {
 *     public static void main(String[] args) {
 *         ApplicationLauncher.run(MyGameServer.class, args);
 *     }
 * }
 * </pre>
 * 
 * <p>The launcher expects the entry class to be annotated with {@code @Bootstrap} to enable
 * component scanning and dependency injection.
 * 
 * @see Bootstrap
 * @see Bootstrapper
 */
public final class ApplicationLauncher extends SystemLogger {

  private static final ApplicationLauncher instance = new ApplicationLauncher();

  private ApplicationLauncher() {
    if (Objects.nonNull(instance)) {
      throw new CommandLine.InitializationException("Could not recreate this class instance");
    }
  }

  /**
   * Runs the application with the specified entry class and parameters.
   * This method initializes the dependency injection container, scans for components,
   * and starts the server.
   *
   * @param entryClass the {@link Class} which is placed in the root package and should be
   *                   annotated with {@code @Bootstrap}
   * @param params     additional command-line parameters for configuring the server
   * 
   * @throws CommandLine.InitializationException if the application cannot be initialized
   * @throws IllegalStateException if the entry class is not properly annotated
   */
  public static void run(Class<?> entryClass, String[] params) {
    var application = ApplicationLauncher.newInstance();
    application.start(entryClass, params);
  }

  /**
   * Creates a new singleton instance of the ApplicationLauncher.
   * This method ensures that only one instance of the launcher exists.
   *
   * @return the singleton {@link ApplicationLauncher} instance
   */
  private static ApplicationLauncher newInstance() {
    return instance;
  }

  /**
   * Starts the game server with dependency injection mechanism.
   * This method performs the following steps:
   * <ol>
   * <li>Validates the entry class annotations</li>
   * <li>Initializes the bootstrapper</li>
   * <li>Scans for components in the specified packages</li>
   * <li>Configures server settings</li>
   * <li>Starts server components</li>
   * </ol>
   *
   * @param entryClass the {@link Class} which is placed in the root package
   * @param params     additional command-line parameters
   * 
   * @throws IllegalStateException if server initialization fails
   */
  private void start(Class<?> entryClass, String[] params) {
    // print out the framework's preface
    if (isInfoEnabled()) {
      var trademark =
          String.format("\n\n%s\n", Strings.join(Arrays.asList(Trademark.CONTENT), '\n'));
      debug("HAPPY CODING", trademark);
    }

    // show system information
    var systemInfo = new SystemInfo();
    systemInfo.logSystemInfo();
    systemInfo.logNetCardsInfo();
    systemInfo.logDiskInfo();

    Bootstrapper bootstrap = null;
    if (Objects.nonNull(entryClass)) {
      bootstrap = Bootstrapper.newInstance();
      try {
        bootstrap.run(entryClass, CoreConstant.DEFAULT_BOOTSTRAP_PACKAGE,
            CoreConstant.DEFAULT_EVENT_PACKAGE, CoreConstant.DEFAULT_COMMAND_PACKAGE,
            CoreConstant.DEFAULT_REST_CONTROLLER_PACKAGE);
      } catch (Exception exception) {
        if (isErrorEnabled()) {
          error(exception, "The application started with exceptions occurred: ", exception.getMessage());
        }
        System.exit(1);
      }
    }

    var server = ServerImpl.getInstance();
    try {
      if (bootstrap == null) {
        throw new IllegalStateException("Bootstrap handler is not initialized");
      }
      server.start(bootstrap.getBootstrapHandler(), params);
    } catch (Exception exception) {
      if (isErrorEnabled()) {
        error(exception, "The application started with exceptions occurred: ",
            exception.getMessage());
      }
      server.shutdown();
      // exit with errors
      System.exit(1);
    }

    // Keep the main thread running
    try {
      var currentThread = Thread.currentThread();
      currentThread.setName("tenio-main-thread");
      currentThread.setUncaughtExceptionHandler((thread, cause) -> {
        if (isErrorEnabled()) {
          error(cause, thread.getName());
        }
      });
      currentThread.join();
    } catch (InterruptedException exception) {
      if (isErrorEnabled()) {
        error(exception);
      }
    }

    // Suddenly shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      server.shutdown();
      System.exit(0);
    }));
  }
}
