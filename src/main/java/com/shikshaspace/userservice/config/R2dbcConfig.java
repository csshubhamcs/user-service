package com.shikshaspace.userservice.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Reactive R2DBC configuration for non-blocking database operations. Enables reactive repositories
 * and transaction management.
 */
@Slf4j
@Configuration
@EnableR2dbcRepositories(basePackages = "com.shikshaspace.userservice.repository")
@EnableR2dbcAuditing
public class R2dbcConfig {

  /** Configures reactive transaction manager for R2DBC operations. */
  @Bean
  public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
    log.info("Initializing R2DBC reactive transaction manager");
    return new R2dbcTransactionManager(connectionFactory);
  }
}
