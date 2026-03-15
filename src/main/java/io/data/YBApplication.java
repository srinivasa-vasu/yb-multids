package io.data;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryTemplate;

@SpringBootApplication(proxyBeanMethods = false)
public class YBApplication {

  static void main(String[] args) {
    SpringApplication.run(YBApplication.class, args);
  }

  @ConfigurationProperties(prefix = "spring.retry")
  @Configuration
  @Data
  public static class RetryPropertyConfig {
    private int maxInterval;
    private int initialInterval;
    private int multiplier;
    private int maxAttempts;
    private int jitter;
  }

  @Bean
  public RetryTemplate retryTemplate(KVRetryPolicy retryPolicy, RetryPropertyConfig config) {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }
}
