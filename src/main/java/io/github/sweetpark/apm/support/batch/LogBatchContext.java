package io.github.sweetpark.apm.support.batch;

import io.github.sweetpark.apm.core.context.LogContext;

/** Spring Batch Job 및 Step 실행 정보와 SQL 통계를 담는 컨텍스트입니다. */
public class LogBatchContext extends LogContext {

  private final String jobName;
  private final String stepName;
  private final String status;

  private LogBatchContext(Builder builder) {
    super(builder);
    this.jobName = builder.jobName;
    this.stepName = builder.stepName;
    this.status = builder.status;
  }

  public String getJobName() {
    return jobName;
  }

  public String getStepName() {
    return stepName;
  }

  public String getStatus() {
    return status;
  }

  public static class Builder extends LogContext.Builder<Builder> {
    private String jobName;
    private String stepName;
    private String status;

    @Override
    protected Builder self() {
      return this;
    }

    public Builder jobName(String jobName) {
      this.jobName = jobName;
      return this;
    }

    public Builder stepName(String stepName) {
      this.stepName = stepName;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public LogBatchContext build() {
      return new LogBatchContext(this);
    }
  }
}
