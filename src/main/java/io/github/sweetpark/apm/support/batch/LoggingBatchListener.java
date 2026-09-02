package io.github.sweetpark.apm.support.batch;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.context.TraceContextHolder;
import io.github.sweetpark.apm.core.sql.SqlTraceContextHolder;
import io.github.sweetpark.apm.core.support.util.TraceIdUtil;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.NonNull;

/** Spring Batch Job 및 Step의 라이프사이클을 추적하는 리스너입니다. */
public class LoggingBatchListener implements JobExecutionListener, StepExecutionListener {

  private final BatchLogProcessor logProcessor;
  private final ApmProperties properties;

  public LoggingBatchListener(ApmProperties properties) {
    this.properties = properties;
    this.logProcessor = new BatchLogProcessor(properties);
  }

  @Override
  public void beforeJob(@NonNull JobExecution jobExecution) {
    String traceId = TraceIdUtil.generateTraceId();
    String jobSpanId = TraceIdUtil.generateSpanId();

    jobExecution.getExecutionContext().put("traceId", traceId);
    jobExecution.getExecutionContext().put("jobSpanId", jobSpanId);
    jobExecution.getExecutionContext().put("jobStartNano", System.nanoTime());

    TraceContextHolder.init(traceId, jobSpanId, properties.getTrace().getLevel(), false);
    SqlTraceContextHolder.init();
  }

  @Override
  public void afterJob(@NonNull JobExecution jobExecution) {
    try {
      String traceId = jobExecution.getExecutionContext().getString("traceId");
      String jobSpanId = jobExecution.getExecutionContext().getString("jobSpanId");
      Long startNano = (Long) jobExecution.getExecutionContext().get("jobStartNano");

      double elapsedMs = 0;
      if (startNano != null) {
        elapsedMs = (System.nanoTime() - startNano) / 1_000_000.0;
      }

      Exception ex = null;
      if (!jobExecution.getFailureExceptions().isEmpty()) {
        Throwable t = jobExecution.getFailureExceptions().get(0);
        ex = (t instanceof Exception exception) ? exception : new RuntimeException(t);
      }

      LogBatchContext batchContext =
          new LogBatchContext.Builder()
              .traceId(traceId)
              .spanId(jobSpanId)
              .jobName(jobExecution.getJobInstance().getJobName())
              .stepName("JOB")
              .status(jobExecution.getStatus().toString())
              .elapsedMs(elapsedMs)
              .ex(ex)
              .build();

      logProcessor.process(batchContext);
    } finally {
      clearContext();
    }
  }

  @Override
  public void beforeStep(@NonNull StepExecution stepExecution) {
    String traceId = stepExecution.getJobExecution().getExecutionContext().getString("traceId");
    String stepSpanId = TraceIdUtil.generateSpanId();

    stepExecution.getExecutionContext().put("stepSpanId", stepSpanId);
    stepExecution.getExecutionContext().put("stepStartNano", System.nanoTime());

    TraceContextHolder.init(traceId, stepSpanId, properties.getTrace().getLevel(), false);
    SqlTraceContextHolder.init();
  }

  @Override
  public ExitStatus afterStep(@NonNull StepExecution stepExecution) {
    String traceId = stepExecution.getJobExecution().getExecutionContext().getString("traceId");
    String stepSpanId = stepExecution.getExecutionContext().getString("stepSpanId");
    Long startNano = (Long) stepExecution.getExecutionContext().get("stepStartNano");

    double elapsedMs = 0;
    if (startNano != null) {
      elapsedMs = (System.nanoTime() - startNano) / 1_000_000.0;
    }

    Exception ex = null;
    if (!stepExecution.getFailureExceptions().isEmpty()) {
      Throwable t = stepExecution.getFailureExceptions().get(0);
      ex = (t instanceof Exception exception) ? exception : new RuntimeException(t);
    }

    LogBatchContext batchContext =
        new LogBatchContext.Builder()
            .traceId(traceId)
            .spanId(stepSpanId)
            .jobName(stepExecution.getJobExecution().getJobInstance().getJobName())
            .stepName(stepExecution.getStepName())
            .status(stepExecution.getStatus().toString())
            .elapsedMs(elapsedMs)
            .ex(ex)
            .build();

    logProcessor.process(batchContext);

    SqlTraceContextHolder.clear();
    TraceContextHolder.clear();

    return stepExecution.getExitStatus();
  }

  private void clearContext() {
    SqlTraceContextHolder.clear();
    TraceContextHolder.clear();
  }
}
