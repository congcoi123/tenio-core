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

package com.tenio.core.command.client;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.data.ServerMessage;
import com.tenio.core.exception.AddedDuplicatedClientCommandException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.reflections.Reflections;

/**
 * The commands' management class.
 *
 * @since 0.4.0
 */
@Component
public final class ClientCommandManager extends SystemLogger {

  private final Map<Short, AbstractClientCommandHandler> commands = new TreeMap<>();
  private final Map<Short, ClientCommand> annotations = new TreeMap<>();

  /**
   * Registers a command handler.
   *
   * @param code   The command code
   * @param command The command handler
   */
  public void registerCommand(Short code, AbstractClientCommandHandler command) {
    debug("CLIENT_COMMAND", "Registered command > " + code);

    // checks availability
    if (commands.containsKey(code)) {
      throw new AddedDuplicatedClientCommandException(code, commands.get(code));
    }

    // gets command data
    var annotation = command.getClass().getAnnotation(ClientCommand.class);
    annotations.put(code, annotation);
    commands.put(code, command);
  }

  /**
   * Removes a registered command handler.
   *
   * @param code The command code
   */
  public void unregisterCommand(Short code) {
    debug("COMMAND", "Unregistered command: " + code);

    annotations.remove(code);
    commands.remove(code);
  }

  public List<ClientCommand> getAnnotationsAsList() {
    return new LinkedList<>(annotations.values());
  }

  public Map<Short, ClientCommand> getAnnotations() {
    return new LinkedHashMap<>(annotations);
  }

  /**
   * Returns a list of all registered commands.
   *
   * @return all command handlers as a list
   */
  public List<AbstractClientCommandHandler> getHandlersAsList() {
    return new LinkedList<>(commands.values());
  }

  public Map<Short, AbstractClientCommandHandler> getHandlers() {
    return commands;
  }

  /**
   * Returns a handler by its code
   *
   * @param code The command code
   * @return the command handler
   */
  public AbstractClientCommandHandler getHandler(Short code) {
    return commands.get(code);
  }

  /**
   * Invokes a command handler with given arguments.
   *
   * @param code The messaged used to invoke the command
   */
  public void invoke(Short code, Player player, ServerMessage message) {
    // gets command handler
    var handler = getHandler(code);

    // checks if the handler is null
    if (Objects.isNull(handler)) {
      return;
    }

    // gets the command's annotation
    var annotation = annotations.get(code);

    // invokes execute method for handler
    Runnable runnable = () -> handler.execute(player, message);
    runnable.run();
  }

  /**
   * Scans for all classes annotated with {@link ClientCommand} and registers them.
   *
   * @param packages a list of packages' names. It allows to define the scanning packages by
   *                 their names
   * @throws IllegalArgumentException it is related to the illegal argument exception
   * @throws SecurityException        it is related to the security exception
   */
  public void scanPackages(String... packages)
      throws IllegalArgumentException, SecurityException {

    // clean maps data first
    commands.clear();
    annotations.clear();

    // start scanning
    var setPackageNames = new HashSet<String>();

    if (Objects.nonNull(packages)) {
      setPackageNames.addAll(Arrays.asList(packages));
    }

    // declares a reflection object based on the package of root class
    var reflections = new Reflections();
    for (var packageName : setPackageNames) {
      var reflectionPackage = new Reflections(packageName);
      reflections.merge(reflectionPackage);
    }

    var classes = reflections.getTypesAnnotatedWith(ClientCommand.class);
    classes.forEach(annotated -> {
      try {
        var commandData = annotated.getAnnotation(ClientCommand.class);
        var object = annotated.getDeclaredConstructor().newInstance();
        if (object instanceof AbstractClientCommandHandler command) {
          command.setCommandManager(this);
          registerCommand(commandData.value(), command);
        } else {
          error(new IllegalArgumentException("Class " + annotated.getName() + " is not a " +
              "AbstractClientCommandHandler"));
        }
      } catch (Exception exception) {
        error(exception, "Failed to register command handler for " + annotated.getSimpleName());
      }
    });
  }
}
