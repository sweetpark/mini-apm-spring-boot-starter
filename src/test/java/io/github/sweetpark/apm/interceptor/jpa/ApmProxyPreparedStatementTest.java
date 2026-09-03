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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Calendar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ApmProxyPreparedStatementTest {

  private Connection rawConnection;
  private ApmProperties properties;
  private ListAppender<ILoggingEvent> appender;
  private Logger apmLogger;

  @BeforeEach
  void setUp() throws SQLException {
    rawConnection = DriverManager.getConnection("jdbc:h2:mem:apmProxyPsTest;DB_CLOSE_DELAY=-1");
    try (Statement s = rawConnection.createStatement()) {
      s.execute("DROP ALL OBJECTS");
      s.execute(
          "CREATE TABLE apm_test (id INT PRIMARY KEY, name VARCHAR(100), amount DECIMAL(10,2))");
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

  private ApmProxyPreparedStatement prepare(String sql) throws SQLException {
    return new ApmProxyPreparedStatement(rawConnection.prepareStatement(sql), sql, properties);
  }

  @Test
  @DisplayName("바인딩 파라미터가 SQL 이력에 포함되어 기록된다")
  void testBoundParametersAreTracked() throws SQLException {
    ApmProxyPreparedStatement ps =
        prepare("INSERT INTO apm_test (id, name, amount) VALUES (?, ?, ?)");
    ps.setInt(1, 1);
    ps.setString(2, "Alice");
    ps.setBigDecimal(3, new java.math.BigDecimal("9.99"));

    ps.executeUpdate();

    SqlTraceContext ctx = SqlTraceContextHolder.get();
    assertThat(ctx.getSqlList()).hasSize(1);
    assertThat(ctx.getSqlList().get(0).getSql()).contains("Alice").contains("9.99");
  }

  @Test
  @DisplayName("MyBatis가 활성 상태이면 JDBC 프록시는 중복 기록을 생략한다 (Smart De-duplication)")
  void testSkipsTrackingWhenMyBatisActive() throws SQLException {
    SqlTraceContextHolder.setMyBatisActive(true);
    ApmProxyPreparedStatement ps = prepare("INSERT INTO apm_test (id, name) VALUES (?, ?)");
    ps.setInt(1, 2);
    ps.setString(2, "Bob");

    ps.executeUpdate();

    assertThat(SqlTraceContextHolder.get().getSqlList()).isEmpty();
  }

  @Test
  @DisplayName("동일 SQL이 임계치만큼 반복되면 N+1 경고를 로깅한다")
  void testN1QueryDetection() throws SQLException {
    properties.getLimit().setN1DetectionThreshold(3);

    for (int i = 0; i < 3; i++) {
      ApmProxyPreparedStatement ps = prepare("SELECT * FROM apm_test WHERE id = ?");
      ps.setInt(1, i);
      ps.executeQuery();
    }

    assertThat(appender.list)
        .anyMatch(e -> e.getFormattedMessage().contains("possible N+1 detected"));
  }

  @Test
  @DisplayName("실패한 SQL은 에러로 기록되고 예외가 전파된다")
  void testFailedExecutionMarkedAsError() throws SQLException {
    ApmProxyPreparedStatement insertPs = prepare("INSERT INTO apm_test (id, name) VALUES (?, ?)");
    insertPs.setInt(1, 500);
    insertPs.setString(2, "dup");
    insertPs.executeUpdate();

    // Duplicate primary key: fails only at execute time, not at prepare time.
    ApmProxyPreparedStatement duplicatePs =
        prepare("INSERT INTO apm_test (id, name) VALUES (?, ?)");
    duplicatePs.setInt(1, 500);
    duplicatePs.setString(2, "dup2");

    assertThrows(SQLException.class, duplicatePs::executeUpdate);

    assertThat(SqlTraceContextHolder.get().getSqlList()).anyMatch(sql -> sql.isError());
  }

  @Test
  @DisplayName("컨텍스트가 가득 차면 초과분은 omitted로 처리된다")
  void testOmittedWhenContextFull() throws SQLException {
    properties.getLimit().setMaxSqlCount(1);

    ApmProxyPreparedStatement ps1 = prepare("INSERT INTO apm_test (id, name) VALUES (?, ?)");
    ps1.setInt(1, 10);
    ps1.setString(2, "x");
    ps1.executeUpdate();

    ApmProxyPreparedStatement ps2 = prepare("INSERT INTO apm_test (id, name) VALUES (?, ?)");
    ps2.setInt(1, 11);
    ps2.setString(2, "y");
    ps2.executeUpdate();

    assertThat(SqlTraceContextHolder.get().getOmittedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("clearParameters() 이후 파라미터 이력이 초기화된다")
  void testClearParameters() throws SQLException {
    ApmProxyPreparedStatement ps = prepare("SELECT * FROM apm_test WHERE id = ?");
    ps.setInt(1, 99);
    ps.clearParameters();
    ps.setInt(1, 1);

    ps.executeQuery();

    assertThat(SqlTraceContextHolder.get().getSqlList().get(0).getSql()).doesNotContain("99");
  }

  @Test
  @DisplayName("다양한 타입의 setter 및 위임 메서드 전반 스모크 테스트")
  void testSetterAndDelegateMethodsSmoke() throws SQLException {
    ApmProxyPreparedStatement ps =
        prepare("INSERT INTO apm_test (id, name, amount) VALUES (?, ?, ?)");

    ps.setNull(1, Types.INTEGER);
    ps.setBoolean(1, true);
    ps.setByte(1, (byte) 1);
    ps.setShort(1, (short) 1);
    ps.setInt(1, 1);
    ps.setLong(1, 1L);
    ps.setFloat(1, 1.1f);
    ps.setDouble(1, 1.1);
    ps.setBigDecimal(1, java.math.BigDecimal.ONE);
    ps.setString(2, "s");
    ps.setBytes(1, new byte[] {1, 2});
    ps.setDate(1, new java.sql.Date(0));
    ps.setTime(1, new java.sql.Time(0));
    ps.setTimestamp(1, new java.sql.Timestamp(0));
    ps.setObject(1, "obj");
    ps.setObject(1, "obj", Types.VARCHAR);
    ps.setObject(1, "obj", Types.VARCHAR, 0);
    ps.setNull(1, Types.INTEGER, "INTEGER");
    runSafely(() -> ps.setURL(1, getSafely(() -> new java.net.URL("https://example.com"))));
    runSafely(() -> ps.setRowId(1, null));
    ps.setNString(2, "nstr");
    ps.setAsciiStream(1, inputStream(), 3);
    runSafely(() -> ps.setUnicodeStream(1, inputStream(), 3));
    ps.setBinaryStream(1, inputStream(), 3);
    ps.setCharacterStream(1, reader(), 3);
    runSafely(() -> ps.setRef(1, null));
    runSafely(() -> ps.setBlob(1, (java.sql.Blob) null));
    runSafely(() -> ps.setClob(1, (java.sql.Clob) null));
    runSafely(() -> ps.setArray(1, null));
    ps.setDate(1, new java.sql.Date(0), Calendar.getInstance());
    ps.setTime(1, new java.sql.Time(0), Calendar.getInstance());
    ps.setTimestamp(1, new java.sql.Timestamp(0), Calendar.getInstance());
    ps.setNCharacterStream(1, reader(), 3);
    runSafely(() -> ps.setNClob(1, (java.sql.NClob) null));
    ps.setClob(1, reader(), 3);
    ps.setBlob(1, inputStream(), 3);
    ps.setNClob(1, reader(), 3);
    runSafely(() -> ps.setSQLXML(1, null));
    ps.setAsciiStream(1, inputStream(), 3L);
    ps.setBinaryStream(1, inputStream(), 3L);
    ps.setCharacterStream(1, reader(), 3L);
    ps.setAsciiStream(1, inputStream());
    ps.setBinaryStream(1, inputStream());
    ps.setCharacterStream(1, reader());
    ps.setNCharacterStream(1, reader());
    ps.setClob(1, reader());
    ps.setBlob(1, inputStream());
    ps.setNClob(1, reader());

    assertThat(ps.isClosed()).isFalse();
    assertThat(ps.getConnection()).isSameAs(rawConnection);
    ps.getResultSet();
    ps.getUpdateCount();
    ps.getMoreResults();
    ps.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
    runSafely((ThrowingRunnable) ps::getGeneratedKeys);
    ps.cancel();
    org.junit.jupiter.api.Assertions.assertNull(ps.getWarnings());
    ps.clearWarnings();
    runSafely(() -> ps.setCursorName("cur"));
    assertThat(ps.getMaxFieldSize()).isGreaterThanOrEqualTo(0);
    ps.setMaxFieldSize(0);
    assertThat(ps.getMaxRows()).isGreaterThanOrEqualTo(0);
    ps.setMaxRows(0);
    ps.setEscapeProcessing(true);
    assertThat(ps.getQueryTimeout()).isGreaterThanOrEqualTo(0);
    ps.setQueryTimeout(0);
    ps.setFetchDirection(java.sql.ResultSet.FETCH_FORWARD);
    ps.getFetchDirection();
    ps.setFetchSize(10);
    ps.getFetchSize();
    ps.getResultSetConcurrency();
    ps.getResultSetType();
    runSafely((ThrowingRunnable) ps::addBatch);
    runSafely((ThrowingRunnable) () -> ps.addBatch("INSERT INTO apm_test (id) VALUES (999)"));
    ps.clearBatch();
    ps.getMetaData();
    runSafely((ThrowingRunnable) ps::getParameterMetaData);
    runSafely(() -> ps.setPoolable(true));
    runSafely((ThrowingRunnable) ps::isPoolable);
    runSafely((ThrowingRunnable) ps::closeOnCompletion);
    runSafely((ThrowingRunnable) ps::isCloseOnCompletion);
    ps.getResultSetHoldability();

    assertThat(ps.isWrapperFor(ApmProxyPreparedStatement.class)).isTrue();
    assertThat(ps.unwrap(ApmProxyPreparedStatement.class)).isSameAs(ps);
    assertThat(ps.isWrapperFor(PreparedStatement.class)).isTrue();
    assertThat(ps.unwrap(PreparedStatement.class)).isNotNull();

    // Statement-inherited String-argument overloads.
    ApmProxyPreparedStatement plain = prepare("SELECT 1");
    runSafely((ThrowingRunnable) () -> plain.executeQuery("SELECT 1"));
    runSafely((ThrowingRunnable) () -> plain.executeUpdate("UPDATE apm_test SET name='z'"));
    runSafely((ThrowingRunnable) () -> plain.execute("SELECT 1"));
    runSafely(
        (ThrowingRunnable)
            () -> plain.executeUpdate("UPDATE apm_test SET name='z'", Statement.NO_GENERATED_KEYS));
    runSafely(
        (ThrowingRunnable) () -> plain.executeUpdate("UPDATE apm_test SET name='z'", new int[0]));
    runSafely(
        (ThrowingRunnable)
            () -> plain.executeUpdate("UPDATE apm_test SET name='z'", new String[0]));
    runSafely(
        (ThrowingRunnable)
            () -> plain.execute("UPDATE apm_test SET name='z'", Statement.NO_GENERATED_KEYS));
    runSafely((ThrowingRunnable) () -> plain.execute("UPDATE apm_test SET name='z'", new int[0]));
    runSafely(
        (ThrowingRunnable) () -> plain.execute("UPDATE apm_test SET name='z'", new String[0]));

    ps.close();
    plain.close();
  }

  @Test
  @DisplayName("execute()/executeLargeUpdate() 실행 경로 검증")
  void testExecuteAndExecuteLargeUpdate() throws SQLException {
    ApmProxyPreparedStatement insertPs = prepare("INSERT INTO apm_test (id, name) VALUES (?, ?)");
    insertPs.setInt(1, 50);
    insertPs.setString(2, "e");
    assertThat(insertPs.execute()).isFalse();

    ApmProxyPreparedStatement updatePs = prepare("UPDATE apm_test SET name = ? WHERE id = ?");
    updatePs.setString(1, "e2");
    updatePs.setInt(2, 50);
    assertThat(updatePs.executeLargeUpdate()).isEqualTo(1L);
  }

  private static java.io.InputStream inputStream() {
    return new java.io.ByteArrayInputStream(new byte[] {1, 2, 3});
  }

  private static java.io.Reader reader() {
    return new java.io.StringReader("abc");
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  private static void runSafely(ThrowingRunnable runnable) {
    try {
      runnable.run();
    } catch (Exception ignored) {
      // Best-effort smoke invocation; not every optional JDBC feature is
      // supported by the H2 driver, which is outside this test's concern.
    }
  }

  private static <T> T getSafely(ThrowingSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (Exception ignored) {
      return null;
    }
  }
}
