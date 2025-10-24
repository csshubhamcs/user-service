package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.domain.User;
import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.request.RefreshTokenRequest;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.response.AuthResponse;
import com.shikshaspace.userservice.dto.response.TokenResponse;
import com.shikshaspace.userservice.exception.KeycloakException;
import com.shikshaspace.userservice.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
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
 * Production-grade authentication service. Handles user registration, login, and token management
 * with Keycloak integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

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

  /** Register new user - creates in both Keycloak and database. */
  @Transactional
  public Mono<AuthResponse> register(RegisterRequest request) {
    log.info("🔵 Registering new user: {}", request.getUsername());

    // Step 1: Create user in Keycloak
    return keycloakService
        .createUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            request.getFirstName(),
            request.getLastName())
        .doOnSuccess(keycloakId -> log.info("✅ User created in Keycloak with ID: {}", keycloakId))
        .flatMap(
            keycloakId -> {
              // Step 2: Create user in database
              User newUser =
                  User.builder()
                      .keycloakId(keycloakId)
                      .username(request.getUsername())
                      .email(request.getEmail())
                      .firstName(request.getFirstName())
                      .lastName(request.getLastName())
                      .emailVerified(false)
                      .isActive(true)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build();

              return userRepository
                  .save(newUser)
                  .doOnSuccess(
                      user -> log.info("✅ User saved to database: {}", user.getUsername()));
            })
        .flatMap(
            user -> {
              // Step 3: Auto-login after registration
              log.info("🔵 Auto-login after registration for: {}", user.getUsername());
              return login(new LoginRequest(request.getUsername(), request.getPassword()));
            })
        .doOnError(error -> log.error("❌ Registration failed: {}", error.getMessage()))
        .onErrorResume(
            error ->
                Mono.error(new KeycloakException("Registration failed: " + error.getMessage())));
  }

  /** Login user - authenticate with Keycloak and return tokens. */
  public Mono<AuthResponse> login(LoginRequest request) {
    log.info("🔵 Login request for user: {}", request.getUsername());

    return authenticateWithKeycloak(request.getUsername(), request.getPassword())
        .flatMap(
            tokenResponse ->
                userRepository
                    .findByUsername(request.getUsername())
                    .map(
                        user ->
                            AuthResponse.builder()
                                .token(tokenResponse.getAccessToken())
                                .refreshToken(tokenResponse.getRefreshToken())
                                .expiresIn(tokenResponse.getExpiresIn())
                                .userId(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .build()))
        .doOnSuccess(response -> log.info("✅ Login successful for: {}", response.getUsername()))
        .doOnError(error -> log.error("❌ Login failed: {}", error.getMessage()));
  }

  /** Refresh access token using refresh token. */
  public Mono<AuthResponse> refreshToken(RefreshTokenRequest request) {
    log.debug("Refreshing access token");

    return refreshWithKeycloak(request.getRefreshToken())
        .flatMap(
            tokenResponse -> {
              // Extract username from token to get user details
              return extractUserInfoFromToken(tokenResponse.getAccessToken())
                  .flatMap(
                      userInfo -> {
                        String username = (String) userInfo.get("preferred_username");
                        return userRepository
                            .findByUsername(username)
                            .map(
                                user ->
                                    AuthResponse.builder()
                                        .token(tokenResponse.getAccessToken())
                                        .refreshToken(tokenResponse.getRefreshToken())
                                        .expiresIn(tokenResponse.getExpiresIn())
                                        .userId(user.getId())
                                        .username(user.getUsername())
                                        .email(user.getEmail())
                                        .build());
                      });
            })
        .doOnSuccess(response -> log.info("✅ Token refreshed successfully"))
        .doOnError(error -> log.error("❌ Token refresh failed: {}", error.getMessage()));
  }

  /** Authenticate with Keycloak and get tokens. */
  private Mono<TokenResponse> authenticateWithKeycloak(String username, String password) {
    String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "password");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("username", username);
    formData.add("password", password);

    return webClientBuilder
        .build()
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(formData)
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .timeout(Duration.ofSeconds(10));
  }

  /** Refresh token with Keycloak. */
  private Mono<TokenResponse> refreshWithKeycloak(String refreshToken) {
    String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "refresh_token");
    formData.add("client_id", clientId);
    formData.add("client_secret", clientSecret);
    formData.add("refresh_token", refreshToken);

    return webClientBuilder
        .build()
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(formData)
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .timeout(Duration.ofSeconds(10));
  }

  /** Extract user info from JWT access token. */
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
}
