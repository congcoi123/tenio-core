/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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
import com.tenio.core.entity.define.room.PlayerRoleInRoom;
import com.tenio.core.entity.implement.RoomImpl;
import com.tenio.core.entity.manager.PlayerManager;
import com.tenio.core.entity.setting.strategy.RoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomPlayerSlotGeneratedStrategy;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.PlayerJoinedRoomException;
import com.tenio.core.exception.RemovedNonExistentPlayerFromRoomException;
import com.tenio.core.exception.SwitchedPlayerRoleInRoomException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The abstract room entity used on the server.
 */
public interface Room {

  /**
   * Retrieves the room's unique ID in the management list, on the server.
   *
   * @return the room's unique ID ({@code long} value)
   */
  long getId();

  /**
   * Retrieves the room's name.
   *
   * @return a {@link String} value for room's name
   */
  String getName();

  /**
   * Sets the room's name.
   *
   * @param name a {@link String} value for the room's name
   * @throws IllegalArgumentException when an invalid value is used
   * @see RoomCredentialValidatedStrategy
   */
  void setName(String name) throws IllegalArgumentException;

  /**
   * Retrieves the room's password.
   *
   * @return a {@link String} value for room's password
   * @see RoomCredentialValidatedStrategy
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
   * Sets a new state for the room.
   *
   * @param state the new {@link RoomState} value
   */
  void setState(RoomState state);

  /**
   * Determines whether the room is public which means it does not contain any credential and
   * allowed to freely get in.
   *
   * @return {@code true} if the room is public, otherwise returns {@code false}
   */
  boolean isPublic();

  /**
   * Retrieves the maximum number of participants allowing in the room.
   *
   * @return the maximum number of participants ({@code integer} value)
   */
  int getMaxParticipants();

  /**
   * Sets the maximum number of participants allowing in the room.
   *
   * @param maxParticipants the maximum number of participants ({@code integer} value)
   * @throws IllegalArgumentException when an invalid value is used
   */
  void setMaxParticipants(int maxParticipants) throws IllegalArgumentException;

  /**
   * Retrieves the maximum number of spectators allowing in the room.
   *
   * @return the maximum number of spectators ({@code integer} value)
   */
  int getMaxSpectators();

  /**
   * Sets the maximum number of spectators allowing in the room.
   *
   * @param maxSpectators the maximum number of spectators ({@code integer} value)
   * @throws IllegalArgumentException when an invalid value is used
   */
  void setMaxSpectators(int maxSpectators) throws IllegalArgumentException;

  /**
   * Retrieves the room's owner.
   *
   * @return the optional {@link Player} as the room's owner
   * @see Optional
   */
  Optional<Player> getOwner();

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
   * @param playerManager a {@link PlayerManager} instance
   */
  void setPlayerManager(PlayerManager playerManager);

  /**
   * Determines whether the room is activated.
   *
   * @return {@code true} when the room is active, otherwise returns {@code false}
   */
  boolean isActivated();

  /**
   * Updates the active state for the room.
   *
   * @param activated sets the value to {@code true} to activate the room, otherwise
   *                  {@code false}
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

  /**
   * Retrieves a property belongs to the room.
   *
   * @param key the {@link String} key to fetch data
   * @return an {@link Object} value if present, otherwise {@code null}
   */
  Object getProperty(String key);

  /**
   * Sets a property belongs to the room.
   *
   * @param key   the {@link String} key
   * @param value an instance {@link Object} of property's value
   */
  void setProperty(String key, Object value);

  /**
   * Determines whether a property is available for the room.
   *
   * @param key a {@link String} key used for searching the corresponding property
   * @return {@code true} if the property is available, otherwise returns {@code false}
   */
  boolean containsProperty(String key);

  /**
   * Removes a property which belongs to the room.
   *
   * @param key the {@link String} key
   */
  void removeProperty(String key);

  /**
   * Removes all properties of the room.
   */
  void clearProperties();

