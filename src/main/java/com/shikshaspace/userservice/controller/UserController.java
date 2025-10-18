package com.shikshaspace.userservice.controller;

import com.shikshaspace.userservice.dto.request.UpdateProfileRequest;
import com.shikshaspace.userservice.dto.response.UserResponse;
import com.shikshaspace.userservice.mapper.UserMapper;
import com.shikshaspace.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userService.getAllUsers()
                .map(userMapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<UserResponse> getUserById(@PathVariable UUID id) {
        log.debug("Fetching user by ID: {}", id);
        return userService.getUserById(id)
                .map(userMapper::toResponse);
    }

    @GetMapping("/email/{email}")
    public Mono<UserResponse> getUserByEmail(@PathVariable String email) {
        log.debug("Fetching user by email: {}", email);
        return userService.getUserByEmail(email)
                .map(userMapper::toResponse);
    }

    @GetMapping("/username/{username}")
    public Mono<UserResponse> getUserByUsername(@PathVariable String username) {
        log.debug("Fetching user by username: {}", username);
        return userService.getUserByUsername(username)
                .map(userMapper::toResponse);
    }

    @PutMapping("/{id}")
    public Mono<UserResponse> updateProfile(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        log.debug("Updating profile for user ID: {}", id);
        return userService.updateProfile(id, request)
                .map(userMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteUser(@PathVariable UUID id) {
        log.debug("Deleting user ID: {}", id);
        return userService.deleteUser(id);
    }
}
