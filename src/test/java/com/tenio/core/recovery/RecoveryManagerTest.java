/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.recovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.zero.utility.ZeroUtility;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.network.entity.session.Session;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RecoveryManagerTest {
  static class Time extends Clock {
    final AtomicLong millis = new AtomicLong(1000);

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(millis.get());
    }

    @Override
    public long millis() {
      return millis.get();
    }
  }

  static class Policy implements RecoveryPolicy {
    Strategy selected = Strategy.REPLAY;

    @Override
    public int maxReplayEvents() {
      return 2;
    }

    @Override
    public Strategy strategy() {
      return selected;
    }

    @Override
    public Credentials credentials(Session session, DataCollection message) {
      return null;
    }

    @Override
    public Snapshot snapshot(Player player, long cursor) {
      return new Snapshot(cursor, ZeroUtility.newZeroMap().putInteger("score", 7));
    }

    @Override
    public void resumed(Player player, Session session, RecoveryManager.Result result) {}
  }

  static Player player(String identity) {
    Player player = mock(Player.class);
    when(player.getIdentity()).thenReturn(identity);
    when(player.isLoggedIn()).thenReturn(true);
    return player;
  }

  @Test
  void tokenIsIdentityBoundSingleUseAndRotatedAfterResume() {
    var policy = new Policy();
    var player = player("alice");
    try (var recovery = new RecoveryManager(policy, ignored -> {})) {
      String token = recovery.issue(player);
      assertNull(recovery.claim(new RecoveryPolicy.Credentials("bob", token, 0)));
      assertNull(recovery.claim(new RecoveryPolicy.Credentials("alice", "invalid", 0)));
      var claim = recovery.claim(new RecoveryPolicy.Credentials("alice", token, 0));
      assertNotNull(claim);
      assertTrue(recovery.isRecovering(player));
      assertNull(recovery.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
      var result = recovery.complete(claim);
      assertNotEquals(token, result.token());
      assertTrue(recovery.isRecovering(player));
      recovery.finish(claim);
      assertFalse(recovery.isRecovering(player));
      assertNull(recovery.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
      assertNotNull(recovery.claim(new RecoveryPolicy.Credentials("alice", result.token(), 0)));
    }
  }

  @Test
  void replayEvictionRejectsGapsAndSnapshotsCanRecoverThem() {
    var policy = new Policy();
    var player = player("alice");
    try (var recovery = new RecoveryManager(policy, ignored -> {})) {
      String token = recovery.issue(player);
      for (int i = 0; i < 3; i++) {
        recovery.record(player, ZeroUtility.newZeroMap().putInteger("n", i));
      }
      assertNull(recovery.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
      var claim = recovery.claim(new RecoveryPolicy.Credentials("alice", token, 1));
      var result = recovery.complete(claim);
      recovery.finish(claim);
      assertEquals(2, result.events().size());
      assertEquals(2, result.events().getFirst().sequence());
    }
  }

  @Test
  void expiresOnlyDisconnectedPlayersAndDoesNotExtendGraceOnRepeatedClose() {
    var policy = new Policy();
    var time = new Time();
    var expired = new ArrayList<Player>();
    var first = player("first");
    var active = player("active");
    try (var recovery = new RecoveryManager(policy, expired::add, time)) {
      String token = recovery.issue(first);
      recovery.issue(active);
      assertTrue(recovery.disconnected(first, ConnectionDisconnectMode.LOST_IN_READ));
      time.millis.set(30_999);
      recovery.expire();
      assertTrue(expired.isEmpty());
      recovery.disconnected(first, ConnectionDisconnectMode.EXCEPTION);
      time.millis.set(31_000);
      recovery.expire();
      assertEquals(java.util.List.of(first), expired);
      recovery.expire();
      assertEquals(1, expired.size());
      assertNull(recovery.claim(new RecoveryPolicy.Credentials("first", token, 0)));
    }
  }

  @Test
  void kickBanLogoutAreNotRecoverable() {
    for (var mode :
        java.util.List.of(
            ConnectionDisconnectMode.KICK,
            ConnectionDisconnectMode.BAN,
            ConnectionDisconnectMode.CLIENT_REQUEST,
            ConnectionDisconnectMode.SERVER_DOWN)) {
      var p = player("alice");
      try (var manager = new RecoveryManager(new Policy(), ignored -> {})) {
        String token = manager.issue(p);
        assertFalse(manager.disconnected(p, mode));
        assertNull(manager.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
      }
    }
  }

  @Test
  void onlyOneConcurrentClaimWins() throws Exception {
    var p = player("alice");
    try (var manager = new RecoveryManager(new Policy(), ignored -> {});
        var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var credentials = new RecoveryPolicy.Credentials("alice", manager.issue(p), 0);
      var tasks = new ArrayList<java.util.concurrent.Callable<RecoveryManager.Claim>>();
      for (int i = 0; i < 50; i++) {
        tasks.add(() -> manager.claim(credentials));
      }
      int winners = 0;
      for (var result : workers.invokeAll(tasks)) {
        if (result.get() != null) {
          winners++;
        }
      }
      assertEquals(1, winners);
    }
  }

  @Test
  void enforcesReplayBytesAndDoesNotExposeMutableBuffersOrSecrets() {
    var p = player("alice");
    var policy =
        new Policy() {
          @Override
          public int maxReplayBytes() {
            return 64;
          }
        };
    try (var manager = new RecoveryManager(policy, ignored -> {})) {
      String token = manager.issue(p);
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.record(p, ZeroUtility.newZeroMap().putString("large", "x".repeat(100))));
      manager.record(p, ZeroUtility.newZeroMap().putInteger("n", 1));
      var credentials = new RecoveryPolicy.Credentials("alice", token, 0);
      assertFalse(credentials.toString().contains(token));
      var claim = manager.claim(credentials);
      var result = manager.complete(claim);
      byte[] data = result.events().getFirst().bytes();
      data[0] = 100;
      assertNotEquals(100, result.events().getFirst().bytes()[0]);
      assertFalse(result.toString().contains(result.token()));
    }
  }

  @Test
  void replayAgeAndReadyBarrierAreBoundedAndReleasedByCompletion() throws Exception {
    var time = new Time();
    var p = player("alice");
    try (var manager = new RecoveryManager(new Policy(), ignored -> {}, time);
        var worker = Executors.newVirtualThreadPerTaskExecutor()) {
      String token = manager.issue(p);
      manager.record(p, ZeroUtility.newZeroMap().putInteger("n", 1));
      time.millis.set(121_001);
      assertNull(manager.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
      var claim = manager.claim(new RecoveryPolicy.Credentials("alice", token, 1));
      assertNotNull(claim);
      var ready = worker.submit(() -> manager.awaitReady(p));
      assertFalse(ready.isDone());
      manager.complete(claim);
      manager.finish(claim);
      assertTrue(ready.get(5, java.util.concurrent.TimeUnit.SECONDS));
    }
  }

  @Test
  void snapshotsRecoverWhenEarlierReplayHasBeenEvicted() {
    var policy = new Policy();
    policy.selected = RecoveryPolicy.Strategy.SNAPSHOT_AND_REPLAY;
    var player = player("alice");
    try (var recovery = new RecoveryManager(policy, ignored -> {})) {
      String token = recovery.issue(player);
      for (int i = 0; i < 3; i++) {
        recovery.record(player, ZeroUtility.newZeroMap().putInteger("n", i));
      }
      var claim = recovery.claim(new RecoveryPolicy.Credentials("alice", token, 0));
      var result = recovery.complete(claim);
      recovery.finish(claim);
      assertNotNull(result.snapshot());
      assertTrue(result.events().isEmpty());
      assertEquals(3, result.sequence());
    }
  }

  @Test
  void playerCallbacksNeverExecuteUnderTheRecoveryMonitor() {
    var reference = new java.util.concurrent.atomic.AtomicReference<RecoveryManager>();
    Player p = mock(Player.class);
    when(p.getIdentity())
        .thenAnswer(
            call -> {
              assertFalse(Thread.holdsLock(reference.get()));
              return "alice";
            });
    when(p.isLoggedIn())
        .thenAnswer(
            call -> {
              assertFalse(Thread.holdsLock(reference.get()));
              return true;
            });
    try (var recovery = new RecoveryManager(new Policy(), ignored -> {})) {
      reference.set(recovery);
      String token = recovery.issue(p);
      recovery.record(p, ZeroUtility.newZeroMap());
      recovery.disconnected(p, ConnectionDisconnectMode.EXCEPTION);
      assertNotNull(recovery.claim(new RecoveryPolicy.Credentials("alice", token, 0)));
      recovery.revoke(p);
    }
  }
}
