package io.github.sweetpark.apm.support.servlet;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.error.ErrorEvaluator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** 서블릿 웹 애플리케이션용 APM 필터 자동 설정 클래스입니다. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "apm",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApmWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public FilterRegistrationBean<LoggingFilter> apmLoggingFilterRegistration(
      ApmProperties properties, ErrorEvaluator errorEvaluator) {
    FilterRegistrationBean<LoggingFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new LoggingFilter(properties, errorEvaluator));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    bean.addUrlPatterns("/*");
    return bean;
  }
}
