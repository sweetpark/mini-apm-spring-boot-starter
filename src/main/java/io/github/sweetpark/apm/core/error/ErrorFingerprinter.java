package io.github.sweetpark.apm.core.error;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

/**
 * 예외로부터 버그 식별용 12자리 SHA-256 지문(Fingerprint)을 생성하는 유틸리티 클래스입니다.
 *
 * <p>Grafana/Loki에서 error_fingerprint 필드로 그룹핑하여 동일 원인의 예외를 집계할 수 있습니다.
 */
public final class ErrorFingerprinter {

  private static final int MAX_CAUSE_DEPTH = 5;

  // 스택 트레이스에서 무시할 프레임워크 패키지 프리픽스 목록
  private static final Set<String> IGNORED_FRAMEWORK_PREFIXES =
      Set.of(
          "java.",
          "javax.",
          "jakarta.",
          "jdk.",
          "sun.",
          "org.springframework.",
          "org.apache.",
          "org.hibernate.",
          "org.mybatis.",
          "io.netty.",
          "com.zaxxer.hikari.",
          "io.github.sweetpark.apm.");

  private ErrorFingerprinter() {}

  /**
   * 주어진 예외로부터 12자리 16진수 지문을 생성합니다.
   *
   * @param ex 지문을 생성할 예외
   * @return 12자리 16진수 문자열, null인 경우 "unknown"
   */
  public static String fingerprint(Throwable ex) {
    if (ex == null) {
      return "unknown";
    }

    String key = buildFingerprintKey(ex);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder(12);
      for (int i = 0; i < 6; i++) {
        hex.append(String.format("%02x", hash[i]));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(key.hashCode());
    }
  }

  private static String buildFingerprintKey(Throwable ex) {
    StringBuilder key = new StringBuilder();

    // 1. 최상위 예외 클래스명
    key.append(ex.getClass().getName()).append(":");

    // 2. 첫 번째 애플리케이션 코드 스택 프레임 (외부 프레임워크 노이즈 필터링)
    String appFrame = findFirstAppFrame(ex);
    key.append(appFrame).append(":");

    // 3. Root Cause 클래스명
    key.append(findRootCause(ex).getClass().getName());

    return key.toString();
  }

  private static String findFirstAppFrame(Throwable ex) {
    ApmProperties props = ApmPropertiesHolder.getProperties();
    List<String> customPrefixes =
        (props != null && props.getError() != null)
            ? props.getError().getAppPackagePrefixes()
            : List.of();

    // 1) 사용자가 정의한 패키지 프리픽스가 있으면 우선 일치 검사
    if (!customPrefixes.isEmpty()) {
      for (StackTraceElement frame : ex.getStackTrace()) {
        String cls = frame.getClassName();
        for (String prefix : customPrefixes) {
          if (cls.startsWith(prefix)) {
            return cls + "." + frame.getMethodName();
          }
        }
      }
    }

    // 2) 일반적인 프레임워크 제외 규칙 적용
    for (StackTraceElement frame : ex.getStackTrace()) {
      String cls = frame.getClassName();
      boolean isFramework = false;
      for (String prefix : IGNORED_FRAMEWORK_PREFIXES) {
        if (cls.startsWith(prefix)) {
          isFramework = true;
          break;
        }
      }
      if (!isFramework) {
        return cls + "." + frame.getMethodName();
      }
    }

    // 3) 모든 프레임이 필터링되었을 경우 첫 번째 프레임 사용
    StackTraceElement[] stack = ex.getStackTrace();
    return (stack.length > 0)
        ? stack[0].getClassName() + "." + stack[0].getMethodName()
        : "unknown";
  }

  private static Throwable findRootCause(Throwable ex) {
    Throwable current = ex;
    for (int i = 0; i < MAX_CAUSE_DEPTH && current.getCause() != null; i++) {
      current = current.getCause();
    }
    return current;
  }
}
