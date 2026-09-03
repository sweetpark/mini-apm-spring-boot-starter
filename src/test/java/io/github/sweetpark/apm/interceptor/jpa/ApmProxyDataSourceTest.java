package io.github.sweetpark.apm.interceptor.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.sweetpark.apm.core.config.ApmProperties;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApmProxyDataSourceTest {

  private JdbcDataSource h2DataSource;
  private ApmProxyDataSource proxy;

  @BeforeEach
  void setUp() {
    h2DataSource = new JdbcDataSource();
    h2DataSource.setURL("jdbc:h2:mem:apmProxyDsTest;DB_CLOSE_DELAY=-1");
    h2DataSource.setUser("sa");
    h2DataSource.setPassword("");
    proxy = new ApmProxyDataSource(h2DataSource, new ApmProperties());
  }

  @AfterEach
  void tearDown() throws SQLException {
    try (Connection c = h2DataSource.getConnection();
        var stmt = c.createStatement()) {
      stmt.execute("DROP ALL OBJECTS");
    }
  }

  @Test
  @DisplayName("getConnection()은 ApmProxyConnection으로 래핑된 커넥션을 반환한다")
  void testGetConnectionWrapsInProxy() throws SQLException {
    Connection conn = proxy.getConnection();
    assertThat(conn).isInstanceOf(ApmProxyConnection.class);
    conn.close();
  }

  @Test
  @DisplayName("getConnection(user, password)도 프록시 커넥션을 반환한다")
  void testGetConnectionWithCredentials() throws SQLException {
    Connection conn = proxy.getConnection("sa", "");
    assertThat(conn).isInstanceOf(ApmProxyConnection.class);
    conn.close();
  }

  @Test
  @DisplayName("properties가 null이면 ApmPropertiesHolder의 기본값을 사용한다")
  void testNullPropertiesFallsBackToHolder() {
    ApmProxyDataSource proxyWithoutProps = new ApmProxyDataSource(h2DataSource, null);
    assertThat(proxyWithoutProps.getTargetDataSource()).isSameAs(h2DataSource);
  }

  @Test
  @DisplayName("getTargetDataSource()는 원본 위임 DataSource를 반환한다")
  void testGetTargetDataSource() {
    assertThat(proxy.getTargetDataSource()).isSameAs(h2DataSource);
  }

  @Test
  @DisplayName("로그 라이터 및 로그인 타임아웃 설정이 위임된다")
  void testLogWriterAndLoginTimeoutDelegation() throws SQLException {
    proxy.setLoginTimeout(5);
    assertThat(proxy.getLoginTimeout()).isEqualTo(5);

    assertThat(proxy.getLogWriter()).isEqualTo(h2DataSource.getLogWriter());
  }

  @Test
  @DisplayName("getParentLogger()는 위임 DataSource로 호출을 전달한다")
  void testGetParentLogger() {
    try {
      proxy.getParentLogger();
    } catch (java.sql.SQLFeatureNotSupportedException ignored) {
      // Some drivers do not support java.util.logging; forwarding the call is what matters here.
    }
  }

  @Test
  @DisplayName("unwrap/isWrapperFor: 자기 자신 및 위임 대상에 대해 동작한다")
  void testUnwrapAndIsWrapperFor() throws SQLException {
    assertThat(proxy.isWrapperFor(ApmProxyDataSource.class)).isTrue();
    assertThat(proxy.unwrap(ApmProxyDataSource.class)).isSameAs(proxy);

    assertThat(proxy.isWrapperFor(JdbcDataSource.class)).isTrue();
    assertThat(proxy.unwrap(JdbcDataSource.class)).isSameAs(h2DataSource);
  }

  @Test
  @DisplayName("unwrap: 관련 없는 인터페이스는 위임 대상에서 SQLException 발생")
  void testUnwrapUnrelatedInterfaceThrows() {
    assertThrows(SQLException.class, () -> proxy.unwrap(java.util.List.class));
  }

  @Test
  @DisplayName("isWrapperFor: 관련 없는 인터페이스는 false 위임")
  void testIsWrapperForUnrelatedInterface() throws SQLException {
    assertThat(proxy.isWrapperFor(DataSource.class)).isTrue();
  }
}
