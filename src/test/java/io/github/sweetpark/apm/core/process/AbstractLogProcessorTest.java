package io.github.sweetpark.apm.core.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.context.LogContext;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AbstractLogProcessorTest {

  static class TestLogContext extends LogContext {
    protected TestLogContext(Builder builder) {
      super(builder);
    }

    static class Builder extends LogContext.Builder<Builder> {
      @Override
      protected Builder self() {
        return this;
      }

      TestLogContext build() {
        return new TestLogContext(this);
      }
    }
  }

  static class TestLogProcessor extends AbstractLogProcessor<TestLogContext> {
    boolean throwOnLogApi = false;
    boolean logApiCalled = false;

    TestLogProcessor(ApmProperties properties) {
      super(properties);
    }

    @Override
    protected void logApi(TestLogContext ctx) {
      logApiCalled = true;
      if (throwOnLogApi) {
        throw new RuntimeException("intentional failure");
      }
    }

    void exposeLogSqlDetails(String traceId, String spanId, boolean isError) {
      logSqlDetails(traceId, spanId, resolveLevel(), isError);
    }

    void exposeLogException(TestLogContext ctx) {
      logException(ctx, ctx.getTraceId(), ctx.getSpanId());
    }

    io.github.sweetpark.apm.core.enums.TraceLevel exposeResolveLevel() {
      return resolveLevel();
    }
  }

  private ListAppender<ILoggingEvent> appender;
  private Logger apmLogger;

  @BeforeEach
  void setUp() {
    apmLogger = (Logger) LoggerFactory.getLogger("ApmLog");
    appender = new ListAppender<>();
    appender.start();
    apmLogger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    apmLogger.detachAppender(appender);
    SqlTraceContextHolder.clear();
    TraceContextHolder.clear();
  }

  @Test
  @DisplayName("process()는 정상 흐름에서 logApi를 호출한다")
  void testProcessCallsLogApi() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());
    TestLogContext ctx = new TestLogContext.Builder().traceId("t1").build();

    processor.process(ctx);

    assertThat(processor.logApiCalled).isTrue();
  }

  @Test
  @DisplayName("process()는 logApi 예외를 삼키고 전파하지 않는다 (Fail-safe)")
  void testProcessFailSafe() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());
    processor.throwOnLogApi = true;
    TestLogContext ctx = new TestLogContext.Builder().traceId("t2").build();

    assertDoesNotThrow(() -> processor.process(ctx));

    assertThat(appender.list)
        .anyMatch(event -> event.getFormattedMessage().contains("[APM_INTERNAL_ERROR]"));
  }

  @Test
  @DisplayName("process()는 ctx가 null이어도 예외 없이 처리한다")
  void testProcessHandlesNullContext() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());
    processor.throwOnLogApi = true;

    assertDoesNotThrow(() -> processor.process(null));
  }

  @Test
  @DisplayName("resolveLevel()은 TraceContextHolder 상태를 반영한다")
  void testResolveLevel() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());

    TraceContextHolder.clear();
    assertThat(processor.exposeResolveLevel())
        .isEqualTo(io.github.sweetpark.apm.core.enums.TraceLevel.PROD);

    TraceContextHolder.init("t", "s", io.github.sweetpark.apm.core.enums.TraceLevel.TRACE, false);
    assertThat(processor.exposeResolveLevel())
        .isEqualTo(io.github.sweetpark.apm.core.enums.TraceLevel.TRACE);
  }

  @Test
  @DisplayName("logSqlDetails: ALWAYS 모드는 모든 SQL을 로깅한다")
  void testLogSqlDetailsAlwaysMode() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setSql(ApmProperties.CaptureMode.ALWAYS);
    TestLogProcessor processor = new TestLogProcessor(props);

    SqlTraceContextHolder.init().add("sql1", "SELECT 1", "p=1", 5L, false, true);

    processor.exposeLogSqlDetails("t3", "s3", false);

    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("sql_id=sql1"));
  }

  @Test
  @DisplayName("logSqlDetails: ERROR 모드는 에러 SQL만 로깅한다")
  void testLogSqlDetailsErrorMode() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setSql(ApmProperties.CaptureMode.ERROR);
    TestLogProcessor processor = new TestLogProcessor(props);

    SqlTraceContextHolder.init().add("sqlErr", "SELECT ERR", null, 5L, true, true);

    processor.exposeLogSqlDetails("t4", "s4", false);

    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("sql_id=sqlErr"));
  }

  @Test
  @DisplayName("logSqlDetails: SLOW 모드에서 단건 슬로우 쿼리 및 누적 슬로우 로깅")
  void testLogSqlDetailsSlowMode() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setSql(ApmProperties.CaptureMode.SLOW);
    props.getSlow().getQuery().setMs(10);
    props.getSlow().getQuery().setTotalMs(10);
    TestLogProcessor processor = new TestLogProcessor(props);

    SqlTraceContextHolder.init().add("slowSql", "SELECT SLOW", null, 999L, false, true);

    processor.exposeLogSqlDetails("t5", "s5", false);

    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("total_sql_elapsed"));
    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("sql_id=slowSql"));
  }

  @Test
  @DisplayName("logSqlDetails: SAMPLE 모드는 TRACE 레벨에서만 로깅한다")
  void testLogSqlDetailsSampleMode() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setSql(ApmProperties.CaptureMode.SAMPLE);
    TestLogProcessor processor = new TestLogProcessor(props);

    SqlTraceContextHolder.init().add("sampleSql", "SELECT SAMPLE", null, 1L, false, true);
    TraceContextHolder.init("t6", "s6", io.github.sweetpark.apm.core.enums.TraceLevel.TRACE, true);

    processor.exposeLogSqlDetails("t6", "s6", false);

    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("sql_id=sampleSql"));
  }

  @Test
  @DisplayName("logSqlDetails: OFF 모드에서도 TRACE 레벨이면 폴백 로깅한다")
  void testLogSqlDetailsOffModeTraceFallback() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setSql(ApmProperties.CaptureMode.OFF);
    TestLogProcessor processor = new TestLogProcessor(props);

    SqlTraceContextHolder.init().add("fallbackSql", "SELECT FB", null, 1L, false, true);
    TraceContextHolder.init("t7", "s7", io.github.sweetpark.apm.core.enums.TraceLevel.TRACE, true);

    processor.exposeLogSqlDetails("t7", "s7", false);

    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("sql_id=fallbackSql"));
  }

  @Test
  @DisplayName("logSqlDetails: 생략된 SQL 카운트가 있으면 SQL 마커로 로깅한다")
  void testLogSqlDetailsOmitted() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setSql(ApmProperties.CaptureMode.ALWAYS);
    TestLogProcessor processor = new TestLogProcessor(props);

    var ctx = SqlTraceContextHolder.init();
    ctx.addOmitted();

    processor.exposeLogSqlDetails("t8", "s8", false);

    assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("omitted"));
  }

  @Test
  @DisplayName("logException: 예외가 없으면 로깅하지 않는다")
  void testLogExceptionSkipsWhenNoEx() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());
    TestLogContext ctx = new TestLogContext.Builder().traceId("t9").spanId("s9").build();

    processor.exposeLogException(ctx);

    assertThat(appender.list).isEmpty();
  }

  @Test
  @DisplayName("logException: DB 계열 예외는 ERROR_DB 마커로 분류된다")
  void testLogExceptionDatabaseError() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());
    TestLogContext ctx =
        new TestLogContext.Builder()
            .traceId("t10")
            .spanId("s10")
            .ex(new SQLException("db down"))
            .build();

    processor.exposeLogException(ctx);

    assertThat(appender.list)
        .anyMatch(e -> e.getFormattedMessage().contains("error_type=DB_ERROR"));
  }

  @Test
  @DisplayName("logException: 그 외 예외는 ERROR_SYSTEM 마커로 분류된다")
  void testLogExceptionSystemError() {
    TestLogProcessor processor = new TestLogProcessor(new ApmProperties());
    TestLogContext ctx =
        new TestLogContext.Builder()
            .traceId("t11")
            .spanId("s11")
            .ex(new RuntimeException("unexpected"))
            .build();

    processor.exposeLogException(ctx);

    assertThat(appender.list)
        .anyMatch(e -> e.getFormattedMessage().contains("error_type=SYSTEM_ERROR"));
  }
}
