package example;

import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.replication.ReplicatedState;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.handler.event.EventServerInitialization;
import java.util.Map;
import java.util.Set;

/** Copy into your bootstrap's scanned package; select capabilities-example at startup. */
@EventHandler(profile = "capabilities-example")
public final class ApplicationFeatures extends AbstractHandler
    implements EventServerInitialization {
  private Room room;
  private ReplicatedState state;

  @Override
  public void onServerInitialization(String name) {
    var replication = api().enableStateReplication(1000, 64 * 1024, 64);
    var directory = api().enableRoomDiscovery(Set.of("rank"), 1000, 1000);
    room =
        api()
            .createRoom(
                roomSetting()
                    .setName("Feature demo")
                    .setMaxParticipants(2)
                    .setMaxSpectators(10)
                    .setActivated(true)
                    .setRoomRemoveMode(RoomRemoveMode.NEVER_REMOVE)
                    .build(),
                null);
    if (room == null) {
      throw new IllegalStateException("Could not create demonstration room");
    }
    state =
        replication.createRoom(
            room.getId(),
            null,
            Map.of(
                "ready",
                    new ReplicatedState.Definition(
                        ReplicatedState.Type.BOOLEAN, ReplicatedState.Visibility.ROOM, false, 1),
                "mode",
                    new ReplicatedState.Definition(
                        ReplicatedState.Type.STRING, ReplicatedState.Visibility.LOBBY, false, 32)));
    state.updateServer(Map.of("ready", false, "mode", "duel"), -1);
    directory.publish(room, "demo", "duel", "waiting", Map.of("rank", 1));
  }

  /**
   * @return the explicitly published demonstration room
   */
  public Room room() {
    return room;
  }

  /**
   * @return its explicitly replicated state
   */
  public ReplicatedState state() {
    return state;
  }
}
