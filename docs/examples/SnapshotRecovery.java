package example;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataUtility;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.entity.Player;
import com.tenio.core.handler.AbstractHandler;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.recovery.RecoveryManager;
import com.tenio.core.recovery.RecoveryPolicy;
import java.util.function.Supplier;

/**
 * Application-specific recovery envelope. Add your authoritative gameplay fields to snapshot().
 * Enroll after successful login with api().getRecoveryManager().issue(player), and privately send
 * the returned token. The supplied room monitor must also guard all mutations and broadcasts.
 */
@Component(profile = "recovery-example")
public final class SnapshotRecovery extends AbstractHandler implements RecoveryPolicy {
  @Override
  public Credentials credentials(Session session, DataCollection message) {
    if (!(message instanceof ZeroMap request) || !request.containsKey("resume-token")) {
      return null;
    }
    return new Credentials(
        request.getString("identity"),
        request.getString("resume-token"),
        request.containsKey("sequence") ? request.getLong("sequence") : 0);
  }

  @Override
  public Snapshot snapshot(Player player, long cursor) {
    var data = map().putString("identity", player.getIdentity());
    player.getCurrentRoom().ifPresent(room -> data.putLong("room", room.getId()));
    return new Snapshot(cursor, data);
  }

  @Override
  public void recover(Player player, Session session, Supplier<RecoveryManager.Result> capture) {
    Object context = player.getCurrentRoom().<Object>map(room -> room).orElse(player);
    synchronized (context) {
      resumed(player, session, capture.get());
    }
  }

  @Override
  public void resumed(Player player, Session session, RecoveryManager.Result result) {
    var snapshot =
        (ZeroMap)
            DataUtility.binariesToCollection(result.snapshot().type(), result.snapshot().bytes());
    var content =
        map()
            .putString("resume-token", result.token())
            .putLong("sequence", result.sequence())
            .putZeroMap("snapshot", snapshot);
    response().setContent(content).setRecipientSession(session).write();
  }
}
