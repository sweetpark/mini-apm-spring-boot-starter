package io.github.sweetpark.apm.core.support.util;

/** 문자열 자르기 및 유틸리티 메서드를 제공합니다. */
public final class CommonUtil {

  private CommonUtil() {}

  public static String truncate(String str, int maxLength) {
    if (str == null) {
      return null;
    }
    if (str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + "...(TRUNCATED)";
  }
}
