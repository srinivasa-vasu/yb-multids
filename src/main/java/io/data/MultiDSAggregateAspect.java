package io.data;

import io.data.DataSourceConfig.AspectRoutingContext;
import io.data.DataSourceConfig.DataSourceType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MultiDSAggregateAspect {
  private final Executor executor;
  private final RetryTemplate retryTemplate;

  public MultiDSAggregateAspect(
      @Qualifier("multiDSExecutorPool") Executor executor, RetryTemplate retryTemplate) {
    this.executor = executor;
    this.retryTemplate = retryTemplate;
  }

  private MultiDSAggregate getAnnotation(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    MultiDSAggregate annotate = signature.getMethod().getAnnotation(MultiDSAggregate.class);
    if (annotate == null) {
      annotate = joinPoint.getTarget().getClass().getAnnotation(MultiDSAggregate.class);
    }
    return annotate;
  }

  private Object executeFallbackAsync(ProceedingJoinPoint joinPoint, DataSourceType[] sources) {
    if (sources == null || sources.length == 0) return null;

    CompletableFuture<Object> chain =
        CompletableFuture.supplyAsync(() -> executeInContext(joinPoint, sources[0]), executor);

    for (int i = 1; i < sources.length; i++) {
      final DataSourceType nextSource = sources[i];
      chain =
          chain.thenCompose(
              result -> {
                if (isPopulated(result)) {
                  return CompletableFuture.completedFuture(result);
                }
                return CompletableFuture.supplyAsync(
                    () -> executeInContext(joinPoint, nextSource), executor);
              });
    }
    return chain.join();
  }

  private Object executeParallelAggregation(
      ProceedingJoinPoint joinPoint, DataSourceType[] sources) {
    return Arrays.stream(sources)
        .map(
            source ->
                CompletableFuture.supplyAsync(() -> executeInContext(joinPoint, source), executor))
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .flatMap(
            result ->
                result instanceof Collection
                    ? ((Collection<?>) result).stream()
                    : Stream.of(result))
        .collect(Collectors.toList());
  }

  private Object executeInContext(ProceedingJoinPoint joinPoint, DataSourceType source) {
    try {
      AspectRoutingContext.preferDS(source);
      return retryTemplate.execute(e -> joinPoint.proceed());
    } catch (Throwable ex) {
      throw new RuntimeException("Execution failed for: " + source, ex);
    } finally {
      AspectRoutingContext.clear();
    }
  }

  private boolean isPopulated(Object result) {
    if (result == null) return false;
    if (result instanceof Collection) return !((Collection<?>) result).isEmpty();
    return true;
  }

  @Around("@annotation(io.data.MultiDSAggregate) || @within(io.data.MultiDSAggregate)")
  public Object handleMultiDS(ProceedingJoinPoint joinPoint) throws Throwable {
    MultiDSAggregate aggregate = getAnnotation(joinPoint);
    DataSourceType[] sources = {DataSourceType.DS1, DataSourceType.DS2};
    // false = aggregate all, true = sequential fallback
    return aggregate.fallback()
        ? executeFallbackAsync(joinPoint, sources)
        : executeParallelAggregation(joinPoint, sources);
  }
}
