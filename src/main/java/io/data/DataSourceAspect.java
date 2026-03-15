package io.data;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceAspect {

  private final RetryTemplate retryTemplate;
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(DataSourceAspect.class);

  public DataSourceAspect(RetryTemplate retryTemplate) {
    this.retryTemplate = retryTemplate;
  }

  @Pointcut("within(*..*Service) || within(*..*svc)")
  public void servicePointcut() {}

  //  @Around("servicePointcut()")
  @Around(
      "@annotation(org.springframework.stereotype.Service) || @within(org.springframework.stereotype.Service)")
  public Object wrapper(ProceedingJoinPoint pjp) throws Throwable {
    return retryTemplate.execute(pjp::proceed);
  }
}
