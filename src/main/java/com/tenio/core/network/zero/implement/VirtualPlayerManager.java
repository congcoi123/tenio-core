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
import com.tenio.core.network.entity.player.Player;
import com.tenio.core.network.entity.player.manager.PlayerManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A virtual thread-based player manager implementation.
 */
public final class VirtualPlayerManager implements PlayerManager {
  private final EventManager eventManager;
  private final Map<String, Player> players;
  private final ReentrantLock lock;
  private final AtomicBoolean isRunning;
  private Thread playerMonitorThread;

  private VirtualPlayerManager(EventManager eventManager) {
    this.eventManager = eventManager;
    this.players = new ConcurrentHashMap<>();
    this.lock = new ReentrantLock();
    this.isRunning = new AtomicBoolean(true);
  }

  /**
   * Creates a new instance of the virtual player manager.
   *
   * @param eventManager the instance of {@link EventManager}
   * @return a new instance of {@link VirtualPlayerManager}
   */
  public static VirtualPlayerManager newInstance(EventManager eventManager) {
    return new VirtualPlayerManager(eventManager);
  }

  @Override
  public void initialize() {
    // Start player monitor thread
    playerMonitorThread = Thread.ofVirtual().start(() -> {
      while (isRunning.get()) {
        try {
          // Monitor players for inactivity
          players.forEach((id, player) -> {
            if (player.isExpired()) {
              removePlayer(id);
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
  public void addPlayer(Player player) {
    lock.lock();
    try {
      players.put(player.getId(), player);
      eventManager.emit(ServerEvent.PLAYER_ADDED, player);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void removePlayer(String playerId) {
    lock.lock();
    try {
      Player player = players.remove(playerId);
      if (player != null) {
        eventManager.emit(ServerEvent.PLAYER_REMOVED, player);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Player getPlayer(String playerId) {
    return players.get(playerId);
  }

  @Override
  public boolean containsPlayer(String playerId) {
    return players.containsKey(playerId);
  }

  @Override
  public int getPlayerCount() {
    return players.size();
  }

  @Override
  public void clear() {
    lock.lock();
    try {
      players.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    isRunning.set(false);
    if (playerMonitorThread != null) {
      playerMonitorThread.interrupt();
    }
    clear();
  }
} 