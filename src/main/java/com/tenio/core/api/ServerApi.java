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

package com.tenio.core.api;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.entity.data.ServerMessage;
import com.tenio.core.entity.define.mode.PlayerBanMode;
import com.tenio.core.entity.define.mode.PlayerLeaveRoomMode;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.implement.RoomImpl;
import com.tenio.core.entity.setting.InitialRoomSetting;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;

/**
 * This class provides all supported APIs from the server.
 *
 * @see ServerApiImpl
 */
public interface ServerApi {

// add @see event interfaces for all methods docs

/**
* Allows creating an instance of player in the server.
* <p>
* The player does not contain session, it should act like a server bot.
*
* @param playerName an unique player's name in the server
* @see Session
*/
  void login(String playerName);

/**
* Allows creating an instance of player in the server.
*
* @param playerName an unique player's name in the server
* @param session a {@link Session} attached to the player
*/
  void login(String playerName, Session session);

/**
* Removes a player from the server.
*
* @param player the current {@link Player} in the server
*/
  void logout(Player player);

/**
* Removes a player manually from the server.
*
* @param player the player needs to be removed
* @param message a text deliveried to the player before he leave
* @param delayInSeconds the count down time in seconds for removing the player. In case the player has to be out immediately, this value should be <code>0</code>
* @throws UnsupportedOperationException this method is not supported at the moment
*/
  default void kickPlayer(Player player, String message, int delayInSeconds) {
    throw new UnsupportedOperationException("Unsupported at the moment");
  }

/**
* Prevents a player from logging in the server.
*
* @param player the player needs to be banned
* @param message a text deliveried to the player before he is banned
* @param banMode the rule applied for the player
* @param durationInMinutes how long the player take punishment calculated in minutes
* @param delayInSeconds the count down time in seconds for banning the player. In case the player has to be banned immediately, this value should be <code>0</code>
* @throws UnsupportedOperationException this method is not supported at the moment
* @see PlayerBanMode
*/
  default void banPlayer(Player player, String message, PlayerBanMode banMode,
                         int durationInMinutes,
                         int delayInSeconds) {
    throw new UnsupportedOperationException("Unsupported at the moment");
  }

/**
* Creates a new room on the server without owner.
*
* @param setting all room settings at the time its created
* @return a new instance of {@link Room}
* @see InitialRoomSetting
*/
  default Room createRoom(InitialRoomSetting setting) {
    return createRoom(setting, null);
  }

/**
* Creates a new room on the server.
*
* @param setting all room settings at the time its created
* @param owner the owner of this room, owner can also be declared by <code>null</code> value
* @return a new instance of {@link Room}
* @see InitialRoomSetting
* @see Player
*/
  Room createRoom(InitialRoomSetting setting, Player owner);

/**
* Retrieves a player activating on the server by using its name.
*
* @param playerName the unique value for player's name
* @return a corresponding instance of {@link Player}
*/
  Player getPlayerByName(String playerName);

// consider remove
  Player getPlayerBySession(Session session);

/**
* Fetches the current number of players activating on the server.
*
* @return the current number of players
*/
  int getPlayerCount();

// change to using iterator
  Collection<Player> getAllPlayers();

// getReadonlyListPlayers

/**
* Retrieves a room instance by its id.
*
* @param roomId the room's id
* @return a {@link Room} instance
*/
  Room getRoomById(long roomId);

// get iterator for rooms

// getReadonlyListRooms

/**
* Allows a player to join a particular room.
*
* @param player the joining {@link Player}
* @param room the current {@link Room}
* @param roomPassword a credential using for joining room. In case of free join, this value would be set by <code>null</code>
* @param slotInRoom the position of player located in the room
* @param asSpectator set by <code>true</code> if the player operating in room as a spectator, otherwise <code>false</code>
*/
  void joinRoom(Player player, Room room, String roomPassword, int slotInRoom, boolean asSpectator);

/**
* Allows a player to join a particular room.
*
* @param player the joining {@link Player}
* @param room the current {@link Room}
*/
  default void joinRoom(Player player, Room room) {
    joinRoom(player, room, null, RoomImpl.DEFAULT_SLOT, false);
  }

/**
* Changes the role of a player in its room, from player to spectator.
*
* @param player the working {@link Player}
* @param room the player's {@link Room}
* @throws UnsupportedOperationException this method is not supported at the moment
*/
  default void switchPlayerToSpectator(Player player, Room room) {
    throw new UnsupportedOperationException("Unsupported at the moment");
  }

/**
* Changes the role of a player in its room, from spectator to player.
*
* @param player the working spectator ({@link Player})
* @param room the spectator's {@link Room}
* @param targetSlot the new position of transformed player in its room
* @throws UnsupportedOperationException this method is not supported at the moment
*/
  default void switchSpectatorToPlayer(Player player, Room room, int targetSlot) {
    throw new UnsupportedOperationException("Unsupported at the moment");
  }

/**
* Makes a player to leave its current room.
*
* @param player the leaving {@link Player}
* @param leaveRoomMode the rule {@link PlayerLeaveRoomMode} applied for leaving player
*/
  void leaveRoom(Player player, PlayerLeaveRoomMode leaveRoomMode);

/**
* Removes a room from the server.
*
* @param room the removing {@link Room}
* @param removeRoomMode the rule {@link RoomRemoveMode} applied for removing room
*/
  void removeRoom(Room room, RoomRemoveMode removeRoomMode);

/**
* Sends a message from a player to all receipients in a room.
*
* @param sender the sender {@link Player}
* @param room the received {@link Room}
* @param message the sending {@link ServerMessage}
* @throws UnsupportedOperationException the method is not supported at the moment
*/
  default void sendPublicMessage(Player sender, Room room, ServerMessage message) {
    throw new UnsupportedOperationException("Unsupported at the moment");
  }

/**
* Sends a message from a player to a receipient.
*
* @param sender the sender {@link Player}
* @param receipient the received {@link Player}
* @param message the sending {@link ServerMessage}
* @throws UnsupportedOperationException the method is not supported at the moment
*/
  default void sendPrivateMessage(Player sender, Player recipient, ServerMessage message) {
    throw new UnsupportedOperationException("Unsupported at the moment");
  }
}
