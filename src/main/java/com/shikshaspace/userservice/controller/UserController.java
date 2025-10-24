package com.shikshaspace.userservice.controller;

import com.shikshaspace.userservice.dto.request.UpdateProfileRequest;
import com.shikshaspace.userservice.dto.response.UserResponse;
import com.shikshaspace.userservice.mapper.UserMapper;
import com.shikshaspace.userservice.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** User management REST API controller. All endpoints require JWT authentication. */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserMapper userMapper;

  /** Get all users (Admin only). */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Flux<UserResponse> getAllUsers() {
    log.info("Fetching all users");
    return userService.getAllUsers().map(userMapper::toResponse);
  }

  /** Get current authenticated user profile. */
  @GetMapping("/me")
  public Mono<UserResponse> getCurrentUser(Principal principal) {
    log.info("Fetching profile for authenticated user");

    String username = extractUsername(principal);

    return userService
        .getUserByUsername(username)
        .map(userMapper::toResponse)
        .doOnSuccess(response -> log.info("Profile fetched for user: {}", username))
        .doOnError(e -> log.error("Error fetching profile: {}", e.getMessage()));
  }

  /** Get user by ID. */
  @GetMapping("/{id}")
  public Mono<UserResponse> getUserById(@PathVariable UUID id) {
    log.info("Fetching user by ID: {}", id);
    return userService.getUserById(id).map(userMapper::toResponse);
  }

  /** Get user by email. */
  @GetMapping("/email/{email}")
  public Mono<UserResponse> getUserByEmail(@PathVariable String email) {
    log.info("Fetching user by email: {}", email);
    return userService.getUserByEmail(email).map(userMapper::toResponse);
  }

  /** Get user by username. */
  @GetMapping("/username/{username}")
  public Mono<UserResponse> getUserByUsername(@PathVariable String username) {
    log.info("Fetching user by username: {}", username);
    return userService.getUserByUsername(username).map(userMapper::toResponse);
  }

  /** Update user profile. Users can only update their own profile unless they have ADMIN role. */
  @PutMapping("/{id}")
  public Mono<UserResponse> updateProfile(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProfileRequest request,
      Principal principal) {

    log.info("Updating profile for user ID: {}", id);

    return userService
        .updateProfile(id, request)
        .map(userMapper::toResponse)
        .doOnSuccess(response -> log.info("Profile updated for user ID: {}", id))
        .doOnError(e -> log.error("Error updating profile: {}", e.getMessage()));
  }

  /** Delete user (Admin only). */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<Void> deleteUser(@PathVariable UUID id) {
    log.info("Deleting user ID: {}", id);
    return userService.deleteUser(id).doOnSuccess(v -> log.info("User deleted: {}", id));
  }

  /** Extract username from JWT principal. */
  private String extractUsername(Principal principal) {
    if (principal instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String username = jwt.getClaimAsString("preferred_username");
      return username != null ? username : jwt.getSubject();
    }
    return principal.getName();
  }
}
