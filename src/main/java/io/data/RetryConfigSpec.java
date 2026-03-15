package io.data;

import io.r2dbc.spi.R2dbcException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import reactor.util.retry.Retry;

@ConfigurationProperties(prefix = "spring.retry")
@Configuration
@Data
public class RetryConfigSpec {
  private int maxInterval;
  private int initialInterval;
  private double multiplier;
  private int maxAttempts;
  private double jitter;

  private static final String SQL_STATE = "^(40001)|(40P01)|(57P01)|(08006)";

  // oom-killer or a process crash in the middle of a tx
  private static final String SQL_MSG = "^(connection is closed)|(connection reset by peer)";

  // XX000 shouldn't be re-tried. Retry it only when the state and msg matches
  private final Map<String, List<String>> specialCodes =
      Map.of("XX000", List.of("schema version mismatch", "duplicate request"));

  private final Predicate<R2dbcException> sqlStatePredicate =
      exception -> {
        String state = exception.getSqlState();
        if (state == null) return false;

        // Case 1: direct SQL_STATE match
        if (state.matches(SQL_STATE)) return true;

        // Case 2: special codes check
        if (specialCodes.containsKey(state)) {
          String msg = exception.getMessage();
          if (msg != null) {
            // shouldn't retry commit statements
            return specialCodes.get(state).stream()
                .anyMatch(code -> msg.toLowerCase().contains(code));
          }
        }

        // Default: no match
        return false;
      };

  private final Predicate<R2dbcException> sqlMsgPredicate =
      exception ->
          Optional.ofNullable(exception.getMessage())
              .filter(msg -> msg.toLowerCase().matches(SQL_MSG))
              .isPresent();

  private final Predicate<Throwable> exceptionPredicate =
      exception ->
          (exception instanceof SQLRecoverableException
              || exception instanceof SQLTransientConnectionException
              || exception instanceof TransientDataAccessException);

  public boolean shouldRetry(@NonNull Throwable cause) {
    do {
      if ((cause instanceof R2dbcException exception
          && (sqlStatePredicate.or(sqlMsgPredicate).test(exception)))) {
        return true;
      }
      cause = cause.getCause();
    } while (cause != null);
    return false;
  }

  @Bean
  public Retry retrySpec() {
    return Retry.backoff(maxAttempts, Duration.ofMillis(initialInterval))
        .multiplier(multiplier)
        .maxBackoff(Duration.ofSeconds(maxInterval))
        .jitter(jitter)
        .filter(this::shouldRetry)
        .onRetryExhaustedThrow((spec, signal) -> signal.failure());
  }
}
