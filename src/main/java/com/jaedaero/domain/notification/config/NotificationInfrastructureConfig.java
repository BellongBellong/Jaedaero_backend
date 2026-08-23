package com.jaedaero.domain.notification.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class NotificationInfrastructureConfig {

  @Bean(destroyMethod = "destroy")
  public LettuceConnectionFactory notificationRedisConnectionFactory(
      @Value("${REDIS_HOST:localhost}") String host,
      @Value("${REDIS_PORT:6379}") int port,
      @Value("${REDIS_USERNAME:}") String username,
      @Value("${REDIS_PASSWORD:}") String password,
      @Value("${REDIS_SSL_ENABLED:false}") boolean sslEnabled) {
    RedisStandaloneConfiguration redis = new RedisStandaloneConfiguration(host, port);
    if (StringUtils.hasText(username)) {
      redis.setUsername(username);
    }
    if (StringUtils.hasText(password)) {
      redis.setPassword(RedisPassword.of(password));
    }

    LettuceClientConfiguration.LettuceClientConfigurationBuilder client =
        LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(5))
            .shutdownTimeout(Duration.ZERO);
    if (sslEnabled) {
      client.useSsl();
    }
    return new LettuceConnectionFactory(redis, client.build());
  }

  @Bean
  public StringRedisTemplate notificationRedisTemplate(
      LettuceConnectionFactory notificationRedisConnectionFactory) {
    return new StringRedisTemplate(notificationRedisConnectionFactory);
  }
}
