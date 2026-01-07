package io.data;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@SpringBootApplication(proxyBeanMethods = false)
@EnableRetry
public class MultiDSApplication {

  static void main(String[] args) {
    SpringApplication.run(MultiDSApplication.class, args);
  }

  @ConfigurationProperties(prefix = "spring.retry")
  @Configuration
  @Data
  public static class RetryPropertyConfig {
    private int maxInterval;
    private int initialInterval;
    private int multiplier;
  }

  @Bean
  public RetryTemplate retryTemplate(KVRetryPolicy retryPolicy, RetryPropertyConfig config) {
    RetryTemplate retryTemplate = new RetryTemplate();
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setMaxInterval(config.getMaxInterval());
    backOffPolicy.setInitialInterval(config.getInitialInterval());
    backOffPolicy.setMultiplier(config.getMultiplier());
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    return retryTemplate;
  }
}
