/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

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

package com.tenio.core.entity.implement;

import com.tenio.common.utility.TimeUtility;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.PlayerState;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.network.entity.session.Session;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * An implemented class is for a player using on the server.
 */
public class PlayerImpl implements Player {

  private final String identity;
  private final Map<String, Object> properties;
  private Consumer<Field> updateConsumer;
  private final AtomicReference<Session> session;
  private final AtomicReference<Room> currentRoom;
  private final AtomicReference<PlayerState> state;
  private final AtomicReference<PlayerRoleInRoom> roleInRoom;

  private final AtomicLong lastLoginTime;
  private final AtomicLong lastJoinedRoomTime;
  private final AtomicLong lastReadTime;
  private final AtomicLong lastWriteTime;
  private final AtomicLong lastActivityTime;

  private final AtomicInteger playerSlotInCurrentRoom;

  private final AtomicBoolean loggedIn;
  private final AtomicBoolean activated;
  private final AtomicBoolean deportedFlag;

  // These variables should be one-time setup and would not be thread-safe!
  private int maxIdleTimeInSecond;
  private int maxIdleTimeNeverDeportedInSecond;

  /**
   * Constructor.
   *
   * @param identity the player unique name
   */
  public PlayerImpl(String identity) {
    this(identity, null);
  }

  /**
   * Constructor.
   *
   * @param identity    the player unique name
   * @param session a session which associates to the player
   */
  public PlayerImpl(String identity, Session session) {
    this.identity = identity;
    this.session = new AtomicReference<>(session);
    properties = new ConcurrentHashMap<>();
    currentRoom = new AtomicReference<>(null);
    state = new AtomicReference<>(null);
    roleInRoom = new AtomicReference<>(PlayerRoleInRoom.SPECTATOR);
    lastLoginTime = new AtomicLong(0L);
    lastJoinedRoomTime = new AtomicLong(0L);
    lastReadTime = new AtomicLong(now());
    lastWriteTime = new AtomicLong(now());
    lastActivityTime = new AtomicLong(now());
    playerSlotInCurrentRoom = new AtomicInteger(Room.NIL_SLOT);
    loggedIn = new AtomicBoolean(false);
    activated = new AtomicBoolean(false);
    deportedFlag = new AtomicBoolean(false);
    maxIdleTimeInSecond = 0;
    maxIdleTimeNeverDeportedInSecond = 0;
  }

  /**
   * Create a new instance without session.
   *
   * @param name a unique name for player on the server
   * @return a new instance
   */
  public static Player newInstance(String name) {
    return new PlayerImpl(name);
  }

  /**
   * Create a new instance.
   *
   * @param name    a unique name for player on the server
   * @param session a session associated to the player
   * @return a new instance
   */
  public static Player newInstance(String name, Session session) {
    return new PlayerImpl(name, session);
  }

  @Override
  public String getIdentity() {
    return identity;
  }

  @Override
  public boolean containsSession() {
    return Objects.nonNull(session.get());
  }

  @Override
  public boolean isState(PlayerState state) {
    return this.state.get() == state;
  }

  @Override
  public PlayerState getState() {
    return state.get();
  }

  @Override
  public void setState(PlayerState state) {
    this.state.set(state);
    notifyUpdate(Field.STATE);
  }

  @Override
  public boolean isActivated() {
    return activated.get();
  }

  @Override
  public void setActivated(boolean activated) {
    this.activated.set(activated);
    notifyUpdate(Field.ACTIVATION);
  }

  @Override
  public boolean isLoggedIn() {
    return loggedIn.get();
  }

  @Override
  public void setLoggedIn(boolean loggedIn) {
    this.loggedIn.set(loggedIn);
    if (loggedIn) {
      setLastLoggedInTime();
    }
  }

  @Override
  public long getLastLoggedInTime() {
    return lastLoginTime.get();
  }

  @Override
  public long getLastActivityTime() {
    return lastActivityTime.get();
  }

  @Override
  public void setLastActivityTime(long timestamp) {
    lastActivityTime.set(timestamp);
  }

  @Override
  public long getLastReadTime() {
    return lastReadTime.get();
  }

  @Override
  public void setLastReadTime(long timestamp) {
    lastReadTime.set(timestamp);
    setLastActivityTime(timestamp);
  }

  @Override
  public long getLastWriteTime() {
    return lastWriteTime.get();
  }

  @Override
  public void setLastWriteTime(long timestamp) {
    lastWriteTime.set(timestamp);
    setLastActivityTime(timestamp);
  }

  @Override
  public int getMaxIdleTimeInSeconds() {
    return maxIdleTimeInSecond;
  }

  @Override
  public void setMaxIdleTimeInSeconds(int seconds) {
    maxIdleTimeInSecond = seconds;
  }

  @Override
  public boolean isIdle() {
    return isConnectionIdle(getMaxIdleTimeInSeconds());
  }

  @Override
  public boolean isNeverDeported() {
    return deportedFlag.get();
  }

