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

package com.tenio.core.entity.replication;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReplicatedStateTest {
  static final ReplicatedState.Audience OWNER = new ReplicatedState.Audience("alice", true, false);
  static final ReplicatedState.Audience MEMBER = new ReplicatedState.Audience("bob", true, false);
  static final ReplicatedState.Audience LOBBY = new ReplicatedState.Audience("carol", false, true);

  static Map<String, ReplicatedState.Definition> schema() {
    return Map.of(
        "secret",
        field(ReplicatedState.Visibility.SERVER, false),
        "private",
        field(ReplicatedState.Visibility.OWNER, false),
        "room",
        field(ReplicatedState.Visibility.ROOM, true),
        "public",
        field(ReplicatedState.Visibility.LOBBY, false));
  }

  static ReplicatedState.Definition field(ReplicatedState.Visibility visibility, boolean writable) {
    return new ReplicatedState.Definition(ReplicatedState.Type.STRING, visibility, writable, 32);
  }

  static ReplicatedState state() {
    return new ReplicatedState("alice", false, schema(), 1024, 5);
  }

  @Test
  void scopesHideSecretsAndSnapshotsAreImmutable() {
    try (var state = state()) {
      state.updateServer(
          Map.of("secret", "token", "private", "owner", "room", "ready", "public", "pong"), -1);
      assertEquals(
          Map.of("private", "owner", "room", "ready", "public", "pong"),
          state.snapshot(OWNER).values());
      assertEquals(Map.of("room", "ready", "public", "pong"), state.snapshot(MEMBER).values());
      assertEquals(Map.of("public", "pong"), state.snapshot(LOBBY).values());
      assertThrows(
          UnsupportedOperationException.class, () -> state.snapshot(OWNER).values().put("x", "x"));
    }
  }

  @Test
  void ownerPermissionAndRevisionChecksAreAtomic() {
    try (var state = state()) {
      assertEquals(1, state.updateClient(OWNER, Map.of("room", "ready"), 0));
      assertThrows(
          SecurityException.class, () -> state.updateClient(MEMBER, Map.of("room", "bad"), 1));
      assertThrows(
          SecurityException.class, () -> state.updateClient(OWNER, Map.of("public", "bad"), 1));
      assertThrows(
          IllegalStateException.class, () -> state.updateClient(OWNER, Map.of("room", "bad"), 0));
      assertThrows(
          IllegalArgumentException.class,
          () -> state.updateServer(Map.of("room", "changed", "public", 42), 1));
      assertEquals("ready", state.snapshot(OWNER).values().get("room"));
      assertEquals(1, state.snapshot(OWNER).revision());
    }
  }

  @Test
  void boundedUtf8ValuesAndNoOpBatchesDoNotCorruptState() {
    try (var state = state()) {
      assertThrows(
          IllegalArgumentException.class,
          () -> state.updateServer(Map.of("room", "🚀".repeat(9)), -1));
      assertThrows(
          IllegalArgumentException.class, () -> state.updateServer(Map.of("unknown", "value"), -1));
      long cursor = state.updateServer(Map.of("room", "ready"), -1);
      assertEquals(cursor, state.updateServer(Map.of("room", "ready"), -1));
      var deletion = new HashMap<String, Object>();
      deletion.put("room", null);
      state.updateServer(deletion, cursor);
      assertTrue(state.snapshot(OWNER).values().isEmpty());
    }
  }

  @Test
  void initialSnapshotHasNoGapAndPollRechecksCurrentMembership() {
    try (var state = state()) {
      var subscription = state.subscribe("bob");
      state.updateServer(Map.of("room", "initial"), -1);
      assertEquals(1, subscription.snapshot(MEMBER).revision());
      assertNull(subscription.poll(MEMBER));
      state.updateServer(Map.of("room", "new", "public", "lobby"), -1);
      var noLongerMember = new ReplicatedState.Audience("bob", false, false);
      var change = subscription.poll(noLongerMember);
      assertEquals(2, change.revision());
      assertTrue(change.values().isEmpty());
      assertThrows(SecurityException.class, () -> subscription.poll(OWNER));
    }
  }

  @Test
  void slowSubscriberGetsResetInsteadOfAnIncompleteState() {
    try (var state = state()) {
      var subscription = state.subscribe("bob");
      subscription.snapshot(MEMBER);
      for (int i = 0; i < 150; i++) {
        state.updateServer(Map.of("room", "phase" + i), -1);
      }
      assertTrue(subscription.poll(MEMBER).resync());
      var snapshot = subscription.snapshot(MEMBER);
      assertEquals("phase149", snapshot.values().get("room"));
      state.updateServer(Map.of("room", "new"), snapshot.revision());
      assertEquals("new", subscription.poll(MEMBER).values().get("room"));
    }
  }

  @Test
  void registryCleansRemovedEntitiesAndSubscriptions() {
    try (var registry = new StateReplication(2, 1024, 5)) {
      var player = registry.createPlayer("alice", schema());
      var room = registry.createRoom(1, "alice", schema());
      var subscription = room.subscribe("alice");
      subscription.snapshot(OWNER);
      registry.removePlayer("alice");
      assertNull(registry.player("alice"));
      assertThrows(SecurityException.class, () -> subscription.poll(OWNER));
      assertThrows(
          IllegalStateException.class, () -> player.updateServer(Map.of("room", "closed"), -1));
      registry.removeRoom(1);
      assertNull(registry.room(1));
      assertThrows(IllegalStateException.class, () -> room.snapshot(OWNER));
    }
  }
}
