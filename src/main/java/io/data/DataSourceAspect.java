package io.data;

import io.data.DataSourceConfig.AspectRoutingContext;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceAspect {
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(DataSourceAspect.class);
  private final RetryTemplate retryTemplate;

  public DataSourceAspect(RetryTemplate retryTemplate) {
    this.retryTemplate = retryTemplate;
  }

  @Around("readPointcut()")
  public Object setRead(ProceedingJoinPoint pjp) throws Throwable {
    try {
      AspectRoutingContext.requestRO();
      return retryTemplate.execute(
          _ -> pjp.proceed(),
          recovery -> {
            log.error(
                "All retry attempts are either exhausted: {} or the circuit is open",
                recovery.getRetryCount(),
                recovery.getLastThrowable());
            return Optional.empty();
          });
    } finally {
      AspectRoutingContext.clear();
    }
  }

  @Around("writePointcut()")
  public Object setWrite(ProceedingJoinPoint pjp) throws Throwable {
    try {
      AspectRoutingContext.requestRW();
      return retryTemplate.execute(
          _ -> pjp.proceed(),
          recovery -> {
            log.error(
                "All retry attempts are either exhausted: {} or the circuit is open",
                recovery.getRetryCount(),
                recovery.getLastThrowable());
            return Optional.empty();
          });
    } finally {
      AspectRoutingContext.clear();
    }
  }

  @Pointcut(
      "execution(* org.springframework.data.repository.Repository+.find*(..)) || execution(* org.springframework.data.repository.Repository+.get*(..))")
  void readPointcut() {}

  @Pointcut(
      "execution(* org.springframework.data.repository.Repository+.save*(..)) || execution(* org.springframework.data.repository.Repository+.delete*(..))")
  void writePointcut() {}

  @Around("@annotation(ReadOnly) || @within(ReadOnly)")
  public Object routeToRead(ProceedingJoinPoint pjp) throws Throwable {
    try {
      AspectRoutingContext.requestRO();
      return retryTemplate.execute(
          _ -> pjp.proceed(),
          recovery -> {
            log.error(
                "All retry attempts are either exhausted: {} or the circuit is open",
                recovery.getRetryCount(),
                recovery.getLastThrowable());
            return Optional.empty();
          });
    } finally {
      AspectRoutingContext.clear();
    }
  }
}
