package io.github.sweetpark.apm.interceptor.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sweetpark.apm.core.config.ApmProperties;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

class ApmDataSourceBeanPostProcessorTest {

  @Test
  @DisplayName("getOrder()는 최우선순위를 반환한다")
  void testOrder() {
    assertThat(new ApmDataSourceBeanPostProcessor().getOrder())
        .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }

  @Test
  @DisplayName("DataSource 빈은 ApmProxyDataSource로 래핑된다")
  void testWrapsDataSourceBean() {
    ApmDataSourceBeanPostProcessor processor =
        new ApmDataSourceBeanPostProcessor(new ApmProperties());
    JdbcDataSource raw = new JdbcDataSource();

    Object result = processor.postProcessAfterInitialization(raw, "dataSource");

    assertThat(result).isInstanceOf(ApmProxyDataSource.class);
    assertThat(((ApmProxyDataSource) result).getTargetDataSource()).isSameAs(raw);
  }

  @Test
  @DisplayName("이미 프록시된 DataSource는 이중으로 래핑하지 않는다")
  void testDoesNotDoubleWrap() {
    ApmDataSourceBeanPostProcessor processor = new ApmDataSourceBeanPostProcessor();
    ApmProxyDataSource alreadyProxied =
        new ApmProxyDataSource(new JdbcDataSource(), new ApmProperties());

    Object result = processor.postProcessAfterInitialization(alreadyProxied, "dataSource");

    assertThat(result).isSameAs(alreadyProxied);
  }

  @Test
  @DisplayName("DataSource가 아닌 빈은 그대로 반환한다")
  void testIgnoresNonDataSourceBeans() {
    ApmDataSourceBeanPostProcessor processor = new ApmDataSourceBeanPostProcessor();
    Object bean = new Object();

    Object result = processor.postProcessAfterInitialization(bean, "someBean");

    assertThat(result).isSameAs(bean);
  }
}
