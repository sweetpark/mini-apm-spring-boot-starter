package io.github.sweetpark.apm.core.config;

import io.github.sweetpark.apm.core.error.DefaultErrorEvaluator;
import io.github.sweetpark.apm.core.error.ErrorEvaluator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** APM 코어 자동 구성 클래스입니다. */
@AutoConfiguration
@EnableConfigurationProperties(ApmProperties.class)
@ConditionalOnProperty(
    prefix = "apm",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApmCoreAutoConfiguration {

  public ApmCoreAutoConfiguration(ApmProperties properties) {
    ApmPropertiesHolder.setProperties(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public ErrorEvaluator errorEvaluator(ApmProperties properties) {
    return new DefaultErrorEvaluator(properties);
  }
}
