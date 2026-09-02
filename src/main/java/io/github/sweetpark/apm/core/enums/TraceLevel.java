package io.github.sweetpark.apm.core.enums;

/** 추적 상세 수준을 정의하는 열거형입니다. */
public enum TraceLevel {
  /** 운영 기본 모드: 요약 정보 및 에러/슬로우 중심 로깅 */
  PROD,

  /** 상세 추적 모드: 요청/응답 파라미터 및 바인딩된 모든 SQL 상세 정보 로깅 */
  TRACE
}
