/*
The MIT License

Copyright (c) 2016-2021 kong <congcoi123@gmail.com>

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

package com.tenio.core.entity;

import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.implement.RoomImpl;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.setting.strategy.RoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.PlayerJoinedRoomException;
import com.tenio.core.exception.RemovedNonExistentPlayerFromRoomException;
import com.tenio.core.exception.SwitchedPlayerSpectatorException;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;
import java.util.List;

/**
 * The abstract room object used on the server.
 */
public interface Room {

/**
* Retrieves the room's Id.
*
* @return the room's Id in <code>long</code> value
*/
  long getId();

/**
* Retrieves the room's name.
*
* @return the {@link String} value for room's name
*/
  String getName();

/**
* Sets the room's name.
*
* @param name a {@link String} value for the room's name
* @throws IllegalArgumentException when an invalid value is used
*/
  void setName(String name) throws IllegalArgumentException;

/**
* Retrieves the room's password.
*
* @return the {@link String} value for room's password
*/
  String getPassword();

/**
* Sets the room's password.
*
* @param password a {@link String} value for the room's password
* @throws IllegalArgumentException when an invalid value is used
*/
  void setPassword(String password) throws IllegalArgumentException;

/**
* Retrieves the current room's state.
*
* @return the current {@link RoomState}
*/
  RoomState getState();

/**
* Sets new state for the room.
*
* @param state the new {@link RoomState} value
*/
  void setState(RoomState state);

/**
* Determines whether the room is public which means it does not contain credential and allow to be free to get in.
*
* @return <code>true</code> if the room is public, otherwise <code>false</code>
*/
  boolean isPublic();

/**
* Retrieves the maximum number of players allows in the room.
*
* @return the maximum number of players in <code>integer</code> value
*/
  int getMaxPlayers();

/**
* Sets the maximum number of players allows in the room.
*
* @param maxPlayers the maximum number of players in <code>integer</code> value
* @throws IllegalArgumentException when an invalid value is used
*/
  void setMaxPlayers(int maxPlayers) throws IllegalArgumentException;

/**
* Retrieves the maximum number of spectators allows in the room.
*
* @return the maximum number of spectators in <code>integer</code> value
*/
  int getMaxSpectators();

/**
* Sets the maximum number of spectators allows in the room.
*
* @param maxSpectators the maximum number of spectators in <code>integer</code> value
* @throws IllegalArgumentException when an invalid value is used
*/
  void setMaxSpectators(int maxSpectators) throws IllegalArgumentException;

/**
*
*
*
*/
// Use Optional
  Player getOwner();

/**
* Updates the room's owner.
*
* @param owner a {@link Player} is set to be the room's owner
*/
  void setOwner(Player owner);

/**
* Retrieves a player manager for the room.
*
* @return a {@link PlayerManager} instance
*/
  PlayerManager getPlayerManager();

/**
* Sets a new player manager for the room.
*
* @param playerManager the {@link PlayerManager} object
*/
  void setPlayerManager(PlayerManager playerManager);

/**
* Determines whether the room is activated.
*
* @return <code>true</code> when the room is active, otherwise <code>false</code>
*/
  boolean isActivated();

/**
* Updates the active state for the room.
*
* @param activated sets the value to <code>true</code> to activate the room, otherwise <code>false</code>
*/
  void setActivated(boolean activated);

/**
* Retrieves the rule applied to room to remove it.
*
* @return the current {@link RoomRemoveMode} of room
*/
  RoomRemoveMode getRoomRemoveMode();

/**
* Sets the rule applied to room to remove it.
*
* @param roomRemoveMode the new {@link RoomRemoveMode} of room
*/
  void setRoomRemoveMode(RoomRemoveMode roomRemoveMode);

  // Use Optional
  Object getProperty(String key);

/**
* Sets a property belongs to the player.
*
* @param key the {@link String} key
* @param value an instance {@link Object} of property's value
*/
  void setProperty(String key, Object value);
  
