package io.github.sweetpark.apm.core.error;

/**
 * 응답 상태, 바디 및 예외 객체를 종합하여 해당 요청이 에러인지 판정하는 인터페이스입니다.
 *
 * <p>소비 프로젝트에서 ErrorEvaluator 빈을 직접 등록하면 고유한 에러 판정 비즈니스 로직을 적용할 수 있습니다.
 */
@FunctionalInterface
public interface ErrorEvaluator {

  /**
   * 요청 결과가 에러인지 판정합니다.
   *
   * @param httpStatusCode HTTP 응답 코드 (Netty/Batch 환경인 경우 -1)
   * @param responseBody 응답 본문 문자열
   * @param exception 발생한 예외 객체 (없으면 null)
   * @return 에러인 경우 true, 정상이면 false
   */
  boolean isError(int httpStatusCode, String responseBody, Throwable exception);
}
