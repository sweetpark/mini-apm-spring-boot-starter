package io.github.sweetpark.apm.support.batch;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.enums.LogMarker;
import io.github.sweetpark.apm.core.enums.TraceLevel;
import io.github.sweetpark.apm.core.process.AbstractLogProcessor;

/** Spring Batch Job 및 Step 단위의 요약 정보 및 SQL 통계를 로깅하는 프로세서입니다. */
public class BatchLogProcessor extends AbstractLogProcessor<LogBatchContext> {

  public BatchLogProcessor(ApmProperties properties) {
    super(properties);
  }

  @Override
  protected void logApi(LogBatchContext ctx) {
    String traceId = ctx.getTraceId();
    String spanId = ctx.getSpanId();
    TraceLevel level = resolveLevel();

    boolean isError = ctx.getEx() != null || "FAILED".equalsIgnoreCase(ctx.getStatus());

    logger.info(
        LogMarker.BATCH.marker(),
        "trace_id={} span_id={} job_name={} step_name={} status={} elapsed={}ms",
        traceId != null ? traceId : "-",
        spanId != null ? spanId : "-",
        ctx.getJobName() != null ? ctx.getJobName() : "-",
        ctx.getStepName() != null ? ctx.getStepName() : "-",
        ctx.getStatus() != null ? ctx.getStatus() : "-",
        (long) ctx.getElapsedMs());

    logSqlDetails(traceId, spanId, level, isError);
    logException(ctx, traceId, spanId);
  }
}
