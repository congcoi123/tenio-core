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

package com.tenio.core.entity.setting;

import com.tenio.common.logger.SystemLogger;
import com.tenio.core.entity.define.mode.RoomRemoveMode;
import com.tenio.core.entity.setting.strategy.RoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomCredentialValidatedStrategy;
import com.tenio.core.entity.setting.strategy.implement.DefaultRoomPlayerSlotGeneratedStrategy;
import java.lang.reflect.InvocationTargetException;

/**
 * The initialized information is for creating a new room.
 */
public final class InitialRoomSetting {

  private final String name;
  private final String password;
  private final int maxPlayers;
  private final int maxSpectators;
  private final boolean activated;
  private final RoomRemoveMode roomRemoveMode;
  private final RoomCredentialValidatedStrategy roomCredentialValidatedStrategy;
  private final RoomPlayerSlotGeneratedStrategy roomPlayerSlotGeneratedStrategy;

  private InitialRoomSetting(Builder builder) {
    name = builder.name;
    password = builder.password;
    maxPlayers = builder.maxPlayers;
    maxSpectators = builder.maxSpectators;
    activated = builder.activated;
    roomRemoveMode = builder.removeMode;
    roomCredentialValidatedStrategy = builder.credentialValidatedStrategy;
    roomPlayerSlotGeneratedStrategy = builder.playerSlotGeneratedStrategy;

  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public int getMaxSpectators() {
    return maxSpectators;
  }

  public boolean isActivated() {
    return activated;
  }

  public RoomRemoveMode getRoomRemoveMode() {
    return roomRemoveMode;
  }

  public RoomCredentialValidatedStrategy getRoomCredentialValidatedStrategy() {
    return roomCredentialValidatedStrategy;
  }

  public RoomPlayerSlotGeneratedStrategy getRoomPlayerSlotGeneratedStrategy() {
    return roomPlayerSlotGeneratedStrategy;
  }

  /**
   * The builder class for collecting setup information.
   */
  public static class Builder extends SystemLogger {

    private String name;
    private String password;
    private int maxPlayers;
    private int maxSpectators;
    private boolean activated;
    private RoomRemoveMode removeMode;
    private RoomCredentialValidatedStrategy credentialValidatedStrategy;
    private RoomPlayerSlotGeneratedStrategy playerSlotGeneratedStrategy;

    private Builder() {
      name = null;
      password = null;
      maxPlayers = 0;
      maxSpectators = 0;
      activated = false;
      removeMode = RoomRemoveMode.DEFAULT;
      credentialValidatedStrategy = null;
      playerSlotGeneratedStrategy = null;
    }

/**
* Creates a new instance.
*
* @return a new instance
*/
    public static Builder newInstance() {
      return new Builder();
    }

/**
* Sets room's name.
*
* @param name the {@link String} room's name
* @return the pointer of builder
*/
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

/**
* Sets room's password.
*
* @param password the {@link String} room's password
* @return the pointer of builder
*/
    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

/**
* Sets room's maximum number of players.
*
* @param maxPlayers the maximum number of players allowed be in the room
* @return the pointer of builder
*/
    public Builder setMaxPlayers(int maxPlayers) {
      this.maxPlayers = maxPlayers;
      return this;
    }

/**
* Sets room's maximum number of spectators.
*
* @param maxSpectators the maximum number of spectators allowed be in the room
* @return the pointer of builder
*/
    public Builder setMaxSpectators(int maxSpectators) {
      this.maxSpectators = maxSpectators;
      return this;
    }

/**
* Allows a room to be activated or not.
*
* @param activated set the flag's value to be <code>true</code> when the room is active, otherwise <code>false</code>
* @return the pointer of builder
*/
    public Builder setActivated(boolean activated) {
      this.activated = activated;
      return this;
    }

/**
* Sets removed mode for the room.
*
* @param roomRemoveMode the {@link RoomRemoveMode} decides rules applied to remove the room
* @return the pointer of builder
*/
// Check the scheduled tasks
    public Builder setRoomRemoveMode(RoomRemoveMode roomRemoveMode) {
      removeMode = roomRemoveMode;
      return this;
    }

/**
* Sets a strategy for validating credentials using for get in the room.
*
* @param clazz a class extends {@link RoomCredentialValidatedStrategy} 
* @return the pointer of builder
*/
// Check the default strategy
    public Builder setRoomCredentialValidatedStrategy(
        Class<? extends RoomCredentialValidatedStrategy> clazz) {
      credentialValidatedStrategy = (RoomCredentialValidatedStrategy) createNewInstance(clazz);
      return this;
    }

/**
* Sets a strategy for generating player's slots in the room.
*
* @param clazz a class extends {@link RoomPlayerSlotGeneratedStrategy} 
* @return the pointer of builder
*/
// Check the default strategy
    public Builder setRoomPlayerSlotGeneratedStrategy(
        Class<? extends RoomPlayerSlotGeneratedStrategy> clazz) {
      playerSlotGeneratedStrategy = (RoomPlayerSlotGeneratedStrategy) createNewInstance(clazz);
      return this;
    }

    /**
     * Initialization.
     *
     * @return a new building instance
     */
    public InitialRoomSetting build() {
      if (credentialValidatedStrategy == null) {
        credentialValidatedStrategy = (RoomCredentialValidatedStrategy) createNewInstance(
            DefaultRoomCredentialValidatedStrategy.class);
      }
      if (playerSlotGeneratedStrategy == null) {
        playerSlotGeneratedStrategy = (RoomPlayerSlotGeneratedStrategy) createNewInstance(
            DefaultRoomPlayerSlotGeneratedStrategy.class);
      }
      return new InitialRoomSetting(this);
    }

    private Object createNewInstance(Class<?> clazz) {
      Object object = null;
      try {
        object = clazz.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException
          | InvocationTargetException | NoSuchMethodException
          | SecurityException e) {
        error(e);
      }
      return object;
    }
  }
}
