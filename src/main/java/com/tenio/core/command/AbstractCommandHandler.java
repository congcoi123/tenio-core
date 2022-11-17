package com.tenio.core.command;

import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.handler.AbstractHandler;
import java.util.List;

public abstract class AbstractCommandHandler extends AbstractHandler {

  private final CommandMap commandMap;

  public AbstractCommandHandler(CommandMap commandMap) {
    this.commandMap = commandMap;
  }

  public String getLabel() {
    return getClass().getAnnotation(Command.class).label();
  }

  public String getUsage() {
    return String.join("\n", getClass().getAnnotation(Command.class).usage());
  }

  public String getDescription() {
    return String.join("\n", getClass().getAnnotation(Command.class).description());
  }

  public boolean isRunningBackground() {
    return getClass().getAnnotation(Command.class).isBackgroundRunning();
  }

  public CommandMap getCommandMap() {
    return commandMap;
  }

  /**
   * Called when the server invokes a command.
   *
   * @param args The arguments to the command
   */
  public abstract void execute(List<String> args);
}
