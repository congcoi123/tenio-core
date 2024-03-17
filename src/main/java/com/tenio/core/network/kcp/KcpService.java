package com.tenio.core.network.kcp;

import com.tenio.common.data.DataType;
import com.tenio.core.network.configuration.SocketConfiguration;
import com.tenio.core.network.entity.packet.Packet;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.statistic.NetworkReaderStatistic;
import com.tenio.core.network.statistic.NetworkWriterStatistic;
import com.tenio.core.service.Service;

public interface KcpService extends Service {
  
  void setSessionManager(SessionManager sessionManager);

  void setNetworkReaderStatistic(NetworkReaderStatistic networkReaderStatistic);

  void setNetworkWriterStatistic(NetworkWriterStatistic networkWriterStatistic);

  void setKcpSocketConfiguration(SocketConfiguration kcpSocketConfiguration);

  void setDataType(DataType dataType);

  void write(Packet packet);
}
