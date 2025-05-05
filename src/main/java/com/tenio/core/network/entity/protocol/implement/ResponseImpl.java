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

package com.tenio.core.network.entity.protocol.implement;

import com.tenio.common.data.DataType;
import com.tenio.core.entity.Player;
import com.tenio.core.network.define.ResponsePriority;
import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.server.ServerImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The implementation for response.
 *
 * @see Response
 */
public final class ResponseImpl implements Response {

  private byte[] content;
  private Collection<Player> players;
  private Collection<Player> nonSessionPlayers;
  private Collection<Session> socketSessions;
  private Collection<Session> datagramSessions;
  private Collection<Session> kcpSessions;
  private Collection<Session> webSocketSessions;
  private ResponsePriority priority;
  private boolean isPrioritizedUdp;
  private boolean isPrioritizedKcp;
  private boolean isEncrypted;
  private DataType contentType;

  private ResponseImpl() {
    players = null;
    socketSessions = new ArrayList<>();
    datagramSessions = new ArrayList<>();
    kcpSessions = new ArrayList<>();
    webSocketSessions = new ArrayList<>();
    nonSessionPlayers = new ArrayList<>();
    priority = ResponsePriority.NORMAL;
    isPrioritizedUdp = false;
    isPrioritizedKcp = false;
    isEncrypted = false;
    contentType = DataType.MSG_PACK;
  }

  /**
   * Creates a new response instance.
   *
   * @return a new instance of {@link Response}
   */
  public static Response newInstance() {
    return new ResponseImpl();
  }

  @Override
  public byte[] getContent() {
    return content;
  }

  @Override
  public Response setContent(byte[] content) {
    this.content = content;
    return this;
  }

  @Override
  public Collection<Player> getRecipientPlayers() {
    return players;
  }

  @Override
  public Collection<Player> getNonSessionRecipientPlayers() {
    return nonSessionPlayers;
  }

  @Override
  public Collection<Session> getRecipientSocketSessions() {
    return socketSessions;
  }

  @Override
  public Collection<Session> getRecipientDatagramSessions() {
    return datagramSessions;
  }

  @Override
  public Collection<Session> getRecipientKcpSessions() {
    return kcpSessions;
  }

  @Override
  public Collection<Session> getRecipientWebSocketSessions() {
    return webSocketSessions;
  }

  @Override
  public Response setRecipientPlayers(Collection<Player> players) {
    this.players = players;
    return this;
  }

  @Override
  public Response setRecipientPlayer(Player player) {
    if (Objects.nonNull(player)) {
      if (Objects.isNull(players)) {
        players = new ArrayList<>();
      }
      players.add(player);
    }
    return this;
  }

  @Override
  public Response setRecipientSessions(Collection<Session> sessions) {
    if (Objects.nonNull(sessions)) {
      sessions.forEach(this::checksAndAddsSession);
    }
    return this;
  }

  @Override
  public Response setRecipientSession(Session session) {
    if (Objects.nonNull(session)) {
      checksAndAddsSession(session);
    }
    return this;
  }

  @Override
  public Response prioritizedUdp() {
    isPrioritizedUdp = true;
    isPrioritizedKcp = false;
    return this;
  }

  @Override
  public Response prioritizedKcp() {
    isPrioritizedKcp = true;
    isPrioritizedUdp = false;
    return this;
  }

  @Override
  public Response encrypted() {
    isEncrypted = true;
    return this;
  }

  @Override
  public Response priority(ResponsePriority priority) {
    this.priority = priority;
    return this;
  }

  @Override
  public boolean isEncrypted() {
    return isEncrypted;
  }

  @Override
  public ResponsePriority getPriority() {
    return priority;
  }

  @Override
  public void write() {
    constructRecipientPlayers();
    ServerImpl.getInstance().write(this, false);
  }

  @Override
  public void writeInDelay(long delayInMilliseconds) {
    try {
      TimeUnit.MILLISECONDS.sleep(delayInMilliseconds);
      write();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void writeThenClose() {
    constructRecipientPlayers();
    ServerImpl.getInstance().write(this, true);
  }

  private void constructRecipientPlayers() {
    if (Objects.isNull(players)) {
      return;
    }

    players.forEach(player -> {
      if (Objects.nonNull(player)) {
        if (player.containsSession()) {
          var session = player.getSession();
          session.ifPresent(this::checksAndAddsSession);
        } else {
          nonSessionPlayers.add(player);
        }
      }
    });
  }

  private void checksAndAddsSession(Session session) {
    if (Objects.isNull(session)) {
      return;
    }

    if (session.isTcp()) {
      if (isPrioritizedUdp && session.containsUdp()) {
        datagramSessions.add(session);
      } else if (isPrioritizedKcp && session.containsKcp()) {
        kcpSessions.add(session);
      } else {
        socketSessions.add(session);
      }
    } else if (session.isWebSocket()) {
      webSocketSessions.add(session);
    }
  }

  @Override
  public String toString() {
    return "Response{" +
        "content(bytes)=" + (Objects.nonNull(content) ? content.length : "null") +
        ", players=" + players +
        ", nonSessionPlayers=" + nonSessionPlayers +
        ", socketSessions=" + socketSessions +
        ", datagramSessions=" + datagramSessions +
        ", kcpSessions=" + kcpSessions +
        ", webSocketSessions=" + webSocketSessions +
        ", priority=" + priority +
        ", isPrioritizedUdp=" + isPrioritizedUdp +
        ", isPrioritizedKcp=" + isPrioritizedKcp +
        ", isEncrypted=" + isEncrypted +
        '}';
  }
}