  /**
   * Retrieves the total number of entities allowed to be in the room (participants and spectators).
   *
   * @return the total number of entities ({@code integer} value)
   */
  int getCapacity();

  /**
   * Sets the limitation values for participants and spectators.
   *
   * @param maxParticipants the maximum number of participants ({@code integer} value)
   * @param maxSpectators   the maximum number of spectators ({@code integer} value)
   * @throws IllegalArgumentException when an invalid value is used
   */
  void setCapacity(int maxParticipants, int maxSpectators) throws IllegalArgumentException;

  /**
   * Retrieves the current number of players in the room.
   *
   * @return the current number of players ({@code integer} value)
   */
  int getParticipantCount();

  /**
   * Retrieves the current number of spectators in the room.
   *
   * @return the current number of spectators ({@code integer} value)
   */
  int getSpectatorCount();

  /**
   * Determines whether a player is present in the room.
   *
   * @param playerName a unique {@link String} player name in the room's player management list
   * @return {@code true} when a player can be found, otherwise returns {@code false}
   */
  boolean containsPlayerName(String playerName);

  /**
   * Retrieves a player by using its name.
   *
   * @param playerName a unique {@link String} name of player on the server
   * @return an instance of optional {@link Player}
   * @see Optional
   */
  Optional<Player> getPlayerByName(String playerName);

  /**
   * Retrieves an iterator for a player management list. This method should be used to prevent
   * the "escape references" issue.
   *
   * @return an iterator of {@link Player} management list
   * @see Iterator
   */
  Iterator<Player> getPlayerIterator();

  /**
   * Retrieves a read-only player management list. This method should be used to prevent the
   * "escape references" issue.
   *
   * @return a list of all {@link Player}s in the management list
   * @see List
   */
  List<Player> getReadonlyPlayersList();

  /**
   * Retrieves a read-only list of participants in a player management list. This method should be
   * used to prevent the "escape references" issue.
   *
   * @return a list of participants in a player management list
   * @see List
   * @see PlayerRoleInRoom#PARTICIPANT
   */
  List<Player> getReadonlyParticipantsList();

  /**
   * Retrieves a read-only list of spectators in a player management list. This method should be
   * used to prevent the "escape references" issue.
   *
   * @return a list of spectators in a player management list
   * @see List
   * @see PlayerRoleInRoom#SPECTATOR
   */
  List<Player> getReadonlySpectatorsList();

  /**
   * Adds a player to the room.
   *
   * @param player      a joining {@link Player}
   * @param asSpectator sets to {@code true} if the player joins room as a "spectator",
   *                    otherwise returns {@code false}
   * @param targetSlot  when the player join the room as role of "participants", then it can
   *                    occupy a slot position ({@code integer} value)
   * @throws PlayerJoinedRoomException      any exception occurred when the player tries to join
   *                                        the room
   * @throws AddedDuplicatedPlayerException when the player has been already in the room, but it
   *                                        is forced to join again
   */
  void addPlayer(Player player, boolean asSpectator, int targetSlot)
      throws PlayerJoinedRoomException, AddedDuplicatedPlayerException;

  /**
   * Adds a player to the room without indicating any slot position.
   *
   * @param player      a joining {@link Player}
   * @param asSpectator sets to {@code true} if the player joins room as a "spectator",
   *                    otherwise returns {@code false}
   * @throws PlayerJoinedRoomException      any exception occurred when the player tries to join
   *                                        the room
   * @throws AddedDuplicatedPlayerException when the player has been already in the room, but it
   *                                        is forced to join again
   */
  default void addPlayer(Player player, boolean asSpectator)
      throws PlayerJoinedRoomException, AddedDuplicatedPlayerException {
    addPlayer(player, asSpectator, RoomImpl.DEFAULT_SLOT);
  }

