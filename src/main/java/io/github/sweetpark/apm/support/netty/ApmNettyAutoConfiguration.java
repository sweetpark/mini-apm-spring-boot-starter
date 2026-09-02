package io.github.sweetpark.apm.support.netty;

import io.github.sweetpark.apm.core.config.ApmProperties;
import io.netty.channel.ChannelHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Netty 로깅 관련 자동 설정 클래스입니다. */
@AutoConfiguration
@ConditionalOnClass(ChannelHandler.class)
@ConditionalOnProperty(
    prefix = "apm",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApmNettyAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public NettyTraceDuplexHandler nettyTraceDuplexHandler(ApmProperties properties) {
    return new NettyTraceDuplexHandler(properties);
  }
}
