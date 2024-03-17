package com.tenio.core.network.kcp.handler;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataType;
import com.tenio.common.data.DataUtility;
import com.tenio.common.logger.SystemLogger;
import com.tenio.core.configuration.define.ServerEvent;
import com.tenio.core.entity.Player;
import com.tenio.core.entity.define.result.AccessDatagramChannelResult;
import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import kcp.KcpListener;
import kcp.Ukcp;

public class KcpHandler implements KcpListener {

  private final EventManager eventManager;
  private final SessionManager sessionManager;
  private final DataType dataType;
  private final NetworkReaderStatistic networkReaderStatistic;
  private final KcpHandlerPrivateLogger logger;

  public KcpHandler(EventManager eventManager, SessionManager sessionManager,
                    DataType dataType, NetworkReaderStatistic networkReaderStatistic) {
    this.eventManager = eventManager;
    this.sessionManager = sessionManager;
    this.dataType = dataType;
    this.networkReaderStatistic = networkReaderStatistic;
    logger = new KcpHandlerPrivateLogger();
  }

  @Override
  public void onConnected(Ukcp ukcp) {
    // Do nothing
  }

  @Override
  public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
    var binary = new byte[byteBuf.readableBytes()];
    byteBuf.getBytes(byteBuf.readerIndex(), binary);

    var message = DataUtility.binaryToCollection(dataType, binary);
    Session session = sessionManager.getSessionByKcp(ukcp);

    if (Objects.isNull(session)) {
      processDatagramChannelReadMessageForTheFirstTime(ukcp, message);
    } else {
      if (session.isActivated()) {
        session.addReadBytes(binary.length);
        networkReaderStatistic.updateReadBytes(binary.length);
        networkReaderStatistic.updateReadPackets(1);
        eventManager.emit(ServerEvent.SESSION_READ_MESSAGE, session, message);
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("READ KCP CHANNEL", "Session is inactivated: ", session.toString());
        }
      }
    }
  }

  @Override
  public void handleException(Throwable cause, Ukcp ukcp) {
    var session = sessionManager.getSessionByKcp(ukcp);
    if (Objects.nonNull(session)) {
      if (logger.isErrorEnabled()) {
        logger.error(cause, "Session: ", session.toString());
      }
      eventManager.emit(ServerEvent.SESSION_OCCURRED_EXCEPTION, session, cause);
    } else {
      if (logger.isErrorEnabled()) {
        logger.error(cause, "Exception was occurred on channel: %s", ukcp.toString());
      }
    }
  }

  @Override
  public void handleClose(Ukcp ukcp) {
    // Do nothing at the moment
  }

  private void processDatagramChannelReadMessageForTheFirstTime(Ukcp ukcp, DataCollection message) {
    // verify the kcp channel accessing request
    Object checkingPlayer = null;
    try {
      checkingPlayer = eventManager.emit(ServerEvent.ACCESS_KCP_CHANNEL_REQUEST_VALIDATION, message);
    } catch (Exception exception) {
      if (logger.isErrorEnabled()) {
        logger.error(exception, message);
      }
    }

    if (!(checkingPlayer instanceof Optional<?> optionalPlayer)) {
      return;
    }

    if (optionalPlayer.isEmpty()) {
      eventManager.emit(ServerEvent.ACCESS_KCP_CHANNEL_REQUEST_VALIDATION_RESULT,
          optionalPlayer,
          AccessDatagramChannelResult.PLAYER_NOT_FOUND);
    } else {
      Player player = (Player) optionalPlayer.get();
      if (!player.containsSession() || player.getSession().isEmpty()) {
        eventManager.emit(ServerEvent.ACCESS_KCP_CHANNEL_REQUEST_VALIDATION_RESULT,
            optionalPlayer,
            AccessDatagramChannelResult.SESSION_NOT_FOUND);
      } else {
        Session session = player.getSession().get();
        if (!session.isTcp()) {
          eventManager.emit(ServerEvent.ACCESS_KCP_CHANNEL_REQUEST_VALIDATION_RESULT,
              optionalPlayer,
              AccessDatagramChannelResult.INVALID_SESSION_PROTOCOL);
        } else {
          var sessionInstance = ((Player) optionalPlayer.get()).getSession().get();
          sessionInstance.setKcpChannel(ukcp);
          sessionManager.addKcpForSession(ukcp, sessionInstance);

          eventManager.emit(ServerEvent.ACCESS_KCP_CHANNEL_REQUEST_VALIDATION_RESULT,
              optionalPlayer,
              AccessDatagramChannelResult.SUCCESS);
        }
      }
    }
  }
}

class KcpHandlerPrivateLogger extends SystemLogger {
}
