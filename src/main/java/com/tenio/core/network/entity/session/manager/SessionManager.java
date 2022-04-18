/*
The MIT License

Copyright (c) 2016-2022 kong <congcoi123@gmail.com>

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

package com.tenio.core.network.entity.session.manager;

import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.define.mode.ConnectionDisconnectMode;
import com.tenio.core.entity.define.mode.PlayerDisconnectMode;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.manager.Manager;
import com.tenio.core.network.entity.packet.PacketQueue;
import com.tenio.core.network.entity.packet.policy.DefaultPacketQueuePolicy;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import com.tenio.core.network.entity.session.Session;
import io.netty.channel.Channel;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * A session manager.
 */
public interface SessionManager extends Manager {

  /**
   * Creates a new socket (TCP) session and adds it to the management list.
   *
   * @param socketChannel the {@link SocketChannel}
   * @param selectionKey  the {@link SelectionKey}
   * @return a new instance of {@link Session}
   */
  Session createSocketSession(SocketChannel socketChannel, SelectionKey selectionKey);

  /**
   * Removes a socket (TCP) session from the management list.
   *
   * @param socketChannel the {@link SocketChannel}
   * @throws NullPointerException if the removing session could not be found
   */
  void removeSessionBySocket(SocketChannel socketChannel) throws NullPointerException;

  /**
   * Retrieves a socket (TCP) session from the management list.
   *
   * @param socketChannel the {@link SocketChannel} associating to the session
   * @return a corresponding {@link Session} instance, otherwise <code>null</code>
   */
  Session getSessionBySocket(SocketChannel socketChannel);

  /**
   * Creates a new WebSocket session and adds it to the management list.
   *
   * @param webSocketChannel the WebSocket {@link Channel}
   * @return a new instance of {@link Session}
   */
  Session createWebSocketSession(Channel webSocketChannel);

  /**
   * Removes a WebSocket session from the management list.
   *
   * @param webSocketChannel the WebSocket {@link Channel}
   * @throws NullPointerException if the removing session could not be found
   */
  void removeSessionByWebSocket(Channel webSocketChannel) throws NullPointerException;

  /**
   * Retrieves a WebSocket session from the management list.
   *
   * @param webSocketChannel the WebSocket {@link Channel} associating to the session
   * @return a corresponding {@link Session} instance, otherwise <code>null</code>
   */
  Session getSessionByWebSocket(Channel webSocketChannel);

  /**
   * Allows a session to use datagram (UDP) channel for communication. This should be applied for
   * socket (TCP) sessions only.
   *
   * @param datagramChannel the {@link DatagramChannel} available on the server
   * @param remoteAddress   the remote {@link SocketAddress} of client side which is using the
   *                        session to connect to the server
   * @param session         a {@link Session} is in use
   * @throws IllegalArgumentException whenever an invalid value is used
   */
  void addDatagramForSession(DatagramChannel datagramChannel, SocketAddress remoteAddress,
                             Session session)
      throws IllegalArgumentException;

  /**
   * Retrieves a socket (TCP) session from the management list.
   *
   * @param remoteAddress the remote {@link SocketAddress} of client side which is using the
   *                      session to connect to the server
   * @return a corresponding {@link Session} instance if the session is allowed to use the
   * datagram channel and the remote address is present, otherwise <code>null</code>
   */
  Session getSessionByDatagram(SocketAddress remoteAddress);

  /**
   * Emits an event on the server.
   *
   * @param event  the valid {@link ServerEvent} is using on the server
   * @param params a list of {@link Object} parameters
   * @see EventManager
   */
  void emitEvent(ServerEvent event, Object... params);

  /**
   * Sets a packet queue policy class for the session manager.
   *
   * @param clazz the implementation class of {@link PacketQueuePolicy} used to apply rules for
   *              the packet queue
   * @throws InstantiationException    it is caused by
   *                                   Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws IllegalAccessException    it is caused by
   *                                   Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws IllegalArgumentException  it is related to the illegal argument exception
   * @throws InvocationTargetException it is caused by
   *                                   Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws NoSuchMethodException     it is caused by
   *                                   {@link Class#getDeclaredConstructor(Class[])}
   * @throws SecurityException         it is related to the security exception
   * @see PacketQueue
   * @see DefaultPacketQueuePolicy
   */
  void setPacketQueuePolicy(Class<? extends PacketQueuePolicy> clazz)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException,
      NoSuchMethodException, SecurityException;

  /**
   * Sets the packet queue size.
   *
   * @param queueSize the <code>integer</code> value of new size for the packet queue
   * @see PacketQueue
   * @see PacketQueuePolicy
   */
  void setPacketQueueSize(int queueSize);

  /**
   * Removes a session from its manager, this method should not be invoked directly. Calls
   * instead the method {@link Session#close(ConnectionDisconnectMode, PlayerDisconnectMode)}
   * to completely eliminate the session.
   *
   * @param session the removing {@link Session}
   */
  void removeSession(Session session);

  /**
   * Retrieves the current number of sessions in the management list.
   *
   * @return the current number of sessions in the management list
   */
  int getSessionCount();
}
