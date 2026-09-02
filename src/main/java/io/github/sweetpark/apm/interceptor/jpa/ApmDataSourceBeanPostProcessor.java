package io.github.sweetpark.apm.interceptor.jpa;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.github.sweetpark.apm.core.config.ApmPropertiesHolder;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/** Spring 컨텍스트에 등록되는 DataSource 빈을 ApmProxyDataSource로 감싸는 BeanPostProcessor입니다. */
public class ApmDataSourceBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

  public ApmDataSourceBeanPostProcessor() {}

  public ApmDataSourceBeanPostProcessor(ApmProperties properties) {}

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof DataSource ds && !(bean instanceof ApmProxyDataSource)) {
      return new ApmProxyDataSource(ds, ApmPropertiesHolder.getProperties());
    }
    return bean;
  }
}