  /**
  * Determines whether a property is available for the player.
  *
  * @param key a {@link String} key used for searching the corresponding property
  * @return <code>true</code> if the property is available, otherwise <code>false</code>
  */
  boolean containsProperty(String key);

/**
* Removes a property which belongs to the player.
*
* @param key the {@link String} key
*/
  void removeProperty(String key);

/**
* Removes all properties of the player.
*/
  void clearProperties();

/**
* Retrieves the total number of entities allowed to be in the room (players and spectators)
*
* @return the <code>integer</code> value, total number of entities
*/
  int getCapacity();

/**
* Sets the limitation values for players and spectators.
*
* @param maxPlayers the maximum number of players in <code>integer</code> value
* @param maxSpectators the maximum number of spectators in <code>integer</code> value
* @throws IllegalArgumentException when an invalid value is used
*/
  void setCapacity(int maxPlayers, int maxSpectators) throws IllegalArgumentException;

// iterator, unmodify list
  List<Player> getPlayersList();

// iterator, unmodify list
  List<Player> getSpectatorsList();

/**
* Retrieves the current number of players in the room.
*
* @return the <code>integer</code> value, the current number of players
*/
  int getPlayerCount();

/**
* Retrieves the current number of spectators in the room.
*
* @return the <code>integer</code> value, the current number of spectators
*/
  int getSpectatorCount();

/**
* Determines whether a player is present in the room.
*
* @param playerName a unique {@link String} player name in the room's player management list
* @return <code>true</code> when a player can be found, otherwise <code>false</code>
*/
  boolean containsPlayerName(String playerName);

// Use Optional
  Player getPlayerByName(String playerName);

// Use Optional
  Player getPlayerBySession(Session session);

// iterator, unmodify list
  Collection<Player> getAllPlayersList();

// iterator, unmodify list
  Collection<Session> getAllSessionList();

/**
* Adds a player to the room.
*
* @param player a joining {@link Player}
* @param asSpectator the player join room as a spectator
* @param targetSlot when the player join the room as role of 'player', then it can occupy a <code>integer</code> slot position
* @throws PlayerJoinedRoomException any exception occured when the player try to join the room
* @throws AddedDuplicatedPlayerException when the player has been already in the room, but it is forced to join again
*/
  void addPlayer(Player player, boolean asSpectator, int targetSlot)
      throws PlayerJoinedRoomException, AddedDuplicatedPlayerException;

/**
* Adds a player to the room without indicating any slot position.
*
* @param player a joining {@link Player}
* @param asSpectator the player join room as a spectator
* @throws PlayerJoinedRoomException any exception occured when the player try to join the room
* @throws AddedDuplicatedPlayerException when the player has been already in the room, but it is forced to join again
*/
  default void addPlayer(Player player, boolean asSpectator)
      throws PlayerJoinedRoomException, AddedDuplicatedPlayerException {
    addPlayer(player, asSpectator, RoomImpl.DEFAULT_SLOT);
  }

/**
* Adds a player to the room without indicating any slot position and it join as role of a 'player'.
*
* @param player a joining {@link Player}
* @throws PlayerJoinedRoomException any exception occured when the player try to join the room
* @throws AddedDuplicatedPlayerException when the player has been already in the room, but it is forced to join again
*/
  default void addPlayer(Player player)
      throws PlayerJoinedRoomException, AddedDuplicatedPlayerException {
    addPlayer(player, false);
  }

/**
* Removes a player from its current room.
*
* @param player the leaving {@link player}
* @throws RemovedNonExistentPlayerFromRoomException when the player has already left the room but it is mentioned again
*/
  void removePlayer(Player player) throws RemovedNonExistentPlayerFromRoomException;

/**
* Changes a player's role from 'player' to 'spectator' in the room.
*
* @param player the changing {@link Player}
* @throws SwitchedPlayerSpectatorException when the changing cannot be done
*/
  void switchPlayerToSpectator(Player player) throws SwitchedPlayerSpectatorException;

/**
* Changes a player's role from 'spectator' to 'player' in the room.
*
* @param player the changing {@link Player}
* @param targetSlot a new <code>integer</code> value of slot position set to the player in the room
* @throws SwitchedPlayerSpectatorException when the changing cannot be done
*/
  void switchSpectatorToPlayer(Player player, int targetSlot)
      throws SwitchedPlayerSpectatorException;

/**
* Changes a player's role from 'spectator' to 'player' without consideration of its slot position in the room.
*
* @param player the changing {@link Player}
* @throws SwitchedPlayerSpectatorException when the changing cannot be done
* @see RoomImpl#DEFAULT_SLOT
*/
  default void switchSpectatorToPlayer(Player player) throws SwitchedPlayerSpectatorException {
    switchSpectatorToPlayer(player, RoomImpl.DEFAULT_SLOT);
  }

/**
* Determines whether the room is empty.
*
* @return <code>true</code> if there is no entity in the room, otherwise <code>false</code>
*/
  boolean isEmpty();

/**
* Determines whether the room is full.
*
* @return <code>true</code> if the number of entities reached the limitation in the room, otherwise <code>false</code>
*/
  boolean isFull();

// Use Optional
  RoomPlayerSlotGeneratedStrategy getPlayerSlotGeneratedStrategy();

/**
* Sets a strategy to generate slot positions in the room.
*
* @param roomPlayerSlotGeneratedStrategy an implemented of {@link RoomPlayerSlotGeneratedStrategy}
*/
  void setPlayerSlotGeneratedStrategy(
      RoomPlayerSlotGeneratedStrategy roomPlayerSlotGeneratedStrategy);

// Use Optional
  RoomCredentialValidatedStrategy getRoomCredentialValidatedStrategy();

/**
* Sets a strategy to validate credentials used to allow players get in the room.
*
* @param roomCredentialValidatedStrategy an implemented of {@link RoomCredentialValidatedStrategy}
*/
  void setRoomCredentialValidatedStrategy(
      RoomCredentialValidatedStrategy roomCredentialValidatedStrategy);

/**
* Clears all information about the room.
*
* @throws UnsupportedOperationException the operation is not supported at the moment
*/
  default void clear() {
    throw new UnsupportedOperationException();
  }
}
