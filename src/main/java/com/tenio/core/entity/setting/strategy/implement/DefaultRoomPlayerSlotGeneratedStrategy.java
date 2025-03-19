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

package com.tenio.core.entity.setting.strategy.implement;

import com.tenio.core.entity.Room;
import com.tenio.core.entity.setting.strategy.RoomPlayerSlotGeneratedStrategy;
import java.util.BitSet;
import java.util.Objects;

/**
 * The default implementation for the strategy.
 */
public final class DefaultRoomPlayerSlotGeneratedStrategy
    implements RoomPlayerSlotGeneratedStrategy {

  private Room room;
  private BitSet slots;
  private int capacity;

  @Override
  public void initialize() {
    if (Objects.isNull(room)) {
      throw new NullPointerException("Room cannot be null");
    }
    capacity = room.getMaxParticipants();
    if (capacity < 0) {
      throw new IllegalArgumentException("Room capacity cannot be negative");
    }
    slots = new BitSet(capacity);
  }

  @Override
  public int getFreePlayerSlotInRoom() {
    if (Objects.isNull(room)) {
      throw new NullPointerException("Room cannot be null");
    }
    if (Objects.isNull(slots)) {
      initialize();
    }
    // Update capacity in case it changed
    int currentCapacity = room.getMaxParticipants();
    if (currentCapacity != capacity) {
      // If capacity increased, return the first new slot
      if (currentCapacity > capacity) {
        int firstNewSlot = capacity;
        capacity = currentCapacity;
        BitSet newSlots = new BitSet(capacity);
        newSlots.or(slots);
        slots = newSlots;
        return firstNewSlot;
      }
      // If capacity decreased, keep the old capacity but don't allow new slots beyond new capacity
      capacity = currentCapacity;
    }
    for (int i = 0; i < capacity; i++) {
      if (!slots.get(i)) {
        slots.set(i);
        return i;
      }
    }
    return Room.NIL_SLOT;
  }

  @Override
  public void freeSlotWhenPlayerLeft(int slot) {
    if (Objects.isNull(room)) {
      throw new NullPointerException("Room cannot be null");
    }
    if (Objects.isNull(slots)) {
      initialize();
    }
    if (slot < 0 || slot >= capacity) {
      throw new IllegalArgumentException("Invalid slot number: " + slot);
    }
    slots.clear(slot);
  }

  @Override
  public void tryTakeSlot(int slot) {
    if (Objects.isNull(room)) {
      throw new NullPointerException("Room cannot be null");
    }
    if (Objects.isNull(slots)) {
      initialize();
    }
    if (slot < 0 || slot >= capacity) {
      throw new IllegalArgumentException("Invalid slot number: " + slot);
    }
    if (slots.get(slot)) {
      throw new IllegalArgumentException("Slot " + slot + " is already taken");
    }
    slots.set(slot);
  }

  @Override
  public Room getRoom() {
    return room;
  }

  @Override
  public void setRoom(Room room) {
    this.room = room;
    if (Objects.nonNull(room)) {
      initialize();
    }
  }
}
