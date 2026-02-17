package io.data;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
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
    private int circuitOpenTimeout;
    private int maxAttempts;
  }

  @Bean
  public RetryTemplate retryTemplate(KVRetryPolicy retryPolicy, RetryPropertyConfig config) {
    RetryTemplate retryTemplate = new RetryTemplate();
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setMaxInterval(config.getMaxInterval());
    backOffPolicy.setInitialInterval(config.getInitialInterval());
    backOffPolicy.setMultiplier(config.getMultiplier());
    // short circuit retry attempts on lengthy network timeouts
    TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy(config.getCircuitOpenTimeout());
    CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
    compositeRetryPolicy.setPolicies(new RetryPolicy[] {timeoutRetryPolicy, retryPolicy});
    retryTemplate.setRetryPolicy(compositeRetryPolicy);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    return retryTemplate;
  }
}
