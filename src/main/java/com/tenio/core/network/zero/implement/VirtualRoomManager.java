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

package com.tenio.core.network.zero.implement;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.room.Room;
import com.tenio.core.network.entity.room.manager.RoomManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A virtual thread-based room manager implementation.
 */
public final class VirtualRoomManager implements RoomManager {
  private final EventManager eventManager;
  private final Map<String, Room> rooms;
  private final ReentrantLock lock;
  private final AtomicBoolean isRunning;
  private Thread roomMonitorThread;

  private VirtualRoomManager(EventManager eventManager) {
    this.eventManager = eventManager;
    this.rooms = new ConcurrentHashMap<>();
    this.lock = new ReentrantLock();
    this.isRunning = new AtomicBoolean(true);
  }

  /**
   * Creates a new instance of the virtual room manager.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link VirtualRoomManager}
   */
  public static VirtualRoomManager newInstance(EventManager eventManager) {
    return new VirtualRoomManager(eventManager);
  }

  @Override
  public void initialize() {
    // Start room monitor thread
    roomMonitorThread = Thread.ofVirtual().start(() -> {
      while (isRunning.get()) {
        try {
          // Monitor rooms for inactivity
          rooms.forEach((id, room) -> {
            if (room.isExpired()) {
              removeRoom(id);
            }
          });
          
          // Sleep for a short duration
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          eventManager.emit(ServerEvent.SERVER_EXCEPTION, e);
        }
      }
    });
  }

  @Override
  public void addRoom(Room room) {
    lock.lock();
    try {
      rooms.put(room.getId(), room);
      eventManager.emit(ServerEvent.ROOM_ADDED, room);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void removeRoom(String roomId) {
    lock.lock();
    try {
      Room room = rooms.remove(roomId);
      if (room != null) {
        eventManager.emit(ServerEvent.ROOM_REMOVED, room);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Room getRoom(String roomId) {
    return rooms.get(roomId);
  }

  @Override
  public boolean containsRoom(String roomId) {
    return rooms.containsKey(roomId);
  }

  @Override
  public int getRoomCount() {
    return rooms.size();
  }

  @Override
  public void clear() {
    lock.lock();
    try {
      rooms.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    isRunning.set(false);
    if (roomMonitorThread != null) {
      roomMonitorThread.interrupt();
    }
    clear();
  }
} 