  /**
   * Adds a player to the room without indicating any slot position, and it joins as role of a
   * "participant".
   *
   * @param player a joining {@link Player}
   * @throws PlayerJoinedRoomException      any exception occurred when the player tries to join
   *                                        the room
   * @throws AddedDuplicatedPlayerException when the player has been already in the room, but it
   *                                        is forced to join again
   */
  default void addPlayer(Player player)
      throws PlayerJoinedRoomException, AddedDuplicatedPlayerException {
    addPlayer(player, false);
  }

  /**
   * Removes a player from its current room.
   *
   * @param player the leaving {@link Player}
   * @throws RemovedNonExistentPlayerFromRoomException when the player has already left the room,
   *                                                   but it is mentioned again
   */
  void removePlayer(Player player) throws RemovedNonExistentPlayerFromRoomException;

  /**
   * Changes a player's role from "participant" to "spectator" in the room.
   *
   * @param player the changing {@link Player}
   * @throws SwitchedPlayerRoleInRoomException when the changing could not be done
   */
  void switchParticipantToSpectator(Player player) throws SwitchedPlayerRoleInRoomException;

  /**
   * Changes a player's role from "spectator" to "participant" in the room.
   *
   * @param player     the changing {@link Player}
   * @param targetSlot a new slot position ({@code integer} value) set to the player in the
   *                   room
   * @throws SwitchedPlayerRoleInRoomException when the changing could not be done
   */
  void switchSpectatorToParticipant(Player player, int targetSlot)
      throws SwitchedPlayerRoleInRoomException;

  /**
   * Changes a player's role from "spectator" to "participant" without consideration of its slot
   * position in the room.
   *
   * @param player the changing {@link Player}
   * @throws SwitchedPlayerRoleInRoomException when the changing could not be done
   * @see RoomImpl#DEFAULT_SLOT
   */
  default void switchSpectatorToParticipant(Player player) throws SwitchedPlayerRoleInRoomException {
    switchSpectatorToParticipant(player, RoomImpl.DEFAULT_SLOT);
  }

  /**
   * Determines whether the room is empty.
   *
   * @return {@code true} if there is no entity in the room, otherwise returns {@code false}
   */
  boolean isEmpty();

  /**
   * Determines whether the room is full.
   *
   * @return {@code true} if the number of entities reached the limitation in the room,
   * otherwise returns {@code false}
   */
  boolean isFull();

  /**
   * Retrieves a strategy to generate slot positions in the room.
   *
   * @return an implementation of {@link RoomPlayerSlotGeneratedStrategy}, in case this value is
   * {@code null}, then the default implementation will be used
   * @see DefaultRoomPlayerSlotGeneratedStrategy
   */
  RoomPlayerSlotGeneratedStrategy getPlayerSlotGeneratedStrategy();

  /**
   * Sets a strategy to generate slot positions in the room.
   *
   * @param roomPlayerSlotGeneratedStrategy an implementation of
   *                                        {@link RoomPlayerSlotGeneratedStrategy}
   */
  void setPlayerSlotGeneratedStrategy(
      RoomPlayerSlotGeneratedStrategy roomPlayerSlotGeneratedStrategy);

  /**
   * Retrieves a strategy to validate credentials used to allow players get in the room.
   *
   * @return an implementation of {@link RoomCredentialValidatedStrategy}, in case this value is
   * {@code null}, then the default implementation will be used
   * @see DefaultRoomCredentialValidatedStrategy
   */
  RoomCredentialValidatedStrategy getRoomCredentialValidatedStrategy();

  /**
   * Sets a strategy to validate credentials used to allow players get in the room.
   *
   * @param roomCredentialValidatedStrategy an implementation of
   *                                        {@link RoomCredentialValidatedStrategy}
   */
  void setRoomCredentialValidatedStrategy(
      RoomCredentialValidatedStrategy roomCredentialValidatedStrategy);

  /**
   * Clears all information related to the room.
   *
   * @throws UnsupportedOperationException the operation is not supported at the moment
   */
  default void clear() {
    throw new UnsupportedOperationException();
  }
}
