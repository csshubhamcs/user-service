package com.shikshaspace.userservice.repository;

import com.shikshaspace.userservice.domain.User;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

  Mono<User> findByEmail(String email);

  Mono<User> findByUsername(String username);

  Mono<User> findByKeycloakId(UUID keycloakId);

  Mono<Boolean> existsByEmail(String email);

  Mono<Boolean> existsByUsername(String username);
}
