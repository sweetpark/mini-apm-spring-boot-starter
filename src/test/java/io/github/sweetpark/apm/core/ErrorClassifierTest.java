package io.github.sweetpark.apm.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.error.ErrorClassifier;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorClassifierTest {

  @Test
  @DisplayName("예외 유형 분류 검증 (BIZ, DATABASE, EXTERNAL, SYSTEM)")
  void testErrorClassification() {
    assertThat(ErrorClassifier.classify(new IllegalArgumentException("Invalid input")))
        .isEqualTo(ErrorClassifier.ErrorType.BIZ);
    assertThat(ErrorClassifier.classify(new SQLException("Table not found")))
        .isEqualTo(ErrorClassifier.ErrorType.DATABASE);
    assertThat(ErrorClassifier.classify(new TimeoutException("Gateway timeout")))
        .isEqualTo(ErrorClassifier.ErrorType.EXTERNAL);
    assertThat(ErrorClassifier.classify(new NullPointerException("NPE")))
        .isEqualTo(ErrorClassifier.ErrorType.SYSTEM);
    assertThat(ErrorClassifier.classify(null)).isEqualTo(ErrorClassifier.ErrorType.SYSTEM);
  }

  @Test
  @DisplayName("Cause 체인을 순회하며 DATABASE 예외 식별 검증")
  void testChainedCauseClassification() {
    Exception wrapped =
        new RuntimeException("Service failure", new SQLException("Connection dead"));
    assertThat(ErrorClassifier.classify(wrapped)).isEqualTo(ErrorClassifier.ErrorType.DATABASE);
  }
}
