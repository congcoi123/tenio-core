module tenio.core {
  exports com.tenio.core.extension.events;
  exports com.tenio.core.network.entity.session;
  exports com.tenio.core.extension;
  exports com.tenio.core.entity.define.result;
  exports com.tenio.core.entity.data;
  requires tenio.common;
  requires org.apache.logging.log4j.core;
  requires java.xml;
}
