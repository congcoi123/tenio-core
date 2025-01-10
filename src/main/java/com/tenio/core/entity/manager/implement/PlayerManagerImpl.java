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

package com.tenio.core.entity.manager.implement;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.implement.PlayerImpl;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.RemovedNonExistentPlayerFromRoomException;
import com.tenio.core.manager.AbstractManager;
import com.tenio.core.network.entity.session.Session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implemented class is for player management.
 */
public final class PlayerManagerImpl extends AbstractManager implements PlayerManager {

  private final Map<String, Player> players;
  private final AtomicReference<Room> ownerRoom;
  // Non thread-safe, one-time setup
  private int maxIdleTimeInSecond;
  private int maxIdleTimeNeverDeportedInSecond;

  private PlayerManagerImpl(EventManager eventManager) {
    super(eventManager);
    players = new ConcurrentHashMap<>();
    ownerRoom = new AtomicReference<>(null);
  }

  /**
   * Creates a new instance of the player manager.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link PlayerManager}
   */
  public static PlayerManager newInstance(EventManager eventManager) {
    return new PlayerManagerImpl(eventManager);
  }

  @Override
  public void addPlayer(Player player) {
    if (containsPlayerIdentity(player.getIdentity())) {
      throw new AddedDuplicatedPlayerException(player, ownerRoom.get());
    }

    players.put(player.getIdentity(), player);
  }

  @Override
  public Player createPlayer(String playerName) {
    var player = PlayerImpl.newInstance(playerName);
    player.setActivated(true);
    player.setLoggedIn(true);
    player.setMaxIdleTimeInSeconds(maxIdleTimeInSecond);
    player.setMaxIdleTimeNeverDeportedInSeconds(maxIdleTimeNeverDeportedInSecond);
    addPlayer(player);

    return player;
  }

  @Override
  public Player createPlayerWithSession(String playerName, Session session) {
    if (Objects.isNull(session)) {
      throw new NullPointerException("Unable to assign a null session for the player");
    }

    var player = PlayerImpl.newInstance(playerName, session);
    player.setActivated(true);
    player.setLoggedIn(true);
    player.setMaxIdleTimeInSeconds(maxIdleTimeInSecond);
    player.setMaxIdleTimeNeverDeportedInSeconds(maxIdleTimeNeverDeportedInSecond);
    addPlayer(player);

    return player;
  }

  @Override
  public Player getPlayerByIdentity(String playerIdentity) {
    return players.get(playerIdentity);
  }

  @Override
  public Iterator<Player> getPlayerIterator() {
    return players.values().iterator();
  }

  @Override
  public List<Player> getReadonlyPlayersList() {
    return players.values().stream().toList();
  }

  @Override
  public void removePlayerByIdentity(String playerIdentity) {
    if (!containsPlayerIdentity(playerIdentity)) {
      throw new RemovedNonExistentPlayerFromRoomException(playerIdentity, ownerRoom.get());
    }

    removePlayer(playerIdentity);
  }

  private void removePlayer(String playerName) {
    players.remove(playerName);
  }

  @Override
  public boolean containsPlayerIdentity(String playerIdentity) {
    return players.containsKey(playerIdentity);
  }

  @Override
  public Room getOwnerRoom() {
    return ownerRoom.get();
  }

  @Override
  public void setOwnerRoom(Room room) {
    ownerRoom.set(room);
  }

  @Override
  public int getPlayerCount() {
    return players.size();
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
  public int getMaxIdleTimeNeverDeportedInSeconds() {
    return maxIdleTimeNeverDeportedInSecond;
  }

  @Override
  public void setMaxIdleTimeNeverDeportedInSeconds(int seconds) {
    maxIdleTimeNeverDeportedInSecond = seconds;
  }

  @Override
  public void clear() {
    players.clear();
  }
}
