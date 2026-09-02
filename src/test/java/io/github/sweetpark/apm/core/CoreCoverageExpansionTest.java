package io.github.sweetpark.apm.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.error.BreadcrumbEvent;
import io.github.sweetpark.apm.core.error.DefaultErrorEvaluator;
import io.github.sweetpark.apm.core.error.ErrorFingerprinter;
import io.github.sweetpark.apm.core.support.util.LogMessageBuilder;
import io.github.sweetpark.apm.core.support.util.SQLUtil;
import io.github.sweetpark.apm.core.support.util.SamplingDecider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoreCoverageExpansionTest {

  @Test
  @DisplayName("BreadcrumbEvent 생성 및 필드 검증")
  void testBreadcrumbEvent() {
    BreadcrumbEvent b1 = new BreadcrumbEvent("HTTP", "GET /api");
    assertThat(b1.category()).isEqualTo("HTTP");
    assertThat(b1.message()).isEqualTo("GET /api");
    assertThat(b1.timestamp()).isNotNull();

    Instant now = Instant.now();
    BreadcrumbEvent b2 = new BreadcrumbEvent(now, "SQL", "SELECT 1");
    assertThat(b2.category()).isEqualTo("SQL");
    assertThat(b2.message()).isEqualTo("SELECT 1");
    assertThat(b2.timestamp()).isEqualTo(now);
  }

  @Test
  @DisplayName("LogMessageBuilder buildError 복합 스택 및 Breadcrumbs 검증")
  void testBuildError() {
    Exception cause = new IllegalArgumentException("Bad input");
    Exception top = new RuntimeException("Top failure", cause);

    List<BreadcrumbEvent> breadcrumbs =
        List.of(new BreadcrumbEvent("DB", "Connect"), new BreadcrumbEvent("SQL", "Query user"));

    String errorMsg =
        LogMessageBuilder.buildError(
            "trace-123", "span-456", "fp-789", "BIZ_ERROR", breadcrumbs, top, 5, 3);

    assertThat(errorMsg)
        .contains("trace_id=trace-123")
        .contains("error_fingerprint=fp-789")
        .contains("error_type=BIZ_ERROR")
        .contains("breadcrumbs=[{cat:\"DB\",msg:\"Connect\"}, {cat:\"SQL\",msg:\"Query user\"}]")
        .contains("Top failure")
        .contains("Caused by: java.lang.IllegalArgumentException: Bad input");

    // null message exception
    Exception nullMsg = new Exception((String) null);
    String nullError = LogMessageBuilder.buildError(null, null, "fp", "SYS", null, nullMsg, 2, 2);
    assertThat(nullError).contains("trace_id=-").contains("message=\"Exception\"");
  }

  @Test
  @DisplayName("SamplingDecider SAMPLE 모드 브랜치 커버리지")
  void testSamplingDeciderSampleMode() {
    ApmProperties props = new ApmProperties();
    props.getCapture().setBody(ApmProperties.CaptureMode.SAMPLE);
    props.getCapture().setSampleRate(1.0); // 100%
    assertThat(SamplingDecider.shouldForceTrace(props, false)).isTrue();

    props.getCapture().setSampleRate(0.0); // 0%
    assertThat(SamplingDecider.shouldForceTrace(props, false)).isFalse();

    props.getCapture().setBody(ApmProperties.CaptureMode.ERROR);
    props.getCapture().setSql(ApmProperties.CaptureMode.SAMPLE);
    props.getCapture().setSampleRate(1.0);
    assertThat(SamplingDecider.shouldForceTrace(props, false)).isTrue();

    assertThat(SamplingDecider.shouldForceTrace(null, false)).isFalse();
  }

  @Test
  @DisplayName("SQLUtil 다양한 리터럴 및 주석/따옴표 처리 검증")
  void testSqlUtilComprehensive() {
    assertThat(SQLUtil.normalizeSql(null, 100)).isNull();
    assertThat(SQLUtil.buildSqlWithParams(null, null, 100)).isNull();
    assertThat(SQLUtil.buildSqlWithParams("SELECT 1", null, 100)).isEqualTo("SELECT 1");

    // Comments and quotes
    String sqlWithComments =
        "SELECT /* block comment */ ? FROM users WHERE name = '--not comment' AND note = '-- line"
            + " comment\n"
            + "?'";
    String formatted =
        SQLUtil.buildSqlWithParams(sqlWithComments, Map.of(1, 42, 2, "NoteVal"), 500);
    assertThat(formatted).contains("42");

    // Types in formatValue
    assertThat(SQLUtil.formatValue(LocalDateTime.of(2026, 9, 2, 12, 0))).contains("2026-09-02");
    assertThat(SQLUtil.formatValue(new Date(0))).isNotNull();
    assertThat(SQLUtil.formatValue(123.45)).isEqualTo("123.45");

    // Max length truncation
    String longSql = "SELECT ? FROM t";
    String truncated = SQLUtil.buildSqlWithParams(longSql, Map.of(1, "long-value"), 5);
    assertThat(truncated).endsWith("...(TRUNCATED)");
  }

  @Test
  @DisplayName("DefaultErrorEvaluator Array 및 Edge case 검증")
  void testDefaultErrorEvaluatorEdgeCases() {
    ApmProperties props = new ApmProperties();
    DefaultErrorEvaluator evaluator = new DefaultErrorEvaluator(props);

    assertThat(evaluator.isError(200, null, null)).isFalse();
    assertThat(evaluator.isError(200, "", null)).isFalse();
    assertThat(evaluator.isError(200, "not-json", null)).isFalse();

    // Array of errors
    String jsonArray = "[{\"resCode\":\"0000\"}, {\"code\":\"FAIL\"}]";
    assertThat(evaluator.isError(200, jsonArray, null)).isTrue();

    String normalArray = "[{\"resCode\":\"0000\"}, {\"code\":\"OK\"}]";
    assertThat(evaluator.isError(200, normalArray, null)).isFalse();
  }

  @Test
  @DisplayName("ErrorFingerprinter 프레임워크 필터링 및 원인 체인 검증")
  void testErrorFingerprinterChains() {
    Exception cause3 = new IllegalArgumentException("Root");
    Exception cause2 = new RuntimeException("Mid", cause3);
    Exception cause1 = new RuntimeException("Top", cause2);

    String fp = ErrorFingerprinter.fingerprint(cause1);
    assertThat(fp).isNotNull().hasSize(12);
  }
}
