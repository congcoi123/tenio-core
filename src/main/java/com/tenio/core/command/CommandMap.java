package com.tenio.core.command;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.bootstrap.annotation.Command;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public final class CommandMap extends SystemLogger {

  private final Map<String, AbstractCommandHandler> commands = new TreeMap<>();
  private final Map<String, Command> annotations = new TreeMap<>();

  /**
   * Register a command handler.
   *
   * @param label   The command label.
   * @param command The command handler.
   * @return Instance chaining.
   */
  public CommandMap registerCommand(String label, AbstractCommandHandler command) {
    debug("COMMAND", "Registered command: " + label);
    label = label.toLowerCase();

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
  public CommandMap unregisterCommand(String label) {
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
}
