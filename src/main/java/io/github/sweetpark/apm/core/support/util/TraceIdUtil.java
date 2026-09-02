package io.github.sweetpark.apm.core.support.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** W3C TraceContext 표준 규격에 부합하는 32자리 traceId 및 16자리 spanId 생성 유틸리티입니다. */
public final class TraceIdUtil {

  private TraceIdUtil() {}

  public static String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public static String generateSpanId() {
    long value = ThreadLocalRandom.current().nextLong();
    return String.format("%016x", value);
  }
}
