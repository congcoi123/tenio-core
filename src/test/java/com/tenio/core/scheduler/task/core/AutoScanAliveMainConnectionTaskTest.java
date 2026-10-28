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

package com.tenio.core.scheduler.task.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.network.entity.session.Session;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutoScanAliveMainConnectionTaskTest {

  private AutoScanAliveMainConnectionTask task;
  private PlayerManager players;
  private Player player;
  private Session session;

  @BeforeEach
  void setUp() {
    task = new AutoScanAliveMainConnectionTask();
    players = mock(PlayerManager.class);
    player = mock(Player.class);
    session = mock(Session.class);
    task.setPlayerManager(players);
    task.setTimeoutSeconds(30);
    task.setInterval(1);
    when(players.getSnapshotPlayersList()).thenReturn(List.of(player));
    when(player.getSession()).thenReturn(Optional.of(session));
    when(session.isTcp()).thenReturn(true);
    when(session.isActivated()).thenReturn(true);
    when(session.getLastReliableReadTime()).thenReturn(1000L);
  }

  @AfterEach
  void tearDown() {
    task.shutdown();
  }

  @Test
  void closesAtDeadlineUsingConnectionLostPolicy() throws IOException {
    task.scan(31000L);
    verify(session).close(ConnectionDisconnectMode.IDLE, PlayerDisconnectMode.CONNECTION_LOST);
    verifyNoInteractionsExceptRead();
  }

  private void verifyNoInteractionsExceptRead() {
    // No logout, registry removal or application room operation belongs in this task.
    verify(players).getSnapshotPlayersList();
    verifyNoMoreInteractions(players);
  }

  @Test
  void recentTcpTrafficKeepsSessionAlive() throws IOException {
    task.scan(30999L);
    verify(session, never()).close(any(), any());
  }

  @Test
  void renewedReliableTimestampExtendsDeadline() throws IOException {
    when(session.getLastReliableReadTime()).thenReturn(30000L);
    task.scan(31000L);
    verify(session, never()).close(any(), any());
  }

  @Test
  void generalReadsAndOutboundWritesCannotConcealSilentTcp() throws IOException {
    when(session.getLastReadTime()).thenReturn(31000L);
    when(session.getLastWriteTime()).thenReturn(31000L);
    task.scan(31000L);
    verify(session).close(ConnectionDisconnectMode.IDLE, PlayerDisconnectMode.CONNECTION_LOST);
    verify(session, never()).getLastReadTime();
    verify(session, never()).getLastWriteTime();
  }

  @Test
  void skipsDetachedPlayer() {
    when(player.getSession()).thenReturn(Optional.empty());
    task.scan(31000L);
    verifyNoInteractions(session);
  }

  @Test
  void skipsInactiveSession() throws IOException {
    when(session.isActivated()).thenReturn(false);
    task.scan(31000L);
    verify(session, never()).close(any(), any());
  }

  @Test
  void skipsNonTcpPrimarySession() throws IOException {
    when(session.isTcp()).thenReturn(false);
    task.scan(31000L);
    verify(session, never()).close(any(), any());
  }

  @Test
  void skipsReplacedSession() throws IOException {
    Session replacement = mock(Session.class);
    when(player.getSession()).thenReturn(Optional.of(session), Optional.of(replacement));
    task.scan(31000L);
    verify(session, never()).close(any(), any());
    verifyNoInteractions(replacement);
  }

  @Test
  void skipsFutureReadTimestampAfterClockAdjustment() throws IOException {
    when(session.getLastReliableReadTime()).thenReturn(40000L);
    task.scan(31000L);
    verify(session, never()).close(any(), any());
  }

  @Test
  void continuesAfterOneCloseFails() throws IOException {
    Player other = mock(Player.class);
    Session otherSession = mock(Session.class);
    when(other.getSession()).thenReturn(Optional.of(otherSession));
    when(otherSession.isTcp()).thenReturn(true);
    when(otherSession.isActivated()).thenReturn(true);
    when(players.getSnapshotPlayersList()).thenReturn(List.of(player, other));
    doThrow(new IOException("failed close")).when(session).close(any(), any());
    task.scan(31000L);
    verify(otherSession).close(ConnectionDisconnectMode.IDLE, PlayerDisconnectMode.CONNECTION_LOST);
  }

  @Test
  void zeroTimeoutSkipsExpiryButKeepsConfiguredSchedule() {
    task.setTimeoutSeconds(0);
    task.scan(Long.MAX_VALUE);
    task.run();
    assertNotNull(task.getScheduler());
    verifyNoInteractions(players);
    task.shutdown();
    assertTrue(task.getScheduler().isCancelled());
  }

  @Test
  void omissionDefaultsToDisabled() {
    AutoScanAliveMainConnectionTask unconfigured = new AutoScanAliveMainConnectionTask();
    unconfigured.run();
    assertNull(unconfigured.getScheduler());
    unconfigured.shutdown();
  }

  @Test
  void rejectsNegativeTimeout() {
    assertThrows(IllegalArgumentException.class, () -> task.setTimeoutSeconds(-1));
  }

  @Test
  void startsOnlyOnceAndShutdownCancelsWorker() {
    task.run();
    var scheduled = task.getScheduler();
    assertNotNull(scheduled);
    task.run();
    assertSame(scheduled, task.getScheduler());
    task.shutdown();
    assertTrue(scheduled.isCancelled());
    task.run();
    assertSame(scheduled, task.getScheduler());
  }

  @Test
  void shutdownBeforeRunDoesNotStartWorker() {
    task.shutdown();
    task.run();
    assertNull(task.getScheduler());
  }

  @Test
  void supportsLargeTimeoutWithoutIntegerOverflow() throws IOException {
    task.setTimeoutSeconds(Integer.MAX_VALUE);
    task.scan(31000L);
    verify(session, never()).close(any(), any());
  }
  @Test
  void disabledScanIntervalsDoNotSchedule() {
    for (int interval : new int[] {-1, 0, -2}) {
      task.setInterval(interval);
      task.scan(Long.MAX_VALUE);
      task.run();
      assertNull(task.getScheduler());
    }
    verifyNoInteractions(players);
  }

  @Test
  void omittedIntervalDisablesTaskEvenWhenTimeoutIsPositive() {
    AutoScanAliveMainConnectionTask unconfigured = new AutoScanAliveMainConnectionTask();
    unconfigured.setPlayerManager(players);
    unconfigured.setTimeoutSeconds(30);
    unconfigured.run();
    assertNull(unconfigured.getScheduler());
    unconfigured.shutdown();
    verifyNoInteractions(players);
  }

  @Test
  void configuredIntervalControlsInitialDelayAndSubsequentScans() {
    var executor = mock(ScheduledExecutorService.class);
    var future = mock(ScheduledFuture.class);
    when(executor.scheduleWithFixedDelay(any(Runnable.class), eq(7L), eq(7L),
        eq(TimeUnit.SECONDS))).thenReturn(future);
    try (var executors = mockStatic(Executors.class)) {
      executors.when(() -> Executors.newSingleThreadScheduledExecutor(
          any(ThreadFactory.class))).thenReturn(executor);
      task.setInterval(7);
      task.run();
      verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(7L), eq(7L),
          eq(TimeUnit.SECONDS));
      assertSame(future, task.getScheduler());
      task.shutdown();
      verify(future).cancel(false);
      verify(executor).shutdownNow();
    }
  }

}
