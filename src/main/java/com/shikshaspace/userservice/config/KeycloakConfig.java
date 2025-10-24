package com.shikshaspace.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin Client configuration for user management operations. Uses service account
 * credentials for administrative tasks.
 */
@Slf4j
@Configuration
public class KeycloakConfig {

  @Value("${keycloak.server-url}")
  private String serverUrl;

  @Value("${keycloak.realm}")
  private String realm;

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.client-secret}")
  private String clientSecret;

  @Value("${keycloak.admin.username}")
  private String adminUsername;

  @Value("${keycloak.admin.password}")
  private String adminPassword;

  /**
   * Creates Keycloak admin client for user and realm management. Uses admin credentials with
   * sufficient privileges.
   */
  @Bean
  public Keycloak keycloak() {
    log.info("Initializing Keycloak Admin Client for realm: {}", realm);

    return KeycloakBuilder.builder()
        .serverUrl(serverUrl)
        .realm(realm)
        .clientId("admin-cli")
        .grantType(OAuth2Constants.PASSWORD)
        .username(adminUsername)
        .password(adminPassword)
        .build();
  }
}
