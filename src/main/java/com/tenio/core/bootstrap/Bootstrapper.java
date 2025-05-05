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

package com.tenio.core.bootstrap;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.bootstrap.annotation.Bootstrap;
import com.tenio.core.bootstrap.injector.Injector;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

/**
 * The Bootstrapper is responsible for initializing and configuring the dependency injection system.
 * It works in conjunction with the {@link Injector} to scan packages, create beans, and wire
 * dependencies.
 * 
 * <p>Key responsibilities:
 * <ul>
 * <li>Package scanning for annotated classes</li>
 * <li>Bean creation and initialization</li>
 * <li>Dependency injection configuration</li>
 * <li>Servlet mapping setup</li>
 * <li>Command manager initialization</li>
 * </ul>
 * 
 * <p>The bootstrapper uses the following annotations to identify components:
 * <ul>
 * <li>{@code @Bootstrap} - Marks the application entry point</li>
 * <li>{@code @Component} - Identifies injectable components</li>
 * <li>{@code @Bean} - Marks methods that produce beans</li>
 * <li>{@code @RestController} - Identifies REST endpoints</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * &#64;Bootstrap
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         Bootstrapper bootstrapper = Bootstrapper.newInstance();
 *         bootstrapper.run(MyApplication.class, "com.example.package");
 *     }
 * }
 * </pre>
 * 
 * @see Bootstrap
 * @see Injector
 * @see BootstrapHandler
 */
public final class Bootstrapper extends SystemLogger {

  private static final Bootstrapper instance = new Bootstrapper();

  private final Injector injector;
  private BootstrapHandler bootstrapHandler;

  private Bootstrapper() {
    if (instance != null) {
      throw new CommandLine.InitializationException("Could not recreate this class instance");
    }
    injector = Injector.newInstance();
  }

  /**
   * Creates a new singleton instance of the Bootstrapper.
   * This method ensures that only one instance of the bootstrapper exists.
   *
   * @return the singleton {@link Bootstrapper} instance
   */
  public static Bootstrapper newInstance() {
    return instance;
  }

  /**
   * Starts the bootstrapping process for the application.
   * This method performs the following steps:
   * <ol>
   * <li>Validates the entry class has {@code @Bootstrap} annotation</li>
   * <li>Initializes the dependency injection system</li>
   * <li>Scans specified packages for components</li>
   * <li>Creates and configures the bootstrap handler</li>
   * <li>Sets up servlet mappings and command managers</li>
   * </ol>
   *
   * @param entryClass the {@link Class} which is placed in the root package and must be
   *                   annotated with {@code @Bootstrap}
   * @param packages   the scanning package names to search for components
   * @return {@code true} if bootstrapping was successful, {@code false} otherwise
   * @throws Exception when any initialization or configuration error occurs
   * 
   * @see Bootstrap
   * @see BootstrapHandler
   */
  public boolean run(Class<?> entryClass, String... packages) throws Exception {
    boolean hasExtApplicationAnnotation = entryClass.isAnnotationPresent(Bootstrap.class);

    if (hasExtApplicationAnnotation) {
      start(entryClass, packages);
      bootstrapHandler = injector.getBean(BootstrapHandler.class);
      bootstrapHandler.setServletMap(injector.getServletBeansMap());
      bootstrapHandler.setSystemCommandManager(injector.getSystemCommandManager());
      bootstrapHandler.setClientCommandManager(injector.getClientCommandManager());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Initializes the dependency injection system by scanning packages for components.
   * This method is synchronized to ensure thread-safe initialization of the injector.
   *
   * @param entryClass the root class for package scanning
   * @param packages   additional package names to scan
   */
  private void start(Class<?> entryClass, String... packages) {
    try {
      synchronized (Bootstrapper.class) {
        injector.scanPackages(entryClass, packages);
      }
    } catch (Exception exception) {
      if (isErrorEnabled()) {
        error(exception);
      }
    }
  }

  /**
   * Retrieves the bootstrap handler instance that was created during initialization.
   * The bootstrap handler manages the lifecycle of server components and handles
   * configuration after dependency injection is complete.
   *
   * @return the configured {@link BootstrapHandler} instance
   */
  public BootstrapHandler getBootstrapHandler() {
    return bootstrapHandler;
  }
}
