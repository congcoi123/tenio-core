package com.tenio.core.command.implement;

import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.command.AbstractCommandHandler;
import com.tenio.core.command.CommandMap;
import java.util.ArrayList;
import java.util.List;

@Command(label = "help", usage = {
    "[<command>]"
})
public final class HelpCommand extends AbstractCommandHandler {

  public HelpCommand(CommandMap commandMap) {
    super(commandMap);
  }

  private String createCommand(AbstractCommandHandler command) {
    return command.getLabel() +
        " - " +
        command.getDescription() +
        "\n\t" +
        command.getUsage();
  }

  @Override
  public void execute(List<String> args) {
    var commandMap = getCommandMap();
    var commands = new ArrayList<>();
    if (args.isEmpty()) {
      commandMap.getHandlers().forEach((key, command) -> commands.add(createCommand(command)));
    } else {
      var commandLabel = args.remove(0).toLowerCase();
      var command = commandMap.getHandler(commandLabel);
      if (command == null) {
        System.out.println("Command: " + commandLabel + " does not exist.");
        return;
      } else {
        commands.add(createCommand(command));
      }
    }
    commands.forEach(System.out::println);
  }
}
