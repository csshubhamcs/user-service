package com.shikshaspace.userservice.controller;

import com.shikshaspace.userservice.dto.request.GoogleSignInRequest;
import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.request.RefreshTokenRequest;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.response.AuthResponse;
import com.shikshaspace.userservice.dto.response.TokenResponse;
import com.shikshaspace.userservice.service.AuthService;
import com.shikshaspace.userservice.service.OAuth2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Authentication REST API controller. Handles user registration, login, token refresh, and OAuth2
 * authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final OAuth2Service oAuth2Service;

  /** Register new user with username/password. Auto-login after successful registration. */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    log.info("Registration request for username: {}", request.getUsername());
    return authService.register(request);
  }

  /** Login with username/password. */
  @PostMapping("/login")
  public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    log.info("Login request for username: {}", request.getUsername());
    return authService.login(request);
  }

  /** Google Sign-In authentication flow. Creates user if not exists, logs in if exists. */
  @PostMapping("/oauth2/google")
  public Mono<AuthResponse> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
    log.info("Google Sign-In request received");
    return oAuth2Service.handleGoogleSignIn(request.getGoogleIdToken());
  }

  /** Refresh access token using refresh token. */
  @PostMapping("/refresh")
  public Mono<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    log.info("Token refresh request received");
    return authService.refreshAccessToken(request.getRefreshToken());
  }
}
