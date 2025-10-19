package com.shikshaspace.userservice.controller;

import com.shikshaspace.userservice.dto.request.UpdateProfileRequest;
import com.shikshaspace.userservice.dto.response.UserResponse;
import com.shikshaspace.userservice.mapper.UserMapper;
import com.shikshaspace.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.UUID;

/**
 * REST API for user management
 * All endpoints require JWT authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userService.getAllUsers()
                .map(userMapper::toResponse);
    }

    /**
     * Get current authenticated user profile
     * Extracts username from JWT token
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getCurrentUser(Principal principal) {
        log.debug("Fetching profile for authenticated user");

        if (principal == null) {
            log.warn("Unauthorized access attempt to /me endpoint");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        // Extract username from JWT principal
        String username = extractUsername(principal);

        if (username == null) {
            log.error("Failed to extract username from principal");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        log.debug("Extracted username from JWT: {}", username);

        return userService.getUserByUsername(username)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnSuccess(response -> log.info("Profile fetched for user: {}", username))
                .doOnError(e -> log.error("Error fetching profile for user: {}", username, e));
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> getUserById(@PathVariable UUID id) {
        log.debug("Fetching user by ID: {}", id);

        return userService.getUserById(id)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    public Mono<ResponseEntity<UserResponse>> getUserByEmail(@PathVariable String email) {
        log.debug("Fetching user by email: {}", email);

        return userService.getUserByEmail(email)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public Mono<ResponseEntity<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.debug("Fetching user by username: {}", username);

        return userService.getUserByUsername(username)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * Update user profile
     * Users can only update their own profile unless ADMIN
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserResponse>> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {

        log.debug("Updating profile for user ID: {}", id);

        return userService.updateProfile(id, request)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnSuccess(response -> log.info("Profile updated for user ID: {}", id))
                .doOnError(e -> log.error("Error updating profile for user ID: {}", id, e));
    }

    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteUser(@PathVariable UUID id) {
        log.debug("Deleting user ID: {}", id);
        return userService.deleteUser(id)
                .doOnSuccess(v -> log.info("User deleted: {}", id));
    }

    /**
     * Extract username from JWT principal
     * Supports both JwtAuthenticationToken and standard Authentication
     */
    private String extractUsername(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            // Try preferred_username first (Keycloak default)
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null) {
                return username;
            }
            // Fallback to sub claim
            return jwt.getSubject();
        }

        if (principal instanceof Authentication auth) {
            return auth.getName();
        }

        // Last resort - use principal name directly
        return principal.getName();
    }
}
