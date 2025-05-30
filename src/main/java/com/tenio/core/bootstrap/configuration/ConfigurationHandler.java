/*
The MIT License

Copyright (c) 2016-2025 kong <congcoi123@gmail.com>

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

package com.tenio.core.bootstrap.configuration;

import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.common.configuration.Configuration;

/**
 * Provides access to the server's configuration instance.
 * This class serves as a component that manages and provides access to the server's
 * configuration settings. It is designed to be used with dependency injection and
 * component scanning.
 *
 * <p>Key features:
 * <ul>
 *   <li>Configuration instance management</li>
 *   <li>Dependency injection support</li>
 *   <li>Component-based architecture</li>
 * </ul>
 *
 * <p>Note: This class is annotated with {@link Component} to enable automatic
 * component scanning and dependency injection. The configuration instance is
 * injected using the {@link Autowired} annotation.
 *
 * @see Component
 * @see Autowired
 * @see Configuration
 * @since 0.3.0
 */
@Component
public final class ConfigurationHandler {

  @Autowired
  private Configuration configuration;

  /**
   * Retrieves a configuration instance for the server.
   *
   * @return the server's {@link Configuration}
   */
  public Configuration getConfiguration() {
    return configuration;
  }
}
