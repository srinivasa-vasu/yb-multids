package io.data;

import io.data.DataSourceConfig.AspectRoutingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

// @Aspect
// @Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataSourceAspect {
  @Around("readPointcut()")
  public Object setRead(ProceedingJoinPoint pjp) throws Throwable {
    try {
      AspectRoutingContext.requestRO();
      return pjp.proceed();
    } finally {
      AspectRoutingContext.clear();
    }
  }

  @Around("writePointcut()")
  public Object setWrite(ProceedingJoinPoint pjp) throws Throwable {
    try {
      AspectRoutingContext.requestRW();
      return pjp.proceed();
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
      return pjp.proceed();
    } finally {
      AspectRoutingContext.clear();
    }
  }
}
