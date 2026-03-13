package io.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import org.hibernate.TransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessException;

class KVRetryPolicyTest {

  private KVRetryPolicy policy;

  @BeforeEach
  void setUp() {
    var config = new MultiDSApplication.RetryPropertyConfig();
    config.setMaxAttempts(3);
    policy = new KVRetryPolicy(config);
    policy.init();
  }

  @Test
  void shouldRetryOnSerializationFailure() {
    var ex = new SQLException("serialization failure", "40001");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnDeadlock() {
    var ex = new SQLException("deadlock detected", "40P01");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnConnectionFailure() {
    var ex = new SQLException("connection failure", "08006");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnBrokenPoolConnection() {
    var ex = new SQLException("pool connection invalidated", "57P01");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnConnectionClosed() {
    var ex = new SQLException("connection is closed");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnConnectionResetByPeer() {
    var ex = new SQLException("connection reset by peer");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnSchemaVersionMismatch() {
    var ex = new SQLException("schema version mismatch", "XX000");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnDuplicateRequest() {
    var ex = new SQLException("duplicate request detected", "XX000");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldNotRetryOnGenericXX000() {
    var ex = new SQLException("some other internal error", "XX000");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isFalse();
  }

  @Test
  void shouldRetryOnSQLRecoverableException() {
    var ex = new SQLRecoverableException("recoverable error");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnSQLTransientConnectionException() {
    var ex = new SQLTransientConnectionException("transient connection error");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnTransientDataAccessException() {
    var ex = new TransientDataAccessException("transient data access error") {};
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldRetryOnTransactionException() {
    var ex = new TransactionException("transaction failed");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldNotRetryOnGenericSQLException() {
    var ex = new SQLException("generic error", "12345");
    var context = policy.open(null);
    policy.registerThrowable(context, ex);

    assertThat(policy.canRetry(context)).isFalse();
  }

  @Test
  void shouldRetryOnNestedSQLException() {
    var sqlEx = new SQLException("serialization failure", "40001");
    var runtimeEx = new RuntimeException("wrapped", sqlEx);
    var context = policy.open(null);
    policy.registerThrowable(context, runtimeEx);

    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  void shouldNotRetryOnDeeplyNestedUnrelatedException() {
    var genericEx = new SQLException("generic error", "12345");
    var runtimeEx = new RuntimeException("wrapped", genericEx);
    var context = policy.open(null);
    policy.registerThrowable(context, runtimeEx);

    assertThat(policy.canRetry(context)).isFalse();
  }
}
