/**
 * Exported packages for the tenio.core module.
 */
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

  exports com.tenio.core;
  exports com.tenio.core.api;
  exports com.tenio.core.entity;
  exports com.tenio.core.entity.data;
  exports com.tenio.core.entity.define.mode;
  exports com.tenio.core.entity.define.result;
  exports com.tenio.core.entity.setting;
  exports com.tenio.core.entity.setting.strategy;
  exports com.tenio.core.entity.setting.strategy.implement;
  exports com.tenio.core.extension;
  exports com.tenio.core.extension.events;
  exports com.tenio.core.network.define;
  exports com.tenio.core.network.entity.packet;
  exports com.tenio.core.network.entity.packet.policy;
  exports com.tenio.core.network.entity.protocol;
  exports com.tenio.core.network.entity.session;
  exports com.tenio.core.network.jetty.servlet;
  exports com.tenio.core.network.jetty.servlet.support;
  exports com.tenio.core.network.security.filter;
  exports com.tenio.core.network.zero.codec;
  exports com.tenio.core.network.zero.codec.compression;
  exports com.tenio.core.network.zero.codec.decoder;
  exports com.tenio.core.network.zero.codec.encoder;
  exports com.tenio.core.network.zero.codec.encryption;
}