  @Override
  public void setNeverDeported(boolean flag) {
    deportedFlag.set(flag);
    notifyUpdate(Field.DEPORTATION);
  }

  @Override
  public int getMaxIdleTimeNeverDeportedInSeconds() {
    return maxIdleTimeNeverDeportedInSecond;
  }

  @Override
  public void setMaxIdleTimeNeverDeportedInSeconds(int seconds) {
    maxIdleTimeNeverDeportedInSecond = seconds;
  }

  @Override
  public boolean isIdleNeverDeported() {
    return isNeverDeported() && isConnectionIdle(getMaxIdleTimeNeverDeportedInSeconds());
  }

  private boolean isConnectionIdle(int maxIdleTimeInSecond) {
    return maxIdleTimeInSecond > 0 &&
        ((now() - getLastActivityTime()) / 1000L) > (long) maxIdleTimeInSecond;
  }

  private void setLastLoggedInTime() {
    lastLoginTime.set(now());
  }

  @Override
  public Optional<Session> getSession() {
    return Optional.ofNullable(session.get());
  }

  @Override
  public void setSession(Session session) {
    if (Objects.nonNull(session)) {
      session.setName(identity);
      session.setAssociatedToPlayer(Session.AssociatedState.DONE);
    }
    this.session.set(session);
  }

  @Override
  public boolean isInRoom() {
    return Objects.nonNull(currentRoom.get());
  }

  @Override
  public PlayerRoleInRoom getRoleInRoom() {
    return roleInRoom.get();
  }

  @Override
  public void setRoleInRoom(PlayerRoleInRoom roleInRoom) {
    this.roleInRoom.set(roleInRoom);
    notifyUpdate(Field.ROLE_IN_ROOM);
  }

  @Override
  public Optional<Room> getCurrentRoom() {
    return Optional.ofNullable(currentRoom.get());
  }

  @Override
  public void setCurrentRoom(Room room) {
    currentRoom.set(room);
    playerSlotInCurrentRoom.set(Objects.isNull(room) ? Room.NIL_SLOT : Room.DEFAULT_SLOT);
    setLastJoinedRoomTime();
  }

  @Override
  public long getLastJoinedRoomTime() {
    return lastJoinedRoomTime.get();
  }

  private void setLastJoinedRoomTime() {
    lastJoinedRoomTime.set(now());
  }

  @Override
  public int getPlayerSlotInCurrentRoom() {
    return playerSlotInCurrentRoom.get();
  }

  @Override
  public void setPlayerSlotInCurrentRoom(int slot) {
    playerSlotInCurrentRoom.set(slot);
    notifyUpdate(Field.SLOT_IN_ROOM);
  }

  @Override
  public Object getProperty(String key) {
    return properties.get(key);
  }

  @Override
  public void setProperty(String key, Object value) {
    properties.put(key, value);
    notifyUpdate(Field.PROPERTY);
  }

  @Override
  public boolean containsProperty(String key) {
    return properties.containsKey(key);
  }

  @Override
  public void removeProperty(String key) {
    properties.remove(key);
    notifyUpdate(Field.PROPERTY);
  }

  @Override
  public void clearProperties() {
    properties.clear();
    notifyUpdate(Field.PROPERTY);
  }

  @Override
  public void onUpdateListener(Consumer<Field> updateConsumer) {
    this.updateConsumer = updateConsumer;
  }

  @Override
  public void clean() {
    setActivated(false);
    setCurrentRoom(null);
    setSession(null);
    clearProperties();
  }

  private long now() {
    return TimeUtility.currentTimeMillis();
  }

  // This is not thread-safe
  private void notifyUpdate(Field field) {
    if (Objects.nonNull(updateConsumer)) {
      updateConsumer.accept(field);
    }
  }

  @Override
  public boolean equals(Object object) {
    return (object instanceof Player player) && identity.equals(player.getIdentity());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * (Objects.isNull(identity) ? 0 : identity.hashCode());
  }

  @Override
  public String toString() {
    return "Player{" +
        "identity='" + identity + '\'' +
        ", properties=" + properties +
        ", currentRoom=" + (Objects.nonNull(currentRoom.get()) ? currentRoom.get().getId() : "") +
        ", state=" + state.get() +
        ", roleInRoom=" + roleInRoom.get() +
        ", lastLoginTime=" + lastLoginTime.get() +
        ", lastJoinedRoomTime=" + lastJoinedRoomTime.get() +
        ", lastReadTime=" + lastReadTime.get() +
        ", lastWriteTime=" + lastWriteTime.get() +
        ", lastActivityTime=" + lastActivityTime.get() +
        ", maxIdleTimeInSecond=" + maxIdleTimeInSecond +
        ", maxIdleTimeNeverDeportedInSecond=" + maxIdleTimeNeverDeportedInSecond +
        ", playerSlotInCurrentRoom=" + playerSlotInCurrentRoom.get() +
        ", loggedIn=" + loggedIn.get() +
        ", activated=" + activated.get() +
        ", deportedFlag=" + deportedFlag.get() +
        ", session=" + session.get() +
        '}';
  }
}
