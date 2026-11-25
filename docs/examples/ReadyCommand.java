package example;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ExecutionMode;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.replication.ReplicatedState;
import java.util.Map;

/** Application-selected command code. Authenticate/login/join using your existing handlers. */
@ClientCommand(
    value = 42,
    name = "ready",
    profile = "capabilities-example",
    execution = ExecutionMode.ROOM,
    mailboxCapacity = 64,
    requestsPerSecond = 2,
    burst = 4)
public final class ReadyCommand extends AbstractClientCommandHandler<Player, DataCollection> {
  @Autowired private ApplicationFeatures features;

  @Override
  public void execute(Player player, DataCollection message) {
    var room = features.room();
    var state = features.state();
    if (room == null
        || state == null
        || !(message instanceof ZeroMap request)
        || player.getCurrentRoom().orElse(null) != room
        || !request.containsKey("ready")) {
      return;
    }
    // Recovery callbacks and every other writer must share this context if they touch this state.
    synchronized (room) {
      state.updateServer(Map.of("ready", request.getBoolean("ready")), -1);
      var snapshot =
          state.snapshot(new ReplicatedState.Audience(player.getIdentity(), true, false));
      var content =
          map()
              .putLong("revision", snapshot.revision())
              .putBoolean("ready", (Boolean) snapshot.values().get("ready"));
      response().setContent(content).setRecipientPlayer(player).write();
    }
  }
}
