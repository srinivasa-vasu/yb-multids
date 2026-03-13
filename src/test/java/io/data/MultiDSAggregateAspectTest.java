package io.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class MultiDSAggregateAspectTest {

  @Mock private ProceedingJoinPoint joinPoint;

  @Mock private MethodSignature methodSignature;

  private MultiDSAggregateAspect aspect;

  @BeforeEach
  void setUp() {
    Executor executor = Executors.newFixedThreadPool(5);
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
    aspect = new MultiDSAggregateAspect(executor, retryTemplate);
  }

  @Test
  void parallelMode_shouldQueryBothDataSources() throws Throwable {
    setupJoinPoint(ParallelTestClass.class);
    when(joinPoint.proceed()).thenReturn(List.of("item"));

    aspect.handleMultiDS(joinPoint);

    verify(joinPoint, times(2)).proceed();
  }

  @Test
  void parallelMode_shouldAggregateResults() throws Throwable {
    setupJoinPoint(ParallelTestClass.class);
    when(joinPoint.proceed())
        .thenAnswer(
            inv -> {
              var ds = DataSourceConfig.AspectRoutingContext.active();
              return ds == DataSourceConfig.DataSourceType.DS1 ? List.of("a") : List.of("b");
            });

    var result = (List<?>) aspect.handleMultiDS(joinPoint);

    assertThat(result).hasSize(2);
    assertThat(result).anyMatch("a"::equals);
    assertThat(result).anyMatch("b"::equals);
  }

  @Test
  void fallbackMode_shouldStopOnFirstPopulatedResult() throws Throwable {
    setupJoinPoint(FallbackTestClass.class);
    when(joinPoint.proceed()).thenReturn(List.of("item"));

    aspect.handleMultiDS(joinPoint);

    verify(joinPoint, times(1)).proceed();
  }

  @Test
  void fallbackMode_shouldContinueOnEmptyResult() throws Throwable {
    setupJoinPoint(FallbackTestClass.class);
    when(joinPoint.proceed()).thenReturn(List.of()).thenReturn(List.of("item"));

    aspect.handleMultiDS(joinPoint);

    verify(joinPoint, times(2)).proceed();
  }

  @Test
  void fallbackMode_shouldContinueOnNullResult() throws Throwable {
    setupJoinPoint(FallbackTestClass.class);
    when(joinPoint.proceed()).thenReturn(null).thenReturn(List.of("item"));

    aspect.handleMultiDS(joinPoint);

    verify(joinPoint, times(2)).proceed();
  }

  @Test
  void shouldClearRoutingContextAfterExecution() throws Throwable {
    setupJoinPoint(ParallelTestClass.class);
    when(joinPoint.proceed()).thenReturn(List.of());

    aspect.handleMultiDS(joinPoint);

    assertThat(DataSourceConfig.AspectRoutingContext.active())
        .isEqualTo(DataSourceConfig.DataSourceType.DS1);
  }

  private void setupJoinPoint(Class<?> targetClass) throws Exception {
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(joinPoint.getTarget()).thenReturn(targetClass.getDeclaredConstructor().newInstance());
    when(methodSignature.getMethod()).thenReturn(targetClass.getDeclaredMethod("testMethod"));
  }

  @MultiDSAggregate(fallback = false)
  static class ParallelTestClass {
    public void testMethod() {}
  }

  @MultiDSAggregate(fallback = true)
  static class FallbackTestClass {
    public void testMethod() {}
  }
}
