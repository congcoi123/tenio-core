package com.tenio.core.command.implement;

import com.tenio.core.bootstrap.annotation.Command;
import com.tenio.core.command.AbstractCommandHandler;
import com.tenio.core.command.CommandMap;
import java.util.List;

@Command(label = "help", usage = {
    "[<address>]"
}, description = "Allow removing banned Ip addresses from the ban list")
public final class UnBanAddressesCommand extends AbstractCommandHandler {

  public UnBanAddressesCommand(CommandMap commandMap) {
    super(commandMap);
  }

  @Override
  public void execute(List<String> args) {
    System.out.println("The feature is not available at the moment");
  }
}
