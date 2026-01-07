package io.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Configuration
public class DataSourceConfig {

  enum DataSourceType {
    RO,
    RW
  }

  @ConfigurationProperties(prefix = "spring.datasource.rw")
  @Bean(name = "rwDSProp")
  public DataSourceProperties rwProperties() {
    return new DataSourceProperties();
  }

  @ConfigurationProperties(prefix = "spring.datasource.ro")
  @Bean(name = "roDSProp")
  public DataSourceProperties roProperties() {
    return new DataSourceProperties();
  }

  private HikariConfig getConfig(DataSourceProperties props){
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(props.getUrl());
    config.setUsername(props.getUsername());
    config.setPassword(props.getPassword());
    config.setDriverClassName(props.getDriverClassName());
    return config;
  }

  @Bean
  @ConfigurationProperties("spring.datasource.rw.hikari")
  public HikariConfig rwHikariConfig(@Qualifier("rwDSProp") DataSourceProperties props) {
    return getConfig(props);
  }

  @Bean
  @ConfigurationProperties("spring.datasource.ro.hikari")
  public HikariConfig roHikariConfig(@Qualifier("roDSProp") DataSourceProperties props) {
    return getConfig(props);
  }

  @Bean(name = "rwDataSource")
  public DataSource rwDataSource(@Qualifier("rwHikariConfig") HikariConfig hikari) {
    return new HikariDataSource(hikari);
  }

  @Bean(name = "roDataSource")
  public DataSource roDataSource(@Qualifier("roHikariConfig") HikariConfig hikari) {
    return new HikariDataSource(hikari);
  }

  @Bean(name = "routingDataSource")
  public DataSource routingDataSource(
      @Qualifier("rwDataSource") DataSource rwDataSource,
      @Qualifier("roDataSource") DataSource roDataSource) {
    //    AbstractRoutingDataSource routingDataSource = new AspectRoutingDataSource();
    //    AbstractRoutingDataSource routingDataSource = new AnnotationAspectRoutingDataSource();
    AbstractRoutingDataSource routingDataSource = new TxnRoutingDataSource();
    routingDataSource.setDefaultTargetDataSource(rwDataSource);
    routingDataSource.setTargetDataSources(
        new HashMap<>() {
          {
            put(DataSourceType.RW, rwDataSource);
            put(DataSourceType.RO, roDataSource);
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

  static class AspectRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected @Nullable Object determineCurrentLookupKey() {
      return AspectRoutingContext.active();
    }
  }

  static class AnnotationAspectRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected @Nullable Object determineCurrentLookupKey() {
      return AspectRoutingContext.active();
    }
  }

  static class AspectRoutingContext {
    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    public static void requestRO() {
      if (!TransactionSynchronizationManager.isActualTransactionActive()) {
        CONTEXT.set(DataSourceType.RO);
      }
    }

    public static void requestRW() {
      if (!TransactionSynchronizationManager.isActualTransactionActive()) {
        CONTEXT.set(DataSourceType.RW);
      }
    }

    public static DataSourceType active() {
      DataSourceType v = CONTEXT.get();
      return v != null ? v : DataSourceType.RW;
    }

    public static void clear() {
      CONTEXT.remove();
    }
  }

  static class TxnRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected @Nullable Object determineCurrentLookupKey() {
      return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
          ? DataSourceType.RO
          : DataSourceType.RW;
    }
  }
}
