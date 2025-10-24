package com.shikshaspace.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for API explorer. Enables Swagger UI with JWT authentication
 * support.
 */
@Slf4j
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI userServiceAPI() {
    log.info("Configuring OpenAPI documentation");

    return new OpenAPI()
        .info(
            new Info()
                .title("User Service API")
                .description("Reactive User Service with Keycloak SSO Integration")
                .version("1.0.0")
                .contact(new Contact().name("Shiksha Space").url("https://shubhamsinghrajput.com")))
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
