package io.github.sweetpark.apm.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.sql.SqlTraceContext;
import io.github.sweetpark.apm.core.support.util.CommonUtil;
import io.github.sweetpark.apm.core.support.util.LogMessageBuilder;
import io.github.sweetpark.apm.core.support.util.SQLUtil;
import io.github.sweetpark.apm.core.support.util.SamplingDecider;
import io.github.sweetpark.apm.core.support.util.TraceIdUtil;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoreUtilitiesTest {

  @Test
  @DisplayName("TraceId 및 SpanId 규격 검증")
  void testTraceIdUtil() {
    String traceId = TraceIdUtil.generateTraceId();
    String spanId = TraceIdUtil.generateSpanId();

    assertThat(traceId).hasSize(32).matches("^[a-f0-9]{32}$");
    assertThat(spanId).hasSize(16).matches("^[a-f0-9]{16}$");
  }

  @Test
  @DisplayName("CommonUtil truncate 검증")
  void testCommonUtil() {
    assertThat(CommonUtil.truncate("Hello World", 5)).isEqualTo("Hello...(TRUNCATED)");
    assertThat(CommonUtil.truncate("Hi", 5)).isEqualTo("Hi");
    assertThat(CommonUtil.truncate(null, 5)).isNull();
  }

  @Test
  @DisplayName("SQLUtil normalize 및 파라미터 치환 검증")
  void testSQLUtil() {
    String raw = "SELECT   *   FROM\nusers WHERE id = ? AND name = ?";
    String built = SQLUtil.buildSqlWithParams(raw, Map.of(1, 100L, 2, "Alice"), 1000);
    assertThat(built).isEqualTo("SELECT * FROM users WHERE id = 100 AND name = 'Alice'");

    assertThat(SQLUtil.formatValue(null)).isEqualTo("NULL");
    assertThat(SQLUtil.formatValue(true)).isEqualTo("true");
    assertThat(SQLUtil.formatValue("O'Reilly")).isEqualTo("'O''Reilly'");
  }

  @Test
  @DisplayName("SamplingDecider 동작 검증")
  void testSamplingDecider() {
    ApmProperties props = new ApmProperties();
    assertThat(SamplingDecider.shouldForceTrace(props, true)).isTrue();
    assertThat(SamplingDecider.shouldForceTrace(props, false)).isFalse();
  }

  @Test
  @DisplayName("SqlTraceContext 한도 및 Omitted 통계 검증")
  void testSqlTraceContext() {
    SqlTraceContext ctx = new SqlTraceContext();
    ctx.add("Q1", "SELECT 1", null, 10, false, true);
    ctx.add("Q2", "SELECT 2", null, 20, true, true);

    assertThat(ctx.count()).isEqualTo(2);
    assertThat(ctx.getTotalElapsed()).isEqualTo(30);

    ctx.addOmitted();
    assertThat(ctx.count()).isEqualTo(3);
    assertThat(ctx.getOmittedCount()).isEqualTo(1);

    int callCount = ctx.incrementCallCount("Q1");
    assertThat(callCount).isEqualTo(1);
    assertThat(ctx.getCallCount("Q1")).isEqualTo(1);
  }

  @Test
  @DisplayName("LogMessageBuilder 포맷팅 검증")
  void testLogMessageBuilder() {
    String sqlMsg =
        LogMessageBuilder.buildSql(LogMarker.SQL, "t1", "s1", "Q1", 15, "SELECT 1", "{id=1}");
    assertThat(sqlMsg).contains("trace_id=t1").contains("elapsed=15ms");

    String totalSlow = LogMessageBuilder.buildTotalSlow("t1", "s1", 1500, 1000);
    assertThat(totalSlow).contains("[TOTAL_SQL_SLOW]");

    String omitted = LogMessageBuilder.buildSqlOmitted("t1", "s1", 5);
    assertThat(omitted).contains("[SQL_LIMIT_EXCEEDED]");
  }
}
