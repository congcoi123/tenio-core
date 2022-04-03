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

package com.tenio.core.entity.manager;

import com.tenio.core.entity.Player;
import com.tenio.core.entity.Room;
import com.tenio.core.exception.AddedDuplicatedPlayerException;
import com.tenio.core.exception.RemovedNonExistentPlayerFromRoomException;
import com.tenio.core.manager.Manager;
import com.tenio.core.network.entity.session.Session;
import java.util.Collection;

/**
 * All supported APIs for player management. A management can belong to a room or stand alone.
 */
public interface PlayerManager extends Manager {

/**
* Adds a new player in to the management list.
*
* @param player a created {@link Player}
* @throws AddedDuplicatedPlayerException when a same player is aready available in the management list
*/
  void addPlayer(Player player) throws AddedDuplicatedPlayerException;

/**
* Creates a new player without session and adds it in to the management list.
*
* @param name a unique player's name on the server
* @return a new instance of {@link Player}
* @throws AddedDuplicatedPlayerException when a same player is aready available in the management list
*/
// Changes parameter
  Player createPlayer(String name) throws AddedDuplicatedPlayerException;

/**
* Creates a new player with session and adds it in to the management list.
*
* @param name a unique player's name on the server
* @param session a session associated to the player
* @return a new instance of {@link Player}
* @throws AddedDuplicatedPlayerException when a same player is aready available in the management list
*/
// Changes parameter
  Player createPlayerWithSession(String name, Session session)
      throws AddedDuplicatedPlayerException, NullPointerException;

/**
* Retrieves a player by using its name.
*
* @param playerName a unique name of player on the server
* @return an instance of {@link Player}, other wise <code>null</code>
*/
  Player getPlayerByName(String playerName);

/**
* Retrieves a player by using its session.
*
* @param session the session associated to player on the server
* @return an instance of {@link Player}, other wise <code>null</code>
*/
  Player getPlayerBySession(Session session);

  // Retrieves readonly list of players, add iterator
  Collection<Player> getAllPlayers();

  // Retrieves readonly list of sessions, add iterator
  Collection<Session> getAllSessions();

/**
* Removes a player from the management list.
*
* @param playerName the player's name
* @throws RemovedNonExistentPlayerFromRoomException when the player is unvailable
*/
  void removePlayerByName(String playerName) throws RemovedNonExistentPlayerFromRoomException;

/**
* Removes a player from the management list.
*
* @param session the session associated to a player
* @throws RemovedNonExistentPlayerFromRoomException when the player is unvailable
*/
  void removePlayerBySession(Session session) throws RemovedNonExistentPlayerFromRoomException;

/**
* Determines whether the management list contains a player by checking its name.
*
* @param playerName the player's name
* @return <code>true</code> if the player is available, otherwise <code>false</code>
*/
  boolean containsPlayerName(String playerName);

/**
* Determines whether the management list contains a player by checking its session.
*
* @param session the session associated to a player
* @return <code>true</code> if the player is available, otherwise <code>false</code>
*/
  boolean containsPlayerSession(Session session);

/**
* Retrieves a room of the management list.
*
* @return an instance of {@link Room} or <code>null</code>
*/
// Checks null value
  Room getOwnerRoom();

/**
* Set a room for the management list.
*
* @param an instance of {@link Room} or <code>null</code>
*/
// Checks null value
  void setOwnerRoom(Room room);

/**
* Retrieves the current number of players in the list.
*
* @return the current number of players in the list
*/
  int getPlayerCount();

/**
* Removes all players from the list.
*/
  void clear();
}
