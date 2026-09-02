package io.github.sweetpark.apm.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.error.DefaultErrorEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultErrorEvaluatorTest {

  @Test
  @DisplayName("HTTP 상태 코드 기반 에러 판정 검증")
  void testHttpStatusEvaluation() {
    ApmProperties props = new ApmProperties();
    DefaultErrorEvaluator evaluator = new DefaultErrorEvaluator(props);

    assertThat(evaluator.isError(200, "{\"msg\":\"ok\"}", null)).isFalse();
    assertThat(evaluator.isError(400, "{\"msg\":\"bad request\"}", null)).isTrue();
    assertThat(evaluator.isError(500, "{\"msg\":\"internal error\"}", null)).isTrue();
  }

  @Test
  @DisplayName("JSON 응답 본문 내 에러 코드 (9999 등) 판정 검증")
  void testResponseBodyErrorCodeEvaluation() {
    ApmProperties props = new ApmProperties();
    DefaultErrorEvaluator evaluator = new DefaultErrorEvaluator(props);

    assertThat(evaluator.isError(200, "{\"resCode\":\"9999\",\"message\":\"fail\"}", null))
        .isTrue();
    assertThat(evaluator.isError(200, "{\"code\":\"ERROR\",\"detail\":\"fail\"}", null)).isTrue();
    assertThat(evaluator.isError(200, "{\"resCode\":\"0000\",\"message\":\"success\"}", null))
        .isFalse();
    assertThat(evaluator.isError(200, "{\"success\":false}", null)).isTrue();
  }

  @Test
  @DisplayName("예외 발생 시 무조건 에러 판정 검증")
  void testExceptionEvaluation() {
    ApmProperties props = new ApmProperties();
    DefaultErrorEvaluator evaluator = new DefaultErrorEvaluator(props);

    assertThat(evaluator.isError(200, "{}", new RuntimeException("error"))).isTrue();
  }
}
