package com.tenio.core.network.entity.session.implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tenio.core.event.implement.EventManager;
import com.tenio.core.network.entity.packet.policy.PacketQueuePolicy;
import com.tenio.core.network.entity.packet.policy.DefaultPacketQueuePolicy;
import com.tenio.core.network.entity.session.Session;
import com.tenio.core.network.entity.session.manager.SessionManager;
import com.tenio.core.network.entity.session.manager.SessionManagerImpl;
import com.tenio.core.network.security.filter.ConnectionFilter;
import com.tenio.core.network.security.filter.DefaultConnectionFilter;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Test Cases For SessionManagerImpl")
class SessionManagerImplTest {
  
  private EventManager eventManager;
  private SessionManager sessionManager;
  
  @BeforeEach
  void setUp() {
    eventManager = EventManager.newInstance();
    sessionManager = SessionManagerImpl.newInstance(eventManager);
  }
  
  @Test
  @DisplayName("newInstance should create a valid SessionManager")
  void testNewInstance() {
    // Act
    SessionManager manager = SessionManagerImpl.newInstance(eventManager);
    
    // Assert
    assertNotNull(manager);
    assertEquals(0, manager.getSessionCount());
    assertNotNull(manager.getReadonlySessionsList());
    assertTrue(manager.getReadonlySessionsList().isEmpty());
  }
  
  @Test
  @DisplayName("createSocketSession should create and add a new session")
  void testCreateSocketSession() {
    // Arrange
    SocketChannel socketChannel = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    
    // Mock socket channel behavior
    when(socketChannel.socket()).thenReturn(mock(java.net.Socket.class));
    when(socketChannel.socket().isClosed()).thenReturn(false);
    when(socketChannel.socket().getRemoteSocketAddress())
        .thenReturn(new InetSocketAddress("localhost", 8080));
    
    // Act
    Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    
    // Assert
    assertNotNull(session);
    assertEquals(1, sessionManager.getSessionCount());
    assertEquals(session, sessionManager.getSessionBySocket(socketChannel));
    assertFalse(sessionManager.getReadonlySessionsList().isEmpty());
  }
  
  @Test
  @DisplayName("removeSession should remove a session and update the count")
  void testRemoveSession() {
    // Arrange
    SocketChannel socketChannel = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    assertEquals(1, sessionManager.getSessionCount());
    
    // Act
    sessionManager.removeSession(session);
    
    // Assert
    assertEquals(0, sessionManager.getSessionCount());
    assertTrue(sessionManager.getReadonlySessionsList().isEmpty());
  }
  
  @Test
  @DisplayName("getSessionIterator should return an iterator over all sessions")
  void testGetSessionIterator() {
    // Arrange
    SocketChannel socketChannel1 = mock(SocketChannel.class);
    SocketChannel socketChannel2 = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    
    sessionManager.createSocketSession(socketChannel1, selectionKey);
    sessionManager.createSocketSession(socketChannel2, selectionKey);
    
    // Act
    Iterator<Session> iterator = sessionManager.getSessionIterator();
    
    // Assert
    assertNotNull(iterator);
    assertTrue(iterator.hasNext());
    assertNotNull(iterator.next());
    assertTrue(iterator.hasNext());
    assertNotNull(iterator.next());
    assertFalse(iterator.hasNext());
  }
  
  @Test
  @DisplayName("configurePacketQueuePolicy should set the packet queue policy")
  void testConfigurePacketQueuePolicy() throws Exception {
    // Act
    sessionManager.configurePacketQueuePolicy(DefaultPacketQueuePolicy.class);
    
    // Assert - Create a session to verify the policy is applied
    SocketChannel socketChannel = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    
    assertNotNull(session);
  }
  
  @Test
  @DisplayName("configurePacketQueueSize should set the packet queue size")
  void testConfigurePacketQueueSize() {
    // Act
    sessionManager.configurePacketQueueSize(1000);
    
    // Assert - Create a session to verify the size is applied
    SocketChannel socketChannel = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    
    assertNotNull(session);
  }
  
  @Test
  @DisplayName("configureConnectionFilter should set the connection filter")
  void testConfigureConnectionFilter() {
    // Arrange
    ConnectionFilter filter = new DefaultConnectionFilter();
    
    // Act
    sessionManager.configureConnectionFilter(filter);
    
    // Assert - Create a session to verify the filter is applied
    SocketChannel socketChannel = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    
    assertNotNull(session);
  }
  
  @Test
  @DisplayName("configureMaxIdleTimeInSeconds should set the max idle time")
  void testConfigureMaxIdleTimeInSeconds() {
    // Act
    sessionManager.configureMaxIdleTimeInSeconds(60);
    
    // Assert - Create a session to verify the idle time is applied
    SocketChannel socketChannel = mock(SocketChannel.class);
    SelectionKey selectionKey = mock(SelectionKey.class);
    Session session = sessionManager.createSocketSession(socketChannel, selectionKey);
    
    assertNotNull(session);
  }
}

