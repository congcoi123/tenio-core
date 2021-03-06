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

package com.tenio.core.network.netty.websocket;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.util.Objects;

/**
 * <a href="https://en.wikipedia.org/wiki/WebSocket">WebSocket</a> is distinct
 * from HTTP. Both protocols are located at layer 7 in the OSI model and depend
 * on TCP at layer 4. Although they are different, RFC 6455 states that
 * WebSocket "is designed to work over HTTP ports 80 and 443 as well as to
 * support HTTP proxies and intermediaries," thus making it compatible with the
 * HTTP protocol. To achieve compatibility, the WebSocket handshake uses the
 * HTTP Upgrade header[1] to change from the HTTP protocol to the WebSocket
 * protocol.
 */
public final class NettyWsHandShake extends ChannelInboundHandlerAdapter {

  private final EventManager eventManager;
  private final SessionManager sessionManager;
  private final ConnectionFilter connectionFilter;
  private final NetworkReaderStatistic networkReaderStatistic;

  private NettyWsHandShake(EventManager eventManager, SessionManager sessionManager,
                           ConnectionFilter connectionFilter,
                           NetworkReaderStatistic networkReaderStatistic) {
    this.eventManager = eventManager;
    this.sessionManager = sessionManager;
    this.connectionFilter = connectionFilter;
    this.networkReaderStatistic = networkReaderStatistic;
  }

  public static NettyWsHandShake newInstance(EventManager eventManager,
                                             SessionManager sessionManager,
                                             ConnectionFilter connectionFilter,
                                             NetworkReaderStatistic networkReaderStatistic) {
    return new NettyWsHandShake(eventManager, sessionManager, connectionFilter,
        networkReaderStatistic);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msgRaw) {

    // check the request for handshake
    if (msgRaw instanceof HttpRequest) {
      var httpRequest = (HttpRequest) msgRaw;
      var headers = httpRequest.headers();

      if (headers.get("Connection").equalsIgnoreCase("Upgrade")
          || headers.get("Upgrade").equalsIgnoreCase("WebSocket")) {

        // add new handler to the existing pipeline to handle HandShake-WebSocket
        // Messages
        ctx.pipeline()
            .replace(this, "handler", NettyWsHandler.newInstance(eventManager, sessionManager,
                connectionFilter, networkReaderStatistic));

        // do the Handshake to upgrade connection from HTTP to WebSocket protocol
        handleHandshake(ctx, httpRequest);
      }
    }
  }

  /**
   * Do the handshaking for WebSocket request.
   *
   * @param ctx the channel, see {@link ChannelHandlerContext}
   * @param req the request, see {@link HttpRequest}
   */
  private void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
    var wsFactory = new WebSocketServerHandshakerFactory(getWebSocketUrl(req), null, true);
    /*
     The handshake starts with an HTTP request/response, allowing servers to
     handle HTTP connections as well as WebSocket connections on the same port.
     Once the connection is established, communication switches to a bidirectional
     binary protocol which does not conform to the HTTP protocol.
     */
    var handshaker = wsFactory.newHandshaker(req);
    if (Objects.isNull(handshaker)) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
    } else {
      handshaker.handshake(ctx.channel(), req);
    }
  }

  private String getWebSocketUrl(HttpRequest req) {
    return "ws://" + req.headers().get("Host") + req.uri();
  }
}
