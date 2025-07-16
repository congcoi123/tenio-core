package com.tenio.core.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tenio.common.configuration.Configuration;
import com.tenio.common.data.DataType;
import com.tenio.common.data.msgpack.element.MsgPackMap;
import com.tenio.common.data.zero.ZeroArray;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.core.api.ServerApi;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.entity.setting.InitialRoomSetting;
import com.tenio.core.exception.UnsupportedDataTypeInUseException;
import com.tenio.core.network.entity.protocol.Response;
import com.tenio.core.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractHandlerTest {

  static class TestHandler extends AbstractHandler {
    private final Server injectedServer;
    TestHandler(Server server) { this.injectedServer = server; }
    // No @Override here, just for test injection
    protected Server getInjectedServer() { return injectedServer; }
  }

  private Server server;
  private AbstractHandler handler;

  @BeforeEach
  void setUp() throws Exception {
    server = mock(Server.class);
    handler = new TestHandler(server);
    // Inject mock server into private final field using reflection
    var field = AbstractHandler.class.getDeclaredField("server");
    field.setAccessible(true);
    field.set(handler, server);
  }

  @Test
  void testConfiguration() {
    Configuration config = mock(Configuration.class);
    when(server.getConfiguration()).thenReturn(config);
    assertEquals(config, handler.configuration());
  }

  @Test
  void testApi() {
    ServerApi api = mock(ServerApi.class);
    when(server.getApi()).thenReturn(api);
    assertEquals(api, handler.api());
  }

  @Test
  void testClientCommand() {
    ClientCommandManager ccm = mock(ClientCommandManager.class);
    when(server.getClientCommandManager()).thenReturn(ccm);
    assertEquals(ccm, handler.clientCommand());
  }

  @Test
  void testResponse() {
    assertNotNull(handler.response());
  }

  @Test
  void testArrayAndMapWithZeroDataType() {
    when(server.getDataType()).thenReturn(DataType.ZERO);
    assertNotNull(handler.array());
    assertNotNull(handler.map());
  }

  @Test
  void testArrayThrowsForNonZeroDataType() {
    when(server.getDataType()).thenReturn(DataType.MSG_PACK);
    assertThrows(UnsupportedDataTypeInUseException.class, () -> handler.array());
  }

  @Test
  void testMapThrowsForNonZeroDataType() {
    when(server.getDataType()).thenReturn(DataType.MSG_PACK);
    assertThrows(UnsupportedDataTypeInUseException.class, () -> handler.map());
  }

  @Test
  void testMsgmapWithMsgPackDataType() {
    when(server.getDataType()).thenReturn(DataType.MSG_PACK);
    assertNotNull(handler.msgmap());
  }

  @Test
  void testMsgmapThrowsForNonMsgPackDataType() {
    when(server.getDataType()).thenReturn(DataType.ZERO);
    assertThrows(UnsupportedDataTypeInUseException.class, () -> handler.msgmap());
  }

  @Test
  void testRoomSetting() {
    assertNotNull(handler.roomSetting());
    assertTrue(handler.roomSetting() instanceof InitialRoomSetting.Builder);
  }
} 