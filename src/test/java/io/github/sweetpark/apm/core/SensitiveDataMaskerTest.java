package io.github.sweetpark.apm.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.support.util.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

  @Test
  @DisplayName("카드번호 마스킹 테스트 - 하이픈 및 연속 숫자")
  void testCardMasking() {
    assertThat(SensitiveDataMasker.mask("1234-5678-9012-3456")).isEqualTo("1234-****-****-3456");
    assertThat(SensitiveDataMasker.mask("Card: 1234567890123456"))
        .isEqualTo("Card: 1234-****-****-3456");
  }

  @Test
  @DisplayName("주민등록번호 마스킹 테스트")
  void testRrnMasking() {
    assertThat(SensitiveDataMasker.mask("900101-1234567")).isEqualTo("900101-*******");
    assertThat(SensitiveDataMasker.mask("User 9001011234567")).isEqualTo("User 900101-*******");
  }

  @Test
  @DisplayName("이메일 마스킹 테스트")
  void testEmailMasking() {
    assertThat(SensitiveDataMasker.mask("contact@sweetpark.io")).isEqualTo("co***@sweetpark.io");
    assertThat(SensitiveDataMasker.mask("a@sweetpark.io")).isEqualTo("a***@sweetpark.io");
  }

  @Test
  @DisplayName("전화번호 마스킹 테스트")
  void testPhoneMasking() {
    assertThat(SensitiveDataMasker.mask("010-1234-5678")).isEqualTo("010-****-5678");
    assertThat(SensitiveDataMasker.mask("02-123-4567")).isEqualTo("02-****-4567");
  }

  @Test
  @DisplayName("maskIfEnabled 조건부 마스킹 테스트")
  void testMaskIfEnabled() {
    String raw = "010-1234-5678";
    assertThat(SensitiveDataMasker.maskIfEnabled(raw, true)).isEqualTo("010-****-5678");
    assertThat(SensitiveDataMasker.maskIfEnabled(raw, false)).isEqualTo("010-1234-5678");
    assertThat(SensitiveDataMasker.maskIfEnabled(null, true)).isNull();
    assertThat(SensitiveDataMasker.maskIfEnabled("", true)).isEmpty();
  }
}
