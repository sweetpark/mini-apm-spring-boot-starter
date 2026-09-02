package io.github.sweetpark.apm.core.error;

import java.time.Instant;

/** 에러 발생 전 애플리케이션의 실행 경로(발자국)를 기록하는 이벤트 모델입니다. */
public record BreadcrumbEvent(Instant timestamp, String category, String message) {
  public BreadcrumbEvent(String category, String message) {
    this(Instant.now(), category, message);
  }
}
