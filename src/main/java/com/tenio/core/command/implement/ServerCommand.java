package com.tenio.core.command.implement;

import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.command.AbstractCommandHandler;
import com.tenio.core.command.CommandMap;
import java.util.List;

@Command(label = "server", usage = {
    "stop",
    "restart",
}, description = "Allow stopping or restarting the server")
public final class ServerCommand extends AbstractCommandHandler {

  public ServerCommand(CommandMap commandMap) {
    super(commandMap);
  }

  @Override
  public void execute(List<String> args) {
    System.out.println("The feature is not available at the moment");
  }
}
