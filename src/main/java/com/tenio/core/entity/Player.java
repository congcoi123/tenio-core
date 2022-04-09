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

import com.tenio.core.network.entity.session.Session;

/**
 * The abstract player entity used on the server.
 */
public interface Player {

  /**
   * Retrieves the player's name. This value must be unique.
   *
   * @return the player's name in (@link String}
   */
  String getName();

/**
* Determines whether the player contains a session.
*
* @return <code>true</code> if the player contains a session, otherwise <code>false</code>
*/
  boolean containsSession();

/**
* Determines whether the player is in a state.
*
* @param state a {@link PlayerState} is in judgment
* @return <code>true</code> if the player in that state, otherwise <code>false</code>
*/
  boolean isState(PlayerState state);

/**
* Retrieves the player's current state.
*
* @return the current {@link PlayerState}
*/
  PlayerState getState();

/**
* Sets current state value for the player.
*
* @param state a new value of {@link PlayerState} set to the player
*/
  void setState(PlayerState state);

/**
* Determines whether the player is activated.
*
* @return <code>true</code> if the player is activated, otherwise <code>false</code>
*/
  boolean isActivated();

/**
* Sets the current active state for the player.
*
* @param activated the new <code>boolean</code> state of player
*/
  void setActivated(boolean activated);

/**
* Determines whether the player is loggedin the server.
*
* @return <code>true</code> if the player is loggedin, otherwise <code>false</code>
*/
  boolean isLoggedIn();

/**
* Sets the current loggedin state for the player.
*
* @param loggedIn the new <code>boolean</code> state of player
*/
  void setLoggedIn(boolean loggedIn);

/**
* Retrieves the last loggedin time in miliseconds of the player on the server.
*
* @return a <code>long</code> miliseconds value
*/
  long getLastLoggedInTime();

// Use Optional
  Session getSession();

/**
* Associates a session to the player.
*
* @param session a {@link Session}
*/
  void setSession(Session session);

/**
* Determines whether the player is in a room.
*
* @return <code>true</code> if the player is in a room, otherwise <code>false</code>
*/
  boolean isInRoom();

/**
* Determines whether the player plays as a spectator in a room.
*
* @return <code>true</code> if the player is playing as a spectator, otherwise <code>false</code>
*/
  boolean isSpectator();

/**
* Marks the player working as a spectator.
*
* @param isSpectator set value to <code>true</code> for marking the player as a spectator, contrary set it to <code>false</code>
*/
  void setSpectator(boolean isSpectator);

// Use Optional
  Room getCurrentRoom();

/**
* Sets associated room to the player.
*
* @param room an instance of {@link Room}
*/
  void setCurrentRoom(Room room);

/**
* Retrieves the last time the player left its room.
*
* @return the miliseconds in <code>long</code> value
*/
  long getLastJoinedRoomTime();

/**
* Retrieves the player's slot in its room.
*
* @return the <code>integer</code> value of the player slot position
*/
  int getPlayerSlotInCurrentRoom();

/**
* Set a slot value for the player in its room.
*
* @param slot the <code>integer</code> value of the player slot position
*/
  void setPlayerSlotInCurrentRoom(int slot);

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
* Wipes out all the player's information.
*/
  void clean();
}
