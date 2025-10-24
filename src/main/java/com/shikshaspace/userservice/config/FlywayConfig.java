package com.shikshaspace.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Production-ready Flyway configuration for database migrations. Handles schema versioning and
 * migration management.
 */
@Slf4j
@Configuration
public class FlywayConfig {

  @Value("${spring.flyway.url}")
  private String flywayUrl;

  @Value("${spring.flyway.user}")
  private String flywayUser;

  @Value("${spring.flyway.password}")
  private String flywayPassword;

  @Bean(initMethod = "migrate")
  public Flyway flyway() {
    log.info("Initializing Flyway migrations for database schema management");

    return Flyway.configure()
        .dataSource(flywayUrl, flywayUser, flywayPassword)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true) // Handle existing databases gracefully
        .validateOnMigrate(true) // Validate migrations
        .outOfOrder(false) // Enforce migration order
        .cleanDisabled(true) // Prevent accidental data loss
        .load();
  }
}
