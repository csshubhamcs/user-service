package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.domain.User;
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
 * Handles OAuth2 authentication flow with external providers (Google). Automatically creates users
 * in Keycloak and local DB if they don't exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

  private final UserRepository userRepository;
  private final KeycloakService keycloakService;
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
   * Handles Google Sign-In flow with automatic user registration.
   *
   * <p>Flow: 1. Exchange Google token with Keycloak 2. Extract user info from token 3. Check if
   * user exists in DB (by email) 4. If not exists: Create in Keycloak + DB 5. Return AuthResponse
   * with tokens
   */
  @Transactional
  public Mono<AuthResponse> handleGoogleSignIn(String googleIdToken) {
    log.info("Processing Google Sign-In request");

    return exchangeGoogleTokenWithKeycloak(googleIdToken)
        .flatMap(
            tokenResponse -> {
              // Extract user info from Keycloak token
              return extractUserInfoFromToken(tokenResponse.getAccessToken())
                  .flatMap(userInfo -> processUserAuthentication(userInfo, tokenResponse));
            })
        .doOnSuccess(
            response -> log.info("Google Sign-In successful for user: {}", response.getUsername()))
        .doOnError(error -> log.error("Google Sign-In failed: {}", error.getMessage()));
  }

  /** Exchange Google ID token with Keycloak access token. */
  private Mono<TokenResponse> exchangeGoogleTokenWithKeycloak(String googleIdToken) {
    String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("subject_token", googleIdToken);
    formData.add("subject_issuer", "google");
    formData.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");

    return webClientBuilder
        .build()
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(formData)
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .timeout(Duration.ofSeconds(10))
        .doOnError(error -> log.error("Token exchange failed: {}", error.getMessage()));
  }

  /** Extract user information from Keycloak JWT token. */
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

  /** Process user authentication - create if new, login if exists. */
  private Mono<AuthResponse> processUserAuthentication(
      Map<String, Object> userInfo, TokenResponse tokenResponse) {

    String email = (String) userInfo.get("email");
    String username = (String) userInfo.getOrDefault("preferred_username", email);
    String firstName = (String) userInfo.getOrDefault("given_name", "");
    String lastName = (String) userInfo.getOrDefault("family_name", "");
    String keycloakSubject = (String) userInfo.get("sub");

    log.debug("Processing authentication for email: {}", email);

    return userRepository
        .findByEmail(email)
        .flatMap(
            existingUser -> {
              // User exists - return auth response
              log.info("Existing user logged in: {}", existingUser.getUsername());
              return buildAuthResponse(existingUser, tokenResponse);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  // New user - create in DB
                  log.info("Creating new user from Google Sign-In: {}", email);
                  return createNewOAuth2User(
                      email, username, firstName, lastName, keycloakSubject, tokenResponse);
                }));
  }

  /** Create new user from OAuth2 provider. */
  private Mono<AuthResponse> createNewOAuth2User(
      String email,
      String username,
      String firstName,
      String lastName,
      String keycloakSubject,
      TokenResponse tokenResponse) {

    User newUser =
        User.builder()
            .keycloakId(UUID.fromString(keycloakSubject))
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
              log.info("New OAuth2 user created: {}", savedUser.getUsername());
              return buildAuthResponse(savedUser, tokenResponse);
            })
        .onErrorResume(
            error -> {
              log.error("Failed to create OAuth2 user: {}", error.getMessage());
              return Mono.error(new KeycloakException("User registration failed"));
            });
  }

  /** Build AuthResponse from user and token data. */
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
}
