/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

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

package com.tenio.core.entity.replication;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Optional owner of explicitly registered replicated entities; lifecycle follows logical entities.
 */
public final class StateReplication implements AutoCloseable {

    private final Map<Key, ReplicatedState> states = new HashMap<>();
    private final int maximumEntities;
    private final int maximumBytes;
    private final int maximumSubscribers;
    private boolean closed;
    /**
     * Creates bounded replication resources; no entity properties are inspected or copied.
     */
    public StateReplication(int maximumEntities, int maximumBytes, int maximumSubscribers) {
        if (maximumEntities < 1
                || maximumBytes < 1
                || maximumBytes > 1024 * 1024
                || maximumSubscribers < 1) {
            throw new IllegalArgumentException("Invalid replication bounds");
        }
        this.maximumEntities = maximumEntities;
        this.maximumBytes = maximumBytes;
        this.maximumSubscribers = maximumSubscribers;
    }

    /**
     * Registers empty state for one authenticated player; duplicate registration is rejected.
     */
    public synchronized ReplicatedState createPlayer(String identity, Map<String, ReplicatedState.Definition> schema) {
        Objects.requireNonNull(identity);
        return create(new Key(true, identity), identity, schema);
    }

    /**
     * Registers empty room state with an optional authenticated owner; null means server-owned.
     */
    public synchronized ReplicatedState createRoom(long id, String owner, Map<String, ReplicatedState.Definition> schema) {
        return create(new Key(false, Long.toString(id)), owner, schema);
    }

    private ReplicatedState create(Key key, String owner, Map<String, ReplicatedState.Definition> schema) {
        if (closed || states.containsKey(key) || states.size() >= maximumEntities) {
            throw new IllegalStateException("Replication closed, duplicate entity, or capacity reached");
        }
        var state = new ReplicatedState(owner, key.player(), schema, maximumBytes, maximumSubscribers);
        states.put(key, state);
        return state;
    }

    /**
     * @return registered player state, or null
     */
    public synchronized ReplicatedState player(String identity) {
        return states.get(new Key(true, identity));
    }

    /**
     * @return registered room state, or null
     */
    public synchronized ReplicatedState room(long id) {
        return states.get(new Key(false, Long.toString(id)));
    }

    /**
     * Destroys player state on final logout, and removes its subscriptions to all other states.
     */
    public synchronized void removePlayer(String identity) {
        var state = states.remove(new Key(true, identity));
        if (state != null) {
            state.close();
        }
        states.values().forEach(value -> value.release(identity));
    }

    /**
     * Destroys room state and subscriber queues when the room is removed.
     */
    public synchronized void removeRoom(long id) {
        var state = states.remove(new Key(false, Long.toString(id)));
        if (state != null) {
            state.close();
        }
    }

    /**
     * Releases all registered entity state and subscriptions.
     */
    @Override
    public synchronized void close() {
        closed = true;
        states.values().forEach(ReplicatedState::close);
        states.clear();
    }

    private record Key(boolean player, String id) {
    }
}
