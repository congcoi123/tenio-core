package com.tenio.core.command.implement;

import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.command.AbstractCommandHandler;
import com.tenio.core.command.CommandMap;
import java.util.List;

@Command(label = "player", usage = {
    "add <name>",
    "remove <name>",
    "list"
}, description = "Allow managing players on the game server")
public final class PlayerCommand extends AbstractCommandHandler {


  public PlayerCommand(CommandMap commandMap) {
    super(commandMap);
  }

  @Override
  public void execute(List<String> args) {
    System.out.println("The feature is not available at the moment");
  }
}
