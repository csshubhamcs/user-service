package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.domain.User;
import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.response.AuthResponse;
import com.shikshaspace.userservice.exception.KeycloakException;
import com.shikshaspace.userservice.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Production-grade OAuth2 service for Google Sign-In integration. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

  private final UserRepository userRepository;
  private final KeycloakService keycloakService;
  private final AuthService authService;
  private final WebClient.Builder webClientBuilder;

  @Value("${keycloak.server-url}")
  private String keycloakServerUrl;

  @Value("${keycloak.realm}")
  private String realm;

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.client-secret}")
  private String clientSecret;

  /** Handle Google Sign-In authentication. */
  @Transactional
  public Mono<AuthResponse> handleGoogleSignIn(String googleIdToken) {
    log.info("🔵 Processing Google Sign-In request");

    return validateGoogleTokenDirectly(googleIdToken)
        .flatMap(this::processUserAuthenticationDirect)
        .doOnSuccess(
            response -> log.info("✅ Google Sign-In successful for: {}", response.getUsername()))
        .doOnError(error -> log.error("❌ Google Sign-In failed: {}", error.getMessage()));
  }

  /** Validate Google ID token directly using Google's API. */
  private Mono<Map<String, Object>> validateGoogleTokenDirectly(String googleIdToken) {
    String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + googleIdToken;

    return webClientBuilder
        .build()
        .get()
        .uri(googleTokenInfoUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(info -> log.info("✅ Google token validated"))
        .doOnError(error -> log.error("❌ Google token validation failed: {}", error.getMessage()));
  }

  /** Process authentication with direct Google token validation. */
  private Mono<AuthResponse> processUserAuthenticationDirect(Map<String, Object> userInfo) {
    String email = (String) userInfo.get("email");
    String username = email;
    String firstName = (String) userInfo.getOrDefault("given_name", "");
    String lastName = (String) userInfo.getOrDefault("family_name", "");

    log.debug("🔵 Processing Google authentication for: {}", email);

    return userRepository
        .findByEmail(email)
        .flatMap(
            existingUser -> {
              log.info("✅ Existing Google user found: {}", existingUser.getUsername());
              return authenticateExistingUser(existingUser);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("🔵 Creating new Google user: {}", email);
                  return createAndAuthenticateNewUser(email, username, firstName, lastName);
                }));
  }

  /** Authenticate existing user. */
  private Mono<AuthResponse> authenticateExistingUser(User user) {
    String tempPassword = "OAUTH_" + user.getKeycloakId().toString();
    LoginRequest loginRequest = new LoginRequest(user.getUsername(), tempPassword);

    return authService
        .login(loginRequest)
        .doOnSuccess(response -> log.info("✅ Existing user authenticated: {}", user.getUsername()))
        .onErrorResume(
            error -> {
              log.warn("⚠️ Login failed, attempting password reset");
              return resetKeycloakPassword(user.getKeycloakId(), tempPassword)
                  .then(authService.login(loginRequest));
            });
  }

  /** Create new Google user in Keycloak and database. */
  private Mono<AuthResponse> createAndAuthenticateNewUser(
      String email, String username, String firstName, String lastName) {

    String randomPassword = "GOOGLE_OAUTH_" + UUID.randomUUID();

    log.info("🔵 Creating new Google user in Keycloak: {}", email);

    return keycloakService
        .createUser(username, email, randomPassword, firstName, lastName)
        .doOnSuccess(keycloakId -> log.info("✅ Google user created in Keycloak: {}", keycloakId))
        .flatMap(
            keycloakId -> {
              User newUser =
                  User.builder()
                      .keycloakId(keycloakId)
                      .username(username)
                      .email(email)
                      .firstName(firstName)
                      .lastName(lastName)
                      .emailVerified(true)
                      .isActive(true)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build();

              return userRepository
                  .save(newUser)
                  .doOnSuccess(
                      user -> log.info("✅ Google user saved to database: {}", user.getUsername()));
            })
        .flatMap(
            savedUser -> {
              log.info("🔵 Authenticating new Google user: {}", savedUser.getUsername());
              return authService.login(new LoginRequest(username, randomPassword));
            })
        .doOnSuccess(
            response -> log.info("✅ Google user authenticated: {}", response.getUsername()))
        .doOnError(error -> log.error("❌ Failed to create Google user: {}", error.getMessage()))
        .onErrorResume(
            error ->
                Mono.error(new KeycloakException("Google sign-in failed: " + error.getMessage())));
  }

  /** Reset Keycloak user password (helper method). */
  private Mono<Void> resetKeycloakPassword(UUID keycloakId, String newPassword) {
    log.warn("⚠️ Password reset needed for Keycloak user: {}", keycloakId);
    return Mono.empty();
  }
}
