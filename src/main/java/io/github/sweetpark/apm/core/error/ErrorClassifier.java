package io.github.sweetpark.apm.core.error;

/**
 * 예외를 유형별로 분류하여 로그 마커와 심각도를 결정하는 클래스입니다.
 *
 * <p>분류 우선순위: BIZ ➔ DATABASE ➔ EXTERNAL ➔ SYSTEM
 */
public final class ErrorClassifier {

  private static final int MAX_CAUSE_DEPTH = 5;

  private ErrorClassifier() {}

  /** 에러 유형 열거형입니다. Grafana 로그의 error_type 필드 값으로 사용됩니다. */
  public enum ErrorType {
    BIZ("BIZ_ERROR"),
    DATABASE("DB_ERROR"),
    EXTERNAL("EXTERNAL_ERROR"),
    SYSTEM("SYSTEM_ERROR");

    private final String label;

    ErrorType(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }

  public static ErrorType classify(Throwable ex) {
    if (ex == null) {
      return ErrorType.SYSTEM;
    }

    Throwable current = ex;

    for (int depth = 0; depth < MAX_CAUSE_DEPTH && current != null; depth++) {
      String className = current.getClass().getName();

      if (isBizException(className)) {
        return ErrorType.BIZ;
      }

      if (isDatabaseException(className)) {
        return ErrorType.DATABASE;
      }

      if (isExternalException(className)) {
        return ErrorType.EXTERNAL;
      }

      current = current.getCause();
    }

    return ErrorType.SYSTEM;
  }

  private static boolean isBizException(String className) {
    return className.contains("BizException")
        || className.contains("BusinessException")
        || className.contains("AppException")
        || className.contains("ValidationException")
        || className.contains("InvalidRequestException")
        || className.contains("ConstraintViolationException")
        || className.contains("IllegalArgumentException");
  }

  private static boolean isDatabaseException(String className) {
    return className.contains("DataAccessException")
        || className.contains("SQLException")
        || className.contains("JdbcException")
        || className.contains("PersistenceException")
        || className.contains("HibernateException")
        || className.contains("JpaSystemException")
        || className.contains("MyBatisSystemException")
        || className.contains("DataIntegrityViolationException")
        || className.contains("CannotAcquireLockException");
  }

  private static boolean isExternalException(String className) {
    return className.contains("TimeoutException")
        || className.contains("ConnectException")
        || className.contains("SocketException")
        || className.contains("HttpClientErrorException")
        || className.contains("HttpServerErrorException")
        || className.contains("RestClientException")
        || className.contains("WebClientResponseException")
        || className.contains("FeignException");
  }
}
