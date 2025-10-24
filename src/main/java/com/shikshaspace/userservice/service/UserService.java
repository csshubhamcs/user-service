package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.domain.User;
import com.shikshaspace.userservice.dto.request.RegisterRequest;
import com.shikshaspace.userservice.dto.request.UpdateProfileRequest;
import com.shikshaspace.userservice.exception.UserNotFoundException;
import com.shikshaspace.userservice.mapper.UserMapper;
import com.shikshaspace.userservice.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Core user management service. Handles user CRUD operations and profile management. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final KeycloakService keycloakService;
  private final UserMapper userMapper;

  /** Register new user in Keycloak and local database. */
  @Transactional
  public Mono<User> registerUser(RegisterRequest request) {
    log.info("Registering user: {}", request.getUsername());

    return keycloakService
        .createUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            request.getFirstName(),
            request.getLastName())
        .flatMap(
            keycloakId -> {
              User user = userMapper.toEntity(request);
              user.setKeycloakId(keycloakId);
              user.setCreatedAt(LocalDateTime.now());
              user.setUpdatedAt(LocalDateTime.now());

              return userRepository.save(user);
            })
        .doOnSuccess(user -> log.info("User registered successfully: {}", user.getId()))
        .doOnError(error -> log.error("User registration failed: {}", error.getMessage()));
  }

  /** Get user by ID. */
  public Mono<User> getUserById(UUID id) {
    log.debug("Fetching user by ID: {}", id);
    return userRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + id)));
  }

  /** Get user by email. */
  public Mono<User> getUserByEmail(String email) {
    log.debug("Fetching user by email: {}", email);
    return userRepository
        .findByEmail(email)
        .switchIfEmpty(
            Mono.error(new UserNotFoundException("User not found with email: " + email)));
  }

  /** Get user by username. */
  public Mono<User> getUserByUsername(String username) {
    log.debug("Fetching user by username: {}", username);
    return userRepository
        .findByUsername(username)
        .switchIfEmpty(
            Mono.error(new UserNotFoundException("User not found with username: " + username)));
  }

  /** Get all users (admin only). */
  public Flux<User> getAllUsers() {
    log.debug("Fetching all users");
    return userRepository.findAll();
  }

  /** Update user profile. */
  @Transactional
  public Mono<User> updateProfile(UUID id, UpdateProfileRequest request) {
    log.info("Updating profile for user: {}", id);

    return userRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + id)))
        .flatMap(
            user -> {
              userMapper.updateEntity(request, user);
              user.setUpdatedAt(LocalDateTime.now());
              return userRepository.save(user);
            })
        .doOnSuccess(user -> log.info("Profile updated successfully: {}", user.getId()))
        .doOnError(error -> log.error("Profile update failed: {}", error.getMessage()));
  }

  /** Delete user from both Keycloak and database. */
  @Transactional
  public Mono<Void> deleteUser(UUID id) {
    log.info("Deleting user: {}", id);

    return userRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new UserNotFoundException("User not found with ID: " + id)))
        .flatMap(
            user ->
                keycloakService
                    .deleteUser(user.getKeycloakId())
                    .then(userRepository.deleteById(id)))
        .doOnSuccess(v -> log.info("User deleted successfully: {}", id))
        .doOnError(error -> log.error("User deletion failed: {}", error.getMessage()));
  }
}
