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

package com.tenio.core.entity.discovery;

import com.tenio.core.entity.Room;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomDirectoryTest {

    static Room room(long id) {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(id);
        when(room.getName()).thenReturn("Room " + id);
        when(room.getMaxParticipants()).thenReturn(2);
        when(room.getSnapshotParticipantCount()).thenReturn(1);
        when(room.isActivated()).thenReturn(true);
        return room;
    }

    @Test
    void firstPublicationEmitsAddAndIdenticalPublicationKeepsRevision() {
        try (var directory = new RoomDirectory(Set.of(), 1, 1)) {
            var subscription = directory.subscribe("alice", "pong");
            var room = room(1);

            directory.publish(room, "pong", "duel", "waiting", Map.of());

            var added = subscription.poll();
            assertNotNull(added);
            assertEquals(RoomDirectory.Kind.ADD, added.kind());
            assertEquals(1, added.roomId());
            assertEquals(1, added.revision());
            assertEquals("pong", added.summary().group());
            assertEquals(1, directory.query(RoomDirectory.Query.group("pong")).total());

            directory.publish(room, "pong", "duel", "waiting", Map.of());

            assertNull(subscription.poll());
            assertEquals(added.revision(), directory.query(RoomDirectory.Query.group("pong")).revision());
        }
    }

    @Test
    void changedSummaryInTheSameGroupEmitsUpdateAtCapacity() {
        try (var directory = new RoomDirectory(Set.of(), 1, 1)) {
            var subscription = directory.subscribe("alice", "pong");
            var room = room(1);
            directory.publish(room, "pong", "duel", "waiting", Map.of());
            var added = subscription.poll();

            directory.publish(room, "pong", "duel", "playing", Map.of());

            var updated = subscription.poll();
            assertNotNull(updated);
            assertEquals(RoomDirectory.Kind.UPDATE, updated.kind());
            assertEquals(added.revision() + 1, updated.revision());
            assertEquals(1, updated.roomId());
            assertEquals("playing", updated.summary().state());
            assertEquals(1, directory.query(RoomDirectory.Query.group("pong")).total());
            assertNull(subscription.poll());
        }
    }

    @Test
    void movingBetweenGroupsEmitsRemoveThenAddAndUpdatesBothIndexes() {
        try (var directory = new RoomDirectory(Set.of(), 1, 2)) {
            var oldGroup = directory.subscribe("alice", "pong");
            var newGroup = directory.subscribe("bob", "chess");
            var room = room(1);
            directory.publish(room, "pong", "duel", "waiting", Map.of());
            var initial = oldGroup.poll();
            assertNull(newGroup.poll());

            directory.publish(room, "chess", "duel", "waiting", Map.of());

            var removed = oldGroup.poll();
            var added = newGroup.poll();
            assertNotNull(removed);
            assertNotNull(added);
            assertEquals(RoomDirectory.Kind.REMOVE, removed.kind());
            assertEquals(RoomDirectory.Kind.ADD, added.kind());
            assertEquals(1, removed.roomId());
            assertEquals(1, added.roomId());
            assertNull(removed.summary());
            assertEquals("chess", added.summary().group());
            assertEquals(initial.revision() + 1, removed.revision());
            assertEquals(removed.revision() + 1, added.revision());
            assertEquals(0, directory.query(RoomDirectory.Query.group("pong")).total());
            assertEquals(1, directory.query(RoomDirectory.Query.group("chess")).total());
            assertNull(oldGroup.poll());
            assertNull(newGroup.poll());
        }
    }

    @Test
    void groupIndexPaginationAndAllowlistedFiltersWork() {
        try (var directory = new RoomDirectory(Set.of("rank"), 20, 5)) {
            for (long i = 0; i < 6; i++) {
                directory.publish(room(i), i < 5 ? "pong" : "chess", "duel", "waiting", Map.of("rank", 3));
            }
            var query = new RoomDirectory.Query("pong", "duel", "waiting", 1, Map.of("rank", 3), 2, 2);
            var page = directory.query(query);
            assertEquals(5, page.total());
            assertEquals(2, page.rooms().size());
            assertTrue(page.rooms().stream().allMatch(r -> r.group().equals("pong")));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> directory.query(new RoomDirectory.Query("pong", null, null, 0, Map.of("secret", 1), 0, 10))
            );
        }
    }

    @Test
    void roomPropertiesAreNeverCopiedAndSubscribersReceiveOnlyTheirGroup() {
        try (var directory = new RoomDirectory(Set.of("public"), 20, 5)) {
            var subscription = directory.subscribe("alice", "pong");
            var room = room(1);
            directory.publish(room, "chess", "duel", "waiting", Map.of());
            assertNull(subscription.poll());
            directory.publish(room, "pong", "duel", "waiting", Map.of("public", true));
            var change = subscription.poll();
            assertEquals(RoomDirectory.Kind.ADD, change.kind());
            assertEquals(Map.of("public", true), change.summary().attributes());
            verify(room, never()).getProperty(anyString());
            assertThrows(UnsupportedOperationException.class, () -> change.summary().attributes().put("x", 1));
            directory.remove(1);
            assertEquals(RoomDirectory.Kind.REMOVE, subscription.poll().kind());
        }
    }

    @Test
    void refreshIsExplicitAndSkipsUnpublishedRooms() {
        try (var directory = new RoomDirectory(Set.of(), 5, 5)) {
            Room room = room(1);
            directory.refresh(room);
            assertEquals(0, directory.query(RoomDirectory.Query.group("pong")).total());
            directory.publish(room, "pong", "duel", "waiting", Map.of());
            when(room.getSnapshotParticipantCount()).thenReturn(2);
            directory.refresh(room);
            assertEquals(
                    0,
                    directory.query(RoomDirectory.Query.group("pong")).rooms().getFirst().availableSlots()
            );
        }
    }

    @Test
    void overflowRequiresResnapshotAndAcknowledgementPreservesNewerChanges() {
        try (var directory = new RoomDirectory(Set.of(), 5, 5)) {
            var subscription = directory.subscribe("alice", "pong");
            var room = room(1);
            for (int i = 0; i < 150; i++) {
                directory.publish(room, "pong", "duel", "phase" + i, Map.of());
            }
            assertEquals(RoomDirectory.Kind.RESET, subscription.poll().kind());
            var page = directory.query(RoomDirectory.Query.group("pong"));
            directory.publish(room, "pong", "duel", "newest", Map.of());
            subscription.acknowledgeSnapshot(page.revision());
            var change = subscription.poll();
            assertNotNull(change);
            assertTrue(change.revision() > page.revision());
            directory.release("alice");
            directory.remove(1);
            assertNull(subscription.poll());
        }
    }

    @Test
    void resourceBoundsRejectUnboundedSubscriptionsAndFields() {
        try (var directory = new RoomDirectory(Set.of("safe"), 1, 1)) {
            directory.publish(room(1), "group", "game", "waiting", Map.of());
            assertThrows(
                    IllegalStateException.class,
                    () -> directory.publish(room(2), "group", "game", "waiting", Map.of())
            );
            directory.subscribe("alice", "group");
            assertThrows(IllegalStateException.class, () -> directory.subscribe("bob", "group"));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> directory.publish(room(1), "group", "game", "waiting", Map.of("safe", new Object()))
            );
        }
    }

    @Test
    void roomGettersRunOutsideTheDirectoryMonitor() {
        try (var directory = new RoomDirectory(Set.of(), 5, 5)) {
            Room room = room(1);
            when(room.getName())
                    .thenAnswer(
                            call -> {
                                assertFalse(Thread.holdsLock(directory));
                                return "safe";
                            }
                    );
            directory.publish(room, "group", "game", "waiting", Map.of());
            directory.refresh(room);
        }
    }

    @Test
    void refreshCannotResurrectARemovedRoom() throws Exception {
        try (var directory = new RoomDirectory(Set.of(), 5, 5);
             var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            Room room = room(1);
            directory.publish(room, "group", "game", "waiting", Map.of());
            var reading = new java.util.concurrent.CountDownLatch(1);
            var proceed = new java.util.concurrent.CountDownLatch(1);
            when(room.getName()).thenAnswer(
                    call -> {
                        reading.countDown();
                        assertTrue(proceed.await(5, java.util.concurrent.TimeUnit.SECONDS));
                        return "updated";
                    }
            );
            var refreshed = workers.submit(() -> directory.refresh(room));
            assertTrue(reading.await(5, java.util.concurrent.TimeUnit.SECONDS));
            directory.remove(1);
            proceed.countDown();
            refreshed.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(0, directory.query(RoomDirectory.Query.group("group")).total());
        }
    }
}
