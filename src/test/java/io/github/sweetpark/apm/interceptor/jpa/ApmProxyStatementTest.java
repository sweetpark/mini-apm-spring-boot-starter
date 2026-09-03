package io.github.sweetpark.apm.interceptor.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ApmProxyStatementTest {

  private Connection rawConnection;
  private ApmProperties properties;
  private ListAppender<ILoggingEvent> appender;
  private Logger apmLogger;

  @BeforeEach
  void setUp() throws SQLException {
    rawConnection = DriverManager.getConnection("jdbc:h2:mem:apmProxyStmtTest;DB_CLOSE_DELAY=-1");
    try (Statement s = rawConnection.createStatement()) {
      s.execute("DROP ALL OBJECTS");
      s.execute("CREATE TABLE apm_test (id INT PRIMARY KEY, name VARCHAR(100))");
    }
    properties = new ApmProperties();
    SqlTraceContextHolder.init();
    SqlTraceContextHolder.setMyBatisActive(false);

    apmLogger = (Logger) LoggerFactory.getLogger("ApmLog");
    appender = new ListAppender<>();
    appender.start();
    apmLogger.addAppender(appender);
  }

  @AfterEach
  void tearDown() throws SQLException {
    apmLogger.detachAppender(appender);
    SqlTraceContextHolder.clear();
    rawConnection.close();
  }

  @Test
  @DisplayName("정상 실행 시 SqlTraceContext에 SQL 이력이 기록된다")
  void testExecuteUpdateTracksSql() throws SQLException {
    ApmProxyStatement stmt = new ApmProxyStatement(rawConnection.createStatement(), properties);

    int updated = stmt.executeUpdate("INSERT INTO apm_test (id, name) VALUES (1, 'a')");

    assertThat(updated).isEqualTo(1);
    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getSqlList()).hasSize(1);
    assertThat(ctx.getSqlList().get(0).isError()).isFalse();
  }

  @Test
  @DisplayName("MyBatis가 활성 상태이면 JDBC 프록시는 중복 기록을 생략한다 (Smart De-duplication)")
  void testSkipsTrackingWhenMyBatisActive() throws SQLException {
    SqlTraceContextHolder.setMyBatisActive(true);
    ApmProxyStatement stmt = new ApmProxyStatement(rawConnection.createStatement(), properties);

    stmt.executeUpdate("INSERT INTO apm_test (id, name) VALUES (2, 'b')");

    assertThat(SqlTraceContextHolder.get().getSqlList()).isEmpty();
  }

  @Test
  @DisplayName("동일 SQL이 임계치만큼 반복되면 N+1 경고를 로깅한다")
  void testN1QueryDetection() throws SQLException {
    properties.getLimit().setN1DetectionThreshold(3);

    for (int i = 0; i < 3; i++) {
      ApmProxyStatement stmt = new ApmProxyStatement(rawConnection.createStatement(), properties);
      stmt.executeQuery("SELECT * FROM apm_test WHERE id = " + i);
    }

    assertThat(appender.list)
        .anyMatch(e -> e.getFormattedMessage().contains("possible N+1 detected"));
  }

  @Test
  @DisplayName("실패한 SQL은 에러로 기록되고 예외가 전파된다")
  void testFailedExecutionMarkedAsError() throws SQLException {
    ApmProxyStatement stmt = new ApmProxyStatement(rawConnection.createStatement(), properties);

    assertThrows(
        SQLException.class, () -> stmt.executeUpdate("INSERT INTO not_a_table VALUES (1)"));

    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getSqlList()).anyMatch(sql -> sql.isError());
  }

  @Test
  @DisplayName("컨텍스트가 가득 차면 초과분은 omitted로 처리된다")
  void testOmittedWhenContextFull() throws SQLException {
    properties.getLimit().setMaxSqlCount(1);

    ApmProxyStatement stmt1 = new ApmProxyStatement(rawConnection.createStatement(), properties);
    stmt1.executeUpdate("INSERT INTO apm_test (id, name) VALUES (10, 'x')");

    ApmProxyStatement stmt2 = new ApmProxyStatement(rawConnection.createStatement(), properties);
    stmt2.executeUpdate("INSERT INTO apm_test (id, name) VALUES (11, 'y')");

    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getOmittedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("executeBatch는 BATCH_QUERY sqlId로 추적된다")
  void testExecuteBatchTracked() throws SQLException {
    Statement raw = rawConnection.createStatement();
    ApmProxyStatement stmt = new ApmProxyStatement(raw, properties);
    stmt.addBatch("INSERT INTO apm_test (id, name) VALUES (20, 'batch1')");
    stmt.addBatch("INSERT INTO apm_test (id, name) VALUES (21, 'batch2')");

    stmt.executeBatch();

    assertThat(SqlTraceContextHolder.get().getSqlList())
        .anyMatch(sql -> "BATCH_QUERY".equals(sql.getSqlId()));
  }

  @Test
  @DisplayName("execute()/executeQuery()/executeUpdate() 오버로드 및 위임 메서드 전반 스모크 테스트")
  void testDelegateMethodsSmoke() throws SQLException {
    Statement raw = rawConnection.createStatement();
    ApmProxyStatement stmt = new ApmProxyStatement(raw, properties);

    stmt.execute("INSERT INTO apm_test (id, name) VALUES (30, 'c')");
    stmt.executeUpdate(
        "UPDATE apm_test SET name = 'c2' WHERE id = 30", Statement.NO_GENERATED_KEYS);
    stmt.executeUpdate("UPDATE apm_test SET name = 'c3' WHERE id = 30", new int[0]);
    stmt.executeUpdate("UPDATE apm_test SET name = 'c4' WHERE id = 30", new String[0]);
    stmt.execute("UPDATE apm_test SET name = 'c5' WHERE id = 30", Statement.NO_GENERATED_KEYS);
    stmt.execute("UPDATE apm_test SET name = 'c6' WHERE id = 30", new int[0]);
    stmt.execute("UPDATE apm_test SET name = 'c7' WHERE id = 30", new String[0]);

    assertThat(stmt.getMaxFieldSize()).isGreaterThanOrEqualTo(0);
    stmt.setMaxFieldSize(0);
    assertThat(stmt.getMaxRows()).isGreaterThanOrEqualTo(0);
    stmt.setMaxRows(0);
    stmt.setEscapeProcessing(true);
    assertThat(stmt.getQueryTimeout()).isGreaterThanOrEqualTo(0);
    stmt.setQueryTimeout(0);
    org.junit.jupiter.api.Assertions.assertNull(stmt.getWarnings());
    stmt.clearWarnings();
    runSafely(() -> stmt.setCursorName("cur1"));
    stmt.getResultSet();
    stmt.getUpdateCount();
    stmt.getMoreResults();
    stmt.setFetchDirection(java.sql.ResultSet.FETCH_FORWARD);
    assertThat(stmt.getFetchDirection()).isEqualTo(java.sql.ResultSet.FETCH_FORWARD);
    stmt.setFetchSize(10);
    assertThat(stmt.getFetchSize()).isEqualTo(10);
    stmt.getResultSetConcurrency();
    stmt.getResultSetType();
    stmt.clearBatch();
    stmt.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
    runSafely(stmt::getGeneratedKeys);
    stmt.getResultSetHoldability();
    assertThat(stmt.isClosed()).isFalse();
    runSafely(() -> stmt.setPoolable(true));
    runSafely(stmt::isPoolable);
    runSafely(stmt::closeOnCompletion);
    runSafely(stmt::isCloseOnCompletion);

    assertThat(stmt.isWrapperFor(ApmProxyStatement.class)).isTrue();
    assertThat(stmt.unwrap(ApmProxyStatement.class)).isSameAs(stmt);
    assertThat(stmt.isWrapperFor(Statement.class)).isTrue();
    assertThat(stmt.unwrap(Statement.class)).isNotNull();
    assertThat(stmt.getConnection()).isSameAs(rawConnection);

    stmt.close();
  }

  @Test
  @DisplayName("컨텍스트가 가득 찬 상태에서 에러가 발생하면 가장 오래된 정상 항목을 밀어내고 에러를 기록한다")
  void testRemoveOldestNormalWhenFullAndError() throws SQLException {
    properties.getLimit().setMaxSqlCount(1);

    ApmProxyStatement ok = new ApmProxyStatement(rawConnection.createStatement(), properties);
    ok.executeUpdate("INSERT INTO apm_test (id, name) VALUES (40, 'ok')");

    ApmProxyStatement failing = new ApmProxyStatement(rawConnection.createStatement(), properties);
    assertThrows(
        SQLException.class, () -> failing.executeUpdate("INSERT INTO no_such_table VALUES (1)"));

    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getOmittedCount()).isZero();
    assertThat(ctx.getSqlList()).hasSize(1);
    assertThat(ctx.getSqlList().get(0).isError()).isTrue();
  }

  @Test
  @DisplayName("빈 SQL 문자열은 JDBC:QUERY sqlId로 기록된다")
  void testBlankSqlUsesFallbackSqlId() throws SQLException {
    ApmProxyStatement stmt = new ApmProxyStatement(rawConnection.createStatement(), properties);

    try {
      stmt.execute("   ");
    } catch (SQLException ignored) {
      // Whether the driver accepts or rejects a blank statement, trackExecution
      // still runs in the finally block and must fall back to a placeholder sqlId.
    }

    assertThat(SqlTraceContextHolder.get().getSqlList())
        .anyMatch(sql -> "JDBC:QUERY".equals(sql.getSqlId()));
  }

  @Test
  @DisplayName("executeQuery/execute 및 executeUpdate/execute 오버로드의 예외 경로를 검증한다")
  void testFailurePathsAcrossOverloads() throws SQLException {
    Statement raw = rawConnection.createStatement();
    ApmProxyStatement stmt = new ApmProxyStatement(raw, properties);

    assertThrows(SQLException.class, () -> stmt.executeQuery("SELECT * FROM no_such_table"));
    assertThrows(SQLException.class, () -> stmt.execute("SELECT * FROM no_such_table"));
    assertThrows(
        SQLException.class, () -> stmt.executeUpdate("BOGUS SQL", Statement.NO_GENERATED_KEYS));
    assertThrows(SQLException.class, () -> stmt.executeUpdate("BOGUS SQL", new int[0]));
    assertThrows(SQLException.class, () -> stmt.executeUpdate("BOGUS SQL", new String[0]));
    assertThrows(SQLException.class, () -> stmt.execute("BOGUS SQL", Statement.NO_GENERATED_KEYS));
    assertThrows(SQLException.class, () -> stmt.execute("BOGUS SQL", new int[0]));
    assertThrows(SQLException.class, () -> stmt.execute("BOGUS SQL", new String[0]));

    Statement batchRaw = rawConnection.createStatement();
    ApmProxyStatement batchStmt = new ApmProxyStatement(batchRaw, properties);
    batchStmt.addBatch("BOGUS SQL 1");
    batchStmt.addBatch("BOGUS SQL 2");
    assertThrows(SQLException.class, batchStmt::executeBatch);
  }

  @Test
  @DisplayName("cancel() 및 unwrap의 위임 전용/무관 인터페이스 분기를 검증한다")
  void testCancelAndUnwrapDelegateOnlyBranch() throws SQLException {
    Statement raw = rawConnection.createStatement();
    ApmProxyStatement stmt = new ApmProxyStatement(raw, properties);

    runSafely(stmt::cancel);

    // A driver-specific interface that only the delegate implements, not the proxy.
    assertThat(stmt.isWrapperFor(org.h2.jdbc.JdbcStatement.class)).isTrue();
    assertThat(stmt.unwrap(org.h2.jdbc.JdbcStatement.class)).isSameAs(raw);

    // An interface unrelated to both the proxy and the delegate: falls through to
    // the delegate's own unwrap(), which is expected to fail.
    assertThrows(SQLException.class, () -> stmt.unwrap(java.util.List.class));
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
