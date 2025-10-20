package com.shikshaspace.userservice.security;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtUtils {

  public Mono<String> getCurrentUsername() {
    return getJwt().map(jwt -> jwt.getClaimAsString("preferred_username"));
  }

  public Mono<UUID> getCurrentKeycloakId() {
    return getJwt().map(jwt -> UUID.fromString(jwt.getSubject()));
  }

  public Mono<String> getCurrentEmail() {
    return getJwt().map(jwt -> jwt.getClaimAsString("email"));
  }

  @SuppressWarnings("unchecked")
  public Mono<List<String>> getCurrentUserRoles() {
    return getJwt()
        .map(
            jwt -> {
              Map<String, Object> realmAccess = jwt.getClaim("realm_access");
              if (realmAccess != null && realmAccess.containsKey("roles")) {
                return (List<String>) realmAccess.get("roles");
              }
              return List.<String>of();
            });
  }

  private Mono<Jwt> getJwt() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getPrincipal)
        .cast(Jwt.class)
        .doOnError(e -> log.error("Error extracting JWT: {}", e.getMessage()));
  }
}
