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

package com.tenio.core.command.client;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.common.data.zero.utility.ZeroUtility;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.entity.Player;
import com.tenio.core.network.entity.session.Session;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandExecutionTest {

    @Test
    void detachesQueuedInputAndPreservesSessionOrder() {
        var manager = new ClientCommandManager();
        var handler = new Ordered();
        Player player = mock(Player.class);
        Session session = mock(Session.class);
        when(player.getSession()).thenReturn(Optional.of(session));
        when(session.isActivated()).thenReturn(true);
        when(session.getId()).thenReturn(1L);
        try {
            manager.registerCommand((short) 10, handler);
            manager.invoke((short) 10, player, ZeroUtility.newZeroMap());
            CommandDispatcherTest.await(handler.entered);
            var input = ZeroUtility.newZeroMap().putString("value", "before");
            manager.invoke((short) 10, player, input);
            input.putString("value", "after");
            handler.release.countDown();
            CommandDispatcherTest.await(handler.done);
            assertEquals("before", handler.observed.get());
        } finally {
            handler.release.countDown();
            manager.clear();
        }
    }

    @Test
    void queuedCommandsFromReplacedSessionAreSuppressed() {
        var manager = new ClientCommandManager();
        var handler = new Ordered();
        Player player = mock(Player.class);
        Session session = mock(Session.class);
        var bound = new AtomicReference<>(session);
        when(player.getSession()).thenAnswer(call -> Optional.ofNullable(bound.get()));
        when(session.isActivated()).thenReturn(true);
        when(session.getId()).thenReturn(3L);
        try {
            manager.registerCommand((short) 10, handler);
            manager.invoke((short) 10, player, ZeroUtility.newZeroMap());
            CommandDispatcherTest.await(handler.entered);
            manager.invoke((short) 10, player, ZeroUtility.newZeroMap().putString("value", "stale"));
            bound.set(mock(Session.class));
            handler.release.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (manager.outstandingCommands() > 0 && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertEquals(0, manager.outstandingCommands());
            assertNull(handler.observed.get());
        } finally {
            handler.release.countDown();
            manager.clear();
        }
    }

    @Test
    void existingUnannotatedHandlersRemainSynchronous() {
        var manager = new ClientCommandManager();
        var called = new AtomicReference<Thread>();
        var handler = new AbstractClientCommandHandler<>() {
            @Override
            public void execute(Player player, DataCollection data) {
                called.set(Thread.currentThread());
            }
        };
        manager.registerCommand((short) 1, handler);
        manager.invoke((short) 1, null, null);
        assertSame(Thread.currentThread(), called.get());
        assertEquals(0, manager.outstandingCommands());
        manager.clear();
    }

    @Test
    void differentPlayersAndHandlersShareTheRoomMailboxBound() {
        var manager = new ClientCommandManager();
        var first = new RoomOrdered();
        var second = new RoomOrdered();
        var room = mock(com.tenio.core.entity.Room.class);
        when(room.getId()).thenReturn(88L);
        Player a = mock(Player.class);
        Player b = mock(Player.class);
        Session sa = mock(Session.class);
        Session sb = mock(Session.class);
        when(a.getSession()).thenReturn(Optional.of(sa));
        when(b.getSession()).thenReturn(Optional.of(sb));
        when(sa.getId()).thenReturn(1L);
        when(sb.getId()).thenReturn(2L);
        when(sa.isActivated()).thenReturn(true);
        when(sb.isActivated()).thenReturn(true);
        when(a.getCurrentRoom()).thenReturn(Optional.of(room));
        when(b.getCurrentRoom()).thenReturn(Optional.of(room));
        try {
            manager.registerCommand((short) 12, first);
            manager.registerCommand((short) 13, second);
            manager.invoke((short) 12, a, ZeroUtility.newZeroMap());
            CommandDispatcherTest.await(first.entered);
            manager.invoke((short) 13, b, ZeroUtility.newZeroMap());
            assertThrows(
                    RejectedExecutionException.class,
                    () -> manager.invoke((short) 13, b, ZeroUtility.newZeroMap())
            );
        } finally {
            first.release.countDown();
            second.release.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (manager.outstandingCommands() > 0 && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            manager.clear();
        }
    }

    @ClientCommand(value = 10, execution = ExecutionMode.SESSION, mailboxCapacity = 2)
    static class Ordered extends AbstractClientCommandHandler<Player, DataCollection> {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(2);
        final AtomicReference<String> observed = new AtomicReference<>();

        @Override
        public void execute(Player player, DataCollection message) {
            if (entered.getCount() > 0) {
                entered.countDown();
                CommandDispatcherTest.await(release);
            } else {
                observed.set(((ZeroMap) message).getString("value"));
            }
            done.countDown();
        }
    }

    @ClientCommand(value = 12, execution = ExecutionMode.ROOM, mailboxCapacity = 2)
    static class RoomOrdered extends Ordered {
    }
}
