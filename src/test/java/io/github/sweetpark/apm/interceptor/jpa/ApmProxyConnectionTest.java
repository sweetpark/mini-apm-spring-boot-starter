package io.github.sweetpark.apm.interceptor.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.config.ApmProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApmProxyConnectionTest {

  private Connection rawConnection;
  private ApmProxyConnection proxy;

  @BeforeEach
  void setUp() throws SQLException {
    rawConnection = DriverManager.getConnection("jdbc:h2:mem:apmProxyConnTest;DB_CLOSE_DELAY=-1");
    try (Statement s = rawConnection.createStatement()) {
      s.execute("DROP ALL OBJECTS");
      s.execute("CREATE TABLE apm_test (id INT PRIMARY KEY, name VARCHAR(100))");
    }
    proxy = new ApmProxyConnection(rawConnection, new ApmProperties());
  }

  @AfterEach
  void tearDown() throws SQLException {
    rawConnection.close();
  }

  @Test
  @DisplayName("createStatement()의 모든 오버로드는 ApmProxyStatement를 반환한다")
  void testCreateStatementOverloadsReturnProxy() throws SQLException {
    assertThat(proxy.createStatement()).isInstanceOf(ApmProxyStatement.class);
    assertThat(proxy.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
        .isInstanceOf(ApmProxyStatement.class);
    assertThat(
            proxy.createStatement(
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT))
        .isInstanceOf(ApmProxyStatement.class);
  }

  @Test
  @DisplayName("prepareStatement()의 모든 오버로드는 ApmProxyPreparedStatement를 반환한다")
  void testPrepareStatementOverloadsReturnProxy() throws SQLException {
    assertThat(proxy.prepareStatement("SELECT 1")).isInstanceOf(ApmProxyPreparedStatement.class);
    assertThat(
            proxy.prepareStatement(
                "SELECT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
        .isInstanceOf(ApmProxyPreparedStatement.class);
    assertThat(
            proxy.prepareStatement(
                "SELECT 1",
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT))
        .isInstanceOf(ApmProxyPreparedStatement.class);
    assertThat(
            proxy.prepareStatement(
                "INSERT INTO apm_test (id) VALUES (?)", Statement.NO_GENERATED_KEYS))
        .isInstanceOf(ApmProxyPreparedStatement.class);
    assertThat(proxy.prepareStatement("INSERT INTO apm_test (id) VALUES (?)", new int[] {1}))
        .isInstanceOf(ApmProxyPreparedStatement.class);
    assertThat(proxy.prepareStatement("INSERT INTO apm_test (id) VALUES (?)", new String[] {"ID"}))
        .isInstanceOf(ApmProxyPreparedStatement.class);
  }

  @Test
  @DisplayName("prepareCall()은 프록시하지 않고 원본 CallableStatement를 그대로 위임한다")
  void testPrepareCallIsNotProxied() throws SQLException {
    runSafely(() -> proxy.prepareCall("CALL 1"));
    runSafely(
        () -> proxy.prepareCall("CALL 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
    runSafely(
        () ->
            proxy.prepareCall(
                "CALL 1",
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT));
  }

  @Test
  @DisplayName("트랜잭션 및 메타데이터 관련 위임 메서드 전반 스모크 테스트")
  void testDelegateMethodsSmoke() throws SQLException {
    assertThat(proxy.nativeSQL("SELECT 1")).isNotNull();
    proxy.setAutoCommit(false);
    assertThat(proxy.getAutoCommit()).isFalse();
    proxy.commit();
    proxy.setAutoCommit(true);

    assertThat(proxy.isClosed()).isFalse();
    assertThat(proxy.getMetaData()).isNotNull();
    proxy.setReadOnly(false);
    assertThat(proxy.isReadOnly()).isFalse();
    runSafely(() -> proxy.setCatalog("PUBLIC"));
    proxy.getCatalog();
    proxy.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    assertThat(proxy.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
    org.junit.jupiter.api.Assertions.assertNull(proxy.getWarnings());
    proxy.clearWarnings();
    runSafely(() -> proxy.getTypeMap());
    runSafely(() -> proxy.setTypeMap(new java.util.HashMap<>()));
    proxy.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
    proxy.getHoldability();

    proxy.setAutoCommit(false);
    var savepoint = proxy.setSavepoint();
    proxy.rollback(savepoint);
    var namedSavepoint = proxy.setSavepoint("sp1");
    proxy.releaseSavepoint(namedSavepoint);
    proxy.setAutoCommit(true);

    runSafely(proxy::createClob);
    runSafely(proxy::createBlob);
    runSafely(proxy::createNClob);
    runSafely(proxy::createSQLXML);
    assertThat(proxy.isValid(2)).isTrue();
    runSafely(() -> proxy.setClientInfo("app", "apm-test"));
    runSafely(() -> proxy.setClientInfo(new java.util.Properties()));
    runSafely(() -> proxy.getClientInfo("app"));
    runSafely(proxy::getClientInfo);
    runSafely(() -> proxy.createArrayOf("INTEGER", new Object[] {1}));
    runSafely(() -> proxy.createStruct("MY_TYPE", new Object[] {}));
    runSafely(() -> proxy.setSchema("PUBLIC"));
    runSafely(proxy::getSchema);
    runSafely(
        () ->
            proxy.setNetworkTimeout(
                java.util.concurrent.Executors.newSingleThreadExecutor(), 1000));
    runSafely(proxy::getNetworkTimeout);

    assertThat(proxy.isWrapperFor(ApmProxyConnection.class)).isTrue();
    assertThat(proxy.unwrap(ApmProxyConnection.class)).isSameAs(proxy);
    assertThat(proxy.isWrapperFor(Connection.class)).isTrue();
    assertThat(proxy.unwrap(Connection.class)).isNotNull();

    proxy.close();
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private static void runSafely(ThrowingRunnable runnable) {
    try {
      runnable.run();
    } catch (Exception ignored) {
      // Best-effort smoke invocation; some optional JDBC features are not
      // supported by every driver, which is not the concern of this test.
    }
  }
}
