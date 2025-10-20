package com.shikshaspace.userservice.controller;

import com.shikshaspace.userservice.dto.request.LoginRequest;
import com.shikshaspace.userservice.dto.request.RefreshTokenRequest;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.response.AuthResponse; // ← Changed
import com.shikshaspace.userservice.security.TokenResponse;
import com.shikshaspace.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { // ← Changed
    log.debug("Register request for username: {}", request.getUsername());
    return authService.register(request);
  }

  @PostMapping("/login")
  public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) { // ← Changed
    log.debug("Login request for username: {}", request.getUsername());
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public Mono<ResponseEntity<TokenResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request) {

    log.info("Token refresh request received");

    return authService
        .refreshAccessToken(request.getRefreshToken())
        .map(ResponseEntity::ok)
        .onErrorResume(
            error -> {
              log.error("Token refresh failed: {}", error.getMessage());
              return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            });
  }
}
