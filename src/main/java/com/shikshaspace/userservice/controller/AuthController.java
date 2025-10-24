package com.shikshaspace.userservice.controller;

import com.shikshaspace.userservice.dto.request.GoogleSignInRequest;
import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.request.RefreshTokenRequest;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.response.AuthResponse;
import com.shikshaspace.userservice.service.AuthService;
import com.shikshaspace.userservice.service.OAuth2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Production-grade authentication controller with comprehensive logging. */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final OAuth2Service oAuth2Service;

  @PostMapping("/register")
  public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
    log.info("🔵 Registration request received for: {}", request.getUsername());

    return authService
        .register(request)
        .map(
            response -> {
              log.info("✅ Registration successful: {}", response.getUsername());
              return ResponseEntity.ok(response);
            })
        .doOnError(error -> log.error("❌ Registration failed: {}", error.getMessage(), error))
        .onErrorResume(
            error -> {
              log.error("❌ Registration error: {}", error.getMessage());
              return Mono.just(ResponseEntity.badRequest().build());
            });
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    log.info("🔵 Login request received for: {}", request.getUsername());

    return authService
        .login(request)
        .map(
            response -> {
              log.info("✅ Login successful: {}", response.getUsername());
              return ResponseEntity.ok(response);
            })
        .doOnError(error -> log.error("❌ Login failed: {}", error.getMessage()))
        .onErrorResume(error -> Mono.just(ResponseEntity.status(401).build()));
  }

  @PostMapping("/oauth2/google")
  public Mono<ResponseEntity<AuthResponse>> googleSignIn(
      @Valid @RequestBody GoogleSignInRequest request) {
    log.info("🔵 Google Sign-In request received");

    return oAuth2Service
        .handleGoogleSignIn(request.getGoogleIdToken())
        .map(
            response -> {
              log.info("✅ Google Sign-In successful: {}", response.getUsername());
              return ResponseEntity.ok(response);
            })
        .doOnError(error -> log.error("❌ Google Sign-In failed: {}", error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/refresh")
  public Mono<ResponseEntity<AuthResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request) {
    log.debug("🔵 Token refresh request received");

    return authService
        .refreshToken(request)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.status(401).build());
  }
}
