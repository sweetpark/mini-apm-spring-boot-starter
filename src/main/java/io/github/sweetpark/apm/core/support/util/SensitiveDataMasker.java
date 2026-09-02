package io.github.sweetpark.apm.core.support.util;

import java.util.regex.Pattern;

/**
 * 로그 출력 전 민감 정보를 마스킹하는 유틸리티 클래스입니다.
 *
 * <p>카드번호, 주민등록번호, 이메일, 전화번호 패턴을 마스킹 처리합니다.
 */
public final class SensitiveDataMasker {

  private SensitiveDataMasker() {}

  // 카드번호: 16자리 숫자 (붙여쓰기, 하이픈, 공백 구분자 모두 처리)
  private static final Pattern CARD_NUMBER =
      Pattern.compile("\\b(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})\\b");

  // 주민등록번호: 6자리-7자리 형식
  private static final Pattern RRN = Pattern.compile("\\b(\\d{6})[\\s-]?([1-8]\\d{6})\\b");

  // 이메일: 아이디 앞 2자리 남기고 마스킹
  private static final Pattern EMAIL =
      Pattern.compile(
          "\\b([A-Za-z0-9._%+-]{1,2})[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

  // 전화번호: 010-1234-5678, 02-123-4567 등 중간 번호 마스킹
  private static final Pattern PHONE =
      Pattern.compile("\\b(01[016789]|02|0[3-9]\\d{1})[\\s-]?(\\d{3,4})[\\s-]?(\\d{4})\\b");

  public static String mask(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }

    String result = CARD_NUMBER.matcher(value).replaceAll("$1-****-****-$4");
    result = RRN.matcher(result).replaceAll("$1-*******");
    result = EMAIL.matcher(result).replaceAll("$1***$2");
    result = PHONE.matcher(result).replaceAll("$1-****-$3");

    return result;
  }

  public static String maskIfEnabled(String value, boolean enabled) {
    return enabled ? mask(value) : value;
  }
}
