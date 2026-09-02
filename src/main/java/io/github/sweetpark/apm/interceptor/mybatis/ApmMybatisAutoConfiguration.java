package io.github.sweetpark.apm.interceptor.mybatis;

import io.github.sweetpark.apm.core.config.ApmProperties;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** MyBatis SQL 추적 인터셉터 자동 설정 클래스입니다. */
@AutoConfiguration(before = MybatisAutoConfiguration.class)
@ConditionalOnClass({org.apache.ibatis.session.Configuration.class, ConfigurationCustomizer.class})
@ConditionalOnProperty(
    prefix = "apm",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApmMybatisAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public SqlTraceInterceptor sqlTraceInterceptor(ApmProperties properties) {
    return new SqlTraceInterceptor(properties);
  }

  @Bean
  public ConfigurationCustomizer apmSqlTraceConfigurationCustomizer(
      SqlTraceInterceptor interceptor) {
    return configuration -> configuration.addInterceptor(interceptor);
  }
}
