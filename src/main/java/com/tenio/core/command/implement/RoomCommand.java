package com.tenio.core.command.implement;

import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.command.AbstractCommandHandler;
import com.tenio.core.command.CommandMap;
import java.util.List;

@Command(label = "room", usage = {
    "add <name>",
    "remove <name>",
    "list"
}, description = "Allow managing rooms on the game server")
public final class RoomCommand extends AbstractCommandHandler {

  public RoomCommand(CommandMap commandMap) {
    super(commandMap);
  }

  @Override
  public void execute(List<String> args) {
    System.out.println("The feature is not available at the moment");
  }
}
