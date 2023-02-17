/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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

import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.bootstrap.configuration.ConfigurationHandler;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.event.handler.EventHandler;
import com.tenio.core.network.jetty.servlet.RestServlet;
import java.util.Map;

/**
 * This class provides instances for the events handler and the configuration setups.
 */
@Component
public final class BootstrapHandler {

  @Autowired
  private EventHandler eventHandler;

  @Autowired
  private SystemCommandManager systemCommandManager;

  @Autowired
  private ConfigurationHandler configurationHandler;

  private Map<String, RestServlet> servletMap;

  /**
   * Retrieves an events handler.
   *
   * @return the {@link EventHandler} instance
   */
  public EventHandler getEventHandler() {
    return eventHandler;
  }

  /**
   * Retrieves a commands' manager.
   *
   * @return the {@link SystemCommandManager} instance
   * @since 0.4.0
   */
  public SystemCommandManager getCommandManager() {
    return systemCommandManager;
  }

  /**
   * Retrieves a configuration setups.
   *
   * @return the {@link ConfigurationHandler} instance
   */
  public ConfigurationHandler getConfigurationHandler() {
    return configurationHandler;
  }

  public Map<String, RestServlet> getServletMap() {
    return servletMap;
  }

  public void setServletMap(Map<String, RestServlet> servletMap) {
    this.servletMap = servletMap;
  }

  public void setCommandManager(SystemCommandManager systemCommandManager) {
    this.systemCommandManager = systemCommandManager;
  }
}
