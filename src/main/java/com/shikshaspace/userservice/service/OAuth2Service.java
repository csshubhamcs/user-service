package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.domain.User;
import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.response.AuthResponse;
import com.shikshaspace.userservice.dto.response.TokenResponse;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Production-grade OAuth2 service for Google Sign-In integration. Supports both Keycloak token
 * exchange and direct Google token validation.
 *
 * <p>Flow: 1. Try Keycloak token exchange (if Google IDP configured) 2. Fallback to direct Google
 * token validation 3. Create user if new, authenticate if exists 4. Return JWT tokens for session
 * management
 */
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

  /**
   * Main entry point for Google Sign-In authentication. Handles both new user registration and
   * existing user login.
   */
  @Transactional
  public Mono<AuthResponse> handleGoogleSignIn(String googleIdToken) {
    log.info("Processing Google Sign-In request");

    return exchangeGoogleTokenWithKeycloak(googleIdToken)
        .flatMap(
            tokenResponse -> {
              // Token exchange successful - use Keycloak tokens
              log.info("✅ Using Keycloak token exchange");
              return extractUserInfoFromToken(tokenResponse.getAccessToken())
                  .flatMap(userInfo -> processUserAuthentication(userInfo, tokenResponse));
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  // Token exchange failed - validate Google token directly
                  log.info("⚠️ Keycloak token exchange not available, using direct validation");
                  return validateGoogleTokenDirectly(googleIdToken)
                      .flatMap(this::processUserAuthenticationDirect);
                }))
        .doOnSuccess(
            response ->
                log.info("✅ Google Sign-In successful for user: {}", response.getUsername()))
        .doOnError(error -> log.error("❌ Google Sign-In failed: {}", error.getMessage()));
  }

  /**
   * Exchange Google ID token with Keycloak access token. Requires Keycloak Google Identity Provider
   * to be configured.
   */
  private Mono<TokenResponse> exchangeGoogleTokenWithKeycloak(String googleIdToken) {
    String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("subject_token", googleIdToken);
    formData.add("subject_issuer", "google");
    formData.add("subject_token_type", "urn:ietf:params:oauth:token-type:id_token");

    return webClientBuilder
        .build()
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(formData)
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .timeout(Duration.ofSeconds(10))
        .onErrorResume(
            error -> {
              log.debug("Token exchange not configured: {}", error.getMessage());
              return Mono.empty(); // Fall back to direct validation
            })
        .doOnSuccess(
            response -> {
              if (response != null) {
                log.info("✅ Keycloak token exchange successful");
              }
            });
  }

  /** Extract user information from Keycloak JWT access token. */
  private Mono<Map<String, Object>> extractUserInfoFromToken(String accessToken) {
    String userInfoUrl =
        keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";

    return webClientBuilder
        .build()
        .get()
        .uri(userInfoUrl)
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .timeout(Duration.ofSeconds(5));
  }

  /** Process authentication using Keycloak tokens. */
  private Mono<AuthResponse> processUserAuthentication(
      Map<String, Object> userInfo, TokenResponse tokenResponse) {

    String email = (String) userInfo.get("email");
    String username = (String) userInfo.getOrDefault("preferred_username", email);
    String firstName = (String) userInfo.getOrDefault("given_name", "");
    String lastName = (String) userInfo.getOrDefault("family_name", "");
    String keycloakSubject = (String) userInfo.get("sub");

    log.debug("Processing Keycloak authentication for: {}", email);

    return userRepository
        .findByEmail(email)
        .flatMap(
            existingUser -> {
              log.info("Existing user logged in: {}", existingUser.getUsername());
              return buildAuthResponse(existingUser, tokenResponse);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("Creating new user from Keycloak: {}", email);
                  return createNewOAuth2User(
                      email,
                      username,
                      firstName,
                      lastName,
                      UUID.fromString(keycloakSubject),
                      tokenResponse);
                }));
  }

  /**
   * Validate Google ID token directly using Google's API. Use this when Keycloak Google IDP is not
   * configured.
   */
  private Mono<Map<String, Object>> validateGoogleTokenDirectly(String googleIdToken) {
    String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + googleIdToken;

    return webClientBuilder
        .build()
        .get()
        .uri(googleTokenInfoUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .timeout(Duration.ofSeconds(10))
        .doOnSuccess(info -> log.info("✅ Google token validated directly"))
        .doOnError(error -> log.error("❌ Google token validation failed: {}", error.getMessage()));
  }

  /**
   * Process authentication with direct Google token validation. Creates user in Keycloak and DB,
   * then generates tokens.
   */
  private Mono<AuthResponse> processUserAuthenticationDirect(Map<String, Object> userInfo) {
    String email = (String) userInfo.get("email");
    String username = email; // Use email as username
    String firstName = (String) userInfo.getOrDefault("given_name", "");
    String lastName = (String) userInfo.getOrDefault("family_name", "");

    log.debug("Processing direct Google authentication for: {}", email);

    return userRepository
        .findByEmail(email)
        .flatMap(
            existingUser -> {
              log.info("Existing user found: {}", existingUser.getUsername());
              // Authenticate existing user via Keycloak
              return authenticateExistingUser(existingUser);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("Creating new user from Google: {}", email);
                  return createAndAuthenticateNewUser(email, username, firstName, lastName);
                }));
  }

  /** Authenticate existing user by generating Keycloak tokens. */
  private Mono<AuthResponse> authenticateExistingUser(User user) {
    // Generate a temporary password for Keycloak authentication
    String tempPassword = "OAUTH_" + user.getKeycloakId().toString();

    LoginRequest loginRequest = new LoginRequest(user.getUsername(), tempPassword);

    return authService
        .login(loginRequest)
        .onErrorResume(
            error -> {
              // If login fails, try to reset password in Keycloak
              log.warn("Login failed, attempting password reset for OAuth user");
              return resetKeycloakPassword(user.getKeycloakId(), tempPassword)
                  .then(authService.login(loginRequest));
            })
        .doOnSuccess(response -> log.info("✅ User authenticated: {}", user.getUsername()));
  }

  /** Create new user in Keycloak and database, then authenticate. */
  private Mono<AuthResponse> createAndAuthenticateNewUser(
      String email, String username, String firstName, String lastName) {

    String randomPassword = "GOOGLE_OAUTH_" + UUID.randomUUID();

    return keycloakService
        .createUser(username, email, randomPassword, firstName, lastName)
        .flatMap(
            keycloakId -> {
              User newUser =
                  User.builder()
                      .keycloakId(keycloakId)
                      .username(username)
                      .email(email)
                      .firstName(firstName)
                      .lastName(lastName)
                      .emailVerified(true) // Google verifies emails
                      .isActive(true)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build();

              return userRepository
                  .save(newUser)
                  .flatMap(
                      savedUser -> {
                        log.info("✅ User created in database: {}", savedUser.getUsername());
                        // Authenticate the newly created user
                        return authService.login(new LoginRequest(username, randomPassword));
                      });
            })
        .doOnSuccess(response -> log.info("✅ New user created and authenticated: {}", username))
        .onErrorResume(
            error -> {
              log.error("❌ Failed to create user: {}", error.getMessage());
              return Mono.error(
                  new KeycloakException("User registration failed: " + error.getMessage()));
            });
  }

  /** Create new OAuth2 user with Keycloak tokens. */
  private Mono<AuthResponse> createNewOAuth2User(
      String email,
      String username,
      String firstName,
      String lastName,
      UUID keycloakId,
      TokenResponse tokenResponse) {

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
        .flatMap(
            savedUser -> {
              log.info("✅ OAuth2 user created: {}", savedUser.getUsername());
              return buildAuthResponse(savedUser, tokenResponse);
            })
        .onErrorResume(
            error -> {
              log.error("❌ Failed to create OAuth2 user: {}", error.getMessage());
              return Mono.error(new KeycloakException("User registration failed"));
            });
  }

  /** Build AuthResponse from user and Keycloak tokens. */
  private Mono<AuthResponse> buildAuthResponse(User user, TokenResponse tokenResponse) {
    return Mono.just(
        AuthResponse.builder()
            .token(tokenResponse.getAccessToken())
            .refreshToken(tokenResponse.getRefreshToken())
            .expiresIn(tokenResponse.getExpiresIn())
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .build());
  }

  /** Reset Keycloak user password (helper method for OAuth users). */
  private Mono<Void> resetKeycloakPassword(UUID keycloakId, String newPassword) {
    // This would require additional Keycloak admin client methods
    // For now, just log and continue
    log.warn("Password reset needed for Keycloak user: {}", keycloakId);
    return Mono.empty();
  }
}
