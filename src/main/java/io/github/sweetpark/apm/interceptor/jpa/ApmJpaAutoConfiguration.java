package io.github.sweetpark.apm.interceptor.jpa;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

/** JPA / Hibernate / JDBC 환경을 위한 DataSource 프록시 SQL 추적 자동 설정 클래스입니다. */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(
    prefix = "apm",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApmJpaAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public static ApmDataSourceBeanPostProcessor apmDataSourceBeanPostProcessor() {
    return new ApmDataSourceBeanPostProcessor();
  }
}
