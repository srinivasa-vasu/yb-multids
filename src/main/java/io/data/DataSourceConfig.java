package io.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Configuration
public class DataSourceConfig {

  enum DataSourceType {
    DS2,
    DS1
  }

  @Bean
  public Executor multiDSExecutorPool() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("MultiDS-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    // This is the "Safety Valve"
    // If the queue is full, the main thread will execute the query itself instead of throwing an
    // exception. This naturally slows down the incoming request rate (Backpressure).
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  @ConfigurationProperties(prefix = "spring.datasource.ds1")
  @Bean(name = "ds1Prop")
  public DataSourceProperties rwProperties() {
    return new DataSourceProperties();
  }

  @ConfigurationProperties(prefix = "spring.datasource.ds2")
  @Bean(name = "ds2Prop")
  public DataSourceProperties roProperties() {
    return new DataSourceProperties();
  }

  private HikariConfig getConfig(DataSourceProperties props) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(props.getUrl());
    config.setUsername(props.getUsername());
    config.setPassword(props.getPassword());
    config.setDriverClassName(props.getDriverClassName());
    return config;
  }

  @Bean
  @ConfigurationProperties("spring.datasource.ds1.hikari")
  public HikariConfig ds1HikariConfig(@Qualifier("ds1Prop") DataSourceProperties props) {
    return getConfig(props);
  }

  @Bean
  @ConfigurationProperties("spring.datasource.ds2.hikari")
  public HikariConfig ds2HikariConfig(@Qualifier("ds2Prop") DataSourceProperties props) {
    return getConfig(props);
  }

  @Bean(name = "ds1DataSource")
  public DataSource ds1DataSource(@Qualifier("ds1HikariConfig") HikariConfig hikari) {
    return new HikariDataSource(hikari);
  }

  @Bean(name = "ds2DataSource")
  public DataSource ds2DataSource(@Qualifier("ds2HikariConfig") HikariConfig hikari) {
    return new HikariDataSource(hikari);
  }

  @Bean(name = "routingDataSource")
  public DataSource routingDataSource(
      @Qualifier("ds1DataSource") DataSource ds1DataSource,
      @Qualifier("ds2DataSource") DataSource ds2DataSource) {
    AbstractRoutingDataSource routingDataSource = new AnnotationAspectRoutingDataSource();
    routingDataSource.setDefaultTargetDataSource(ds1DataSource);
    routingDataSource.setTargetDataSources(
        new HashMap<>() {
          {
            put(DataSourceType.DS1, ds1DataSource);
            put(DataSourceType.DS2, ds2DataSource);
          }
        });
    routingDataSource.afterPropertiesSet();
    return routingDataSource;
  }

  @Bean
  @Primary
  public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
    return new LazyConnectionDataSourceProxy(routingDataSource);
  }

  static class AnnotationAspectRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected @Nullable Object determineCurrentLookupKey() {
      return AspectRoutingContext.active();
    }
  }

  static class AspectRoutingContext {
    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    public static void preferDS(DataSourceType type) {
      if (!TransactionSynchronizationManager.isActualTransactionActive()) {
        CONTEXT.set(type);
      }
    }

    public static DataSourceType active() {
      DataSourceType v = CONTEXT.get();
      return v != null ? v : DataSourceType.DS1;
    }

    public static void clear() {
      CONTEXT.remove();
    }
  }
}
