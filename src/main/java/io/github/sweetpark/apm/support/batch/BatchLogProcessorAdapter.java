package io.github.sweetpark.apm.support.batch;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.enums.TraceLevel;

/** TaskDecorator 등 외부에서 안전하게 SQL 상세 로깅을 호출하기 위한 어댑터입니다. */
public class BatchLogProcessorAdapter extends BatchLogProcessor {

  public BatchLogProcessorAdapter(ApmProperties properties) {
    super(properties);
  }

  public void logSqlOnly(String traceId, String spanId, TraceLevel level) {
    try {
      logSqlDetails(traceId, spanId, level, false);
    } catch (Exception ex) {
      logger.error(
          "[APM_INTERNAL_ERROR] batch task SQL logging failed traceId={} cause={}",
          traceId,
          ex.getMessage());
    }
  }
}
