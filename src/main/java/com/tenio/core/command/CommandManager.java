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

package com.tenio.core.command;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.bootstrap.annotation.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.reflections.Reflections;

@Component
public final class CommandManager extends SystemLogger {

  private final Map<String, AbstractCommandHandler> commands = new TreeMap<>();
  private final Map<String, Command> annotations = new TreeMap<>();

  /**
   * Register a command handler.
   *
   * @param label   The command label.
   * @param command The command handler.
   * @return Instance chaining.
   */
  public CommandManager registerCommand(String label, AbstractCommandHandler command) {
    debug("COMMAND", "Registered command: " + label);
    label = label.toLowerCase();

    // Check availability
    if (commands.containsKey(label)) {
      throw new RuntimeException("Duplicated Command: " + label + " > " + commands.get(label).getClass().getName());
    }

    // Get command data.
    var annotation = command.getClass().getAnnotation(Command.class);
    this.annotations.put(label, annotation);
    this.commands.put(label, command);

    return this;
  }

  /**
   * Removes a registered command handler.
   *
   * @param label The command label.
   * @return Instance chaining.
   */
  public CommandManager unregisterCommand(String label) {
    debug("COMMAND", "Unregistered command: " + label);

    AbstractCommandHandler handler = this.commands.get(label);
    if (handler == null) {
      return this;
    }

    var annotation = handler.getClass().getAnnotation(Command.class);
    this.annotations.remove(label);
    this.commands.remove(label);

    return this;
  }

  public List<Command> getAnnotationsAsList() {
    return new LinkedList<>(this.annotations.values());
  }

  public Map<String, Command> getAnnotations() {
    return new LinkedHashMap<>(this.annotations);
  }

  /**
   * Returns a list of all registered commands.
   *
   * @return All command handlers as a list.
   */
  public List<AbstractCommandHandler> getHandlersAsList() {
    return new LinkedList<>(this.commands.values());
  }

  public Map<String, AbstractCommandHandler> getHandlers() {
    return this.commands;
  }

  /**
   * Returns a handler by label/alias.
   *
   * @param label The command label.
   * @return The command handler.
   */
  public AbstractCommandHandler getHandler(String label) {
    return this.commands.get(label);
  }

  /**
   * Invoke a command handler with the given arguments.
   *
   * @param rawMessage The messaged used to invoke the command.
   */
  public void invoke(String rawMessage) {
    // The console outputs in-game command
    debug("COMMAND", "Command used by server console: " + rawMessage);

    rawMessage = rawMessage.trim();
    if (rawMessage.length() == 0) {
      return;
    }

    // Parse message.
    String[] split = rawMessage.split(" ");
    List<String> args = new LinkedList<>(Arrays.asList(split));
    String label = args.remove(0).toLowerCase();

    // Get command handler.
    var handler = getHandler(label);

    // Check if the handler is null.
    if (handler == null) {
      return;
    }

    // Get the command's annotation.
    var annotation = this.annotations.get(label);

    // Invoke execute method for handler.
    Runnable runnable = () -> handler.execute(args);
    if (annotation.isBackgroundRunning()) {
      new Thread(runnable).start();
    } else {
      runnable.run();
    }
  }

  /**
   * Scans for all classes annotated with {@link Command} and registers them.
   */
  public void scanPackages(Class<?> entryClass, String... packages)
      throws IllegalArgumentException, SecurityException {

    // clean first
    commands.clear();
    annotations.clear();

    // start scanning
    var setPackageNames = new HashSet<String>();

    if (Objects.nonNull(entryClass)) {
      setPackageNames.add(entryClass.getPackage().getName());
    }

    if (Objects.nonNull(packages)) {
      setPackageNames.addAll(Arrays.asList(packages));
    }

    // declares a reflection object based on the package of root class
    var reflections = new Reflections();
    for (var packageName : setPackageNames) {
      var reflectionPackage = new Reflections(packageName);
      reflections.merge(reflectionPackage);
    }

    Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Command.class);

    classes.forEach(annotated -> {
      try {
        Command cmdData = annotated.getAnnotation(Command.class);
        Object object = annotated.getDeclaredConstructor().newInstance();
        if (object instanceof AbstractCommandHandler) {
          var command = (AbstractCommandHandler) object;
          command.setCommandInjector(this);
          this.registerCommand(cmdData.label(), command);
        } else {
          error(new IllegalArgumentException("Class " + annotated.getName() + " is not a " +
              "CommandHandler!"));
        }
      } catch (Exception exception) {
        error(exception, "Failed to register command handler for " + annotated.getSimpleName());
      }
    });
  }
}
