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

package com.tenio.core.recovery;

import com.tenio.common.data.DataCollection;
import com.tenio.core.entity.Player;
import com.tenio.core.network.entity.session.Session;

import java.time.Duration;

/**
 * Optional application bean enabling authenticated recovery for TCP and WebSocket sessions. The
 * application defines its handshake and response envelopes; no wire fields are reserved. Tokens
 * must be delivered over an authenticated, encrypted connection and must not be logged.
 */
public interface RecoveryPolicy {

    /**
     * @return grace interval during which a disconnected player may recover
     */
    default Duration grace() {
        return Duration.ofSeconds(30);
    }

    /**
     * @return maximum simultaneously enrolled players
     */
    default int maxPlayers() {
        return 4096;
    }

    /**
     * @return per-player replay byte bound
     */
    default int maxReplayBytes() {
        return 256 * 1024;
    }

    /**
     * @return per-player replay event bound
     */
    default int maxReplayEvents() {
        return 256;
    }

    /**
     * @return maximum retained event age; expiry can require a fresh snapshot/login
     */
    default Duration maxReplayAge() {
        return Duration.ofMinutes(2);
    }

    /**
     * @return chosen recovery behavior
     */
    default Strategy strategy() {
        return Strategy.SNAPSHOT;
    }

    /**
     * Parses recovery intent. Return null for normal login. Invalid recovery credentials are rejected
     * rather than being passed to the legacy retry handler or silently treated as a fresh login.
     *
     * @param session fresh transport session
     * @param message application handshake
     * @return credentials or null
     */
    Credentials credentials(Session session, DataCollection message);

    /**
     * Captures application state consistently with record() calls; implement under the application's
     * room mutation lock or ordered execution context. Never expose secret/internal properties.
     *
     * @param player       retained player
     * @param lastSequence current server replay cursor
     * @return snapshot and cursor, required for snapshot strategies
     */
    Snapshot snapshot(Player player, long lastSequence);

    /**
     * Delivers recovery data and the rotated token after transport attachment, before the resumed
     * event. The application must send the snapshot first, then replay in sequence, and communicate
     * the cursor.
     *
     * @param player  recovered player
     * @param session newly associated session
     * @param result  immutable recovery material
     */
    void resumed(Player player, Session session, RecoveryManager.Result result);

    /**
     * Runs capture and response delivery in the application's mutation context. Override when other
     * room writers can run concurrently: hold the same room lock used by mutations/broadcasts for the
     * entire operation, or submit it to the room's ordered executor and await completion. This method
     * must finish synchronously before the server emits PLAYER_CONNECTION_RESUMED.
     */
    default void recover(Player player, Session session, java.util.function.Supplier<RecoveryManager.Result> capture) {
        resumed(player, session, capture.get());
    }

    /**
     * Chooses bounded replay, a fresh snapshot, or a snapshot followed by newer replay events.
     */
    enum Strategy {
        /**
         * Return a fresh application snapshot.
         */
        SNAPSHOT,
        /**
         * Replay retained events; reject when the acknowledged cursor is no longer covered.
         */
        REPLAY,
        /**
         * Return a snapshot at its cursor followed by retained events after that cursor.
         */
        SNAPSHOT_AND_REPLAY
    }

    /**
     * Credentials extracted from an application handshake.
     *
     * @param identity     authenticated logical identity associated with the previously issued token
     * @param token        opaque token; possession is proof of recovery authority
     * @param lastSequence last fully applied replay sequence, starting at zero
     */
    record Credentials(String identity, String token, long lastSequence) {

        @Override
        public String toString() {
            return "RecoveryCredentials[redacted]";
        }
    }

    /**
     * Application snapshot captured atomically with the application's replay cursor.
     *
     * @param sequence last event represented in this snapshot
     * @param data     detached, application-owned state
     */
    record Snapshot(long sequence, DataCollection data) {
    }
}
