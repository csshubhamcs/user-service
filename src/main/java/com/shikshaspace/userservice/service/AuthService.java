package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.response.UserResponse;
import com.shikshaspace.userservice.mapper.UserMapper;
import com.shikshaspace.userservice.security.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public Mono<UserResponse> register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        return userService.registerUser(request)
                .map(userMapper::toResponse)
                .doOnSuccess(user -> log.info("User registered: {}", user.getUsername()));
    }

    public Mono<TokenResponse> login(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());

        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());

        return webClientBuilder.build()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .doOnSuccess(token -> log.info("User logged in: {}", request.getUsername()))
                .doOnError(e -> log.error("Login failed for user: {}", request.getUsername(), e));
    }

    /**
     * Refresh access token using refresh token
     */
    public Mono<TokenResponse> refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token");

        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        return webClientBuilder.build()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.info("Access token refreshed successfully"))
                .doOnError(error -> log.error("Failed to refresh token: {}", error.getMessage()));
    }
}
