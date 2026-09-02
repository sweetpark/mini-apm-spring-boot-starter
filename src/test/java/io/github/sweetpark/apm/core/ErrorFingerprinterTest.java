package io.github.sweetpark.apm.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import io.github.sweetpark.apm.core.error.ErrorFingerprinter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorFingerprinterTest {

  @BeforeEach
  void setUp() {
    ApmProperties props = new ApmProperties();
    props.getError().setAppPackagePrefixes(List.of("io.github.sweetpark"));
    ApmPropertiesHolder.setProperties(props);
  }

  @Test
  @DisplayName("예외 객체로부터 12자리 16진수 지문 생성 검증")
  void testFingerprintGeneration() {
    Exception ex =
        new RuntimeException("Test Exception", new IllegalArgumentException("Root cause"));
    String fp1 = ErrorFingerprinter.fingerprint(ex);

    assertThat(fp1).isNotNull().hasSize(12).matches("^[a-f0-9]{12}$");

    // 동일한 스택 및 원인의 예외는 동일한 fingerprint 생성
    String fp2 = ErrorFingerprinter.fingerprint(ex);
    assertThat(fp1).isEqualTo(fp2);
  }

  @Test
  @DisplayName("null 예외 전달 시 unknown 반환 검증")
  void testNullFingerprint() {
    assertThat(ErrorFingerprinter.fingerprint(null)).isEqualTo("unknown");
  }
}
