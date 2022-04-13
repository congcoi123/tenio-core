open module tenio.core {
  requires tenio.common;
  requires org.apache.logging.log4j.core;
  requires java.xml;
  requires io.netty.transport;
  requires io.netty.handler;
  requires java.management;
  requires jdk.management;
  requires jsr305;
  requires javax.servlet.api;
  requires org.json;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;
  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.common;
  requires javassist;
  requires io.netty.codec.http;
  requires org.apache.logging.log4j;

  exports com.tenio.core.extension.events;
  exports com.tenio.core.network.entity.session;
  exports com.tenio.core.extension;
  exports com.tenio.core.entity.define.result;
  exports com.tenio.core.entity.data;
  exports com.tenio.core.entity;
  exports com.tenio.core.network.entity.session.manager;
}
