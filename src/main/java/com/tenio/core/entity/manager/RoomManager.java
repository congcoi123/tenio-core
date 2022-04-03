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
import com.tenio.core.entity.setting.InitialRoomSetting;
import com.tenio.core.exception.AddedDuplicatedRoomException;
import com.tenio.core.exception.CreatedRoomException;
import com.tenio.core.manager.Manager;
import java.util.Collection;
import java.util.List;

/**
 * All supported APIs for room management.
 */
public interface RoomManager extends Manager {

/**
* Retrieves the maximum number of rooms on the server.
*
* @return the maximum number of room on the server
*/
  int getMaxRooms();

/**
* Sets the maximum number of room on the server.
*
* @param maxRooms the maximum number of rooms
*/
  void setMaxRooms(int maxRooms);

/**
* Adds a new room to the server.
*
* @param room an instance of {@link Room}
* @throws AddedDuplicatedRoomException when a room is already available on the server
*/
  void addRoom(Room room) throws AddedDuplicatedRoomException;

/**
* Creates a new room without owner and adds it to the server.
*
* @param roomSetting all settings created by a {@link InitialRoomSetting} builder
* @return an instance of {@link Room}
* @throws IllegalArgumentException when an invalid setting builder set
*/
  default Room createRoom(InitialRoomSetting roomSetting)
      throws IllegalArgumentException, CreatedRoomException {
    return createRoomWithOwner(roomSetting, null);
  }

/**
* Creates a new room with owner and adds it to the server.
*
* @param roomSetting all settings created by a {@link InitialRoomSetting} builder
* @param player a {@link Player} as the room's owner
* @return an instance of {@link Room}
* @throws IllegalArgumentException when an invalid setting builder set
* @throws CreatedRoomException when it fails to create a new room
*/
  Room createRoomWithOwner(InitialRoomSetting roomSetting, Player player)
      throws IllegalArgumentException, CreatedRoomException;

/**
* Determines whether room is in the management list by looking for its unique Id.
*
* @param roomId the <code>long</code> value room's id
* @return <code>true</code> if the searching room is available, otherwise <code>false</code>
*/
  boolean containsRoomId(long roomId);

/**
* Determines whether room is in the management list by looking for its name.
*
* @param roomName the {@link String} value room's name
* @return <code>true</code> if the searching room is available, otherwise <code>false</code>
*/
  boolean containsRoomName(String roomName);

/**
* Retrieves a room instance by looking for its unique Id.
*
* @param roomId the <code>long</code> value room's id
* @return an instance of {@link Room} if the searching room is available, otherwise <code>null</code>
*/
  Room getRoomById(long roomId);

// changes to use iterator (consider to remove)
  List<Room> getRoomListByName(String roomName);

// changes to use iterator (consider to remove)
  Collection<Room> getRoomList();
  
  // get a interior unchangable list of room

  void removeRoomById(long roomId);

/**
* Updates a room's name.
*
* @param room the updating {@link Room}
* @param roomName new {@link String} value of room's name
* @throws IllegalArgumentException when invalid name is set
*/
// Check strategy
  void changeRoomName(Room room, String roomName) throws IllegalArgumentException;

/**
* Updates a room's password.
*
* @param room the updating {@link Room}
* @param roomPassword new {@link String} value of room's password
* @throws IllegalArgumentException when invalid password is set
*/
// Check strategy
  void changeRoomPassword(Room room, String roomPassword) throws IllegalArgumentException;

/**
* Updates a room's capacity.
*
* @param room the updating {@link Room}
* @param maxPlayers new <code>integer</code> maximum number of players allows in the room
* @param maxSpectators new <code>integer</code> maximum number of spectators allows in the room
* @throws IllegalArgumentException when invalid value is set
*/
  void changeRoomCapacity(Room room, int maxPlayers, int maxSpectators)
      throws IllegalArgumentException;

/**
* Fetches the current number of rooms in the management list.
*
* @return the current number of rooms
*/
  int getRoomCount();

/**
* Removes all rooms in the management list.
*
* @throws UnsupportedOperationException the operation is not supported at the moment
*/
  default void clear() {
    throw new UnsupportedOperationException();
  }
}
