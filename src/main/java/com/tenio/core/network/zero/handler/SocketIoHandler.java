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

package com.tenio.core.network.zero.handler;

import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.zero.codec.decoder.BinaryPacketDecoder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * The socket IO handler.
 */
public interface SocketIoHandler extends BaseIoHandler {

/**
* When the first connection signal sent from client side to the server via socket (TCP) channel then this method is invoked.
*
* @param socketChannel the active {@link SocketChannel}
* @param selectionKey the {@link SelectionKey} used to distinguish each session
*/
  void channelActive(SocketChannel socketChannel, SelectionKey selectionKey);

// moves to base io
  void sessionRead(Session session, byte[] binary);

/**
* When the disconnection signal sent from client side to the server via socket (TCP) channel then this method is invoked.
*
* @param socketChannel the inactive {@link SocketChannel}
*/
  void channelInactive(SocketChannel socketChannel);

/**
* When any exception occured on th socket (TCP) channel then this method is invoked.
*
* @param socketChannel the {@link SocketChannel} created on the server
* @param exception an {@link exception} emerging
*/
  void channelException(SocketChannel socketChannel, Exception exception);

// moves to base io
  void sessionException(Session session, Exception exception);

/**
* Sets the packet decoder for the socket (TCP), every packet should be decoded for the following steps. In theory, every kind of decoder should be acceptable, for example a text decoder. However, this server is using binary decoder for all processes.
*
* @param packetDecoder an instance of {@link BinaryPacketDecoder}
*/
  void setPacketDecoder(BinaryPacketDecoder packetDecoder);
}
