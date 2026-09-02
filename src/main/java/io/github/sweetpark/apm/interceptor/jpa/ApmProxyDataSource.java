package io.github.sweetpark.apm.interceptor.jpa;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** JDBC DataSource를 프록시하여 커넥션 및 Statement 실행을 추적하는 래퍼 클래스입니다. */
public class ApmProxyDataSource implements DataSource {

  private final DataSource delegate;
  private final ApmProperties properties;

  public ApmProxyDataSource(DataSource delegate, ApmProperties properties) {
    this.delegate = delegate;
    this.properties = properties != null ? properties : ApmPropertiesHolder.getProperties();
  }

  public DataSource getTargetDataSource() {
    return delegate;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection conn = delegate.getConnection();
    return new ApmProxyConnection(conn, properties);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection conn = delegate.getConnection(username, password);
    return new ApmProxyConnection(conn, properties);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    if (iface.isInstance(delegate)) {
      return iface.cast(delegate);
    }
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this) || iface.isInstance(delegate) || delegate.isWrapperFor(iface);
  }
}
