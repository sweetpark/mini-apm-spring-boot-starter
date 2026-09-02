package io.github.sweetpark.apm.support.batch;

import io.github.sweetpark.apm.core.config.ApmProperties;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Spring Batch 환경을 위한 APM 자동 설정 클래스입니다. */
@AutoConfiguration
@ConditionalOnClass({JobExecutionListener.class, StepExecutionListener.class})
@ConditionalOnProperty(
    prefix = "apm",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApmBatchAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public LoggingBatchListener loggingBatchListener(ApmProperties properties) {
    return new LoggingBatchListener(properties);
  }
}
