package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.exception.KeycloakException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for Keycloak admin operations. Handles user creation, deletion, and synchronization with
 * Keycloak.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

  private final Keycloak keycloak;

  @Value("${keycloak.realm}")
  private String realm;

  /** Create user in Keycloak with credentials. */
  public Mono<UUID> createUser(
      String username, String email, String password, String firstName, String lastName) {
    return Mono.fromCallable(
            () -> {
              log.info("Creating Keycloak user: {}", username);

              RealmResource realmResource = keycloak.realm(realm);
              UsersResource usersResource = realmResource.users();

              UserRepresentation user = new UserRepresentation();
              user.setUsername(username);
              user.setEmail(email);
              user.setFirstName(firstName);
              user.setLastName(lastName);
              user.setEnabled(true);
              user.setEmailVerified(false);

              CredentialRepresentation credential = new CredentialRepresentation();
              credential.setType(CredentialRepresentation.PASSWORD);
              credential.setValue(password);
              credential.setTemporary(false);
              user.setCredentials(Collections.singletonList(credential));

              Response response = usersResource.create(user);

              if (response.getStatus() != 201) {
                throw new KeycloakException("Failed to create user: " + response.getStatusInfo());
              }

              String location = response.getLocation().getPath();
              String keycloakId = location.substring(location.lastIndexOf('/') + 1);

              log.info("Keycloak user created with ID: {}", keycloakId);
              return UUID.fromString(keycloakId);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> log.error("Error creating Keycloak user: {}", error.getMessage()));
  }

  /** Find Keycloak user by email. */
  public Mono<UserRepresentation> findUserByEmail(String email) {
    return Mono.fromCallable(
            () -> {
              log.debug("Searching Keycloak user by email: {}", email);

              RealmResource realmResource = keycloak.realm(realm);
              UsersResource usersResource = realmResource.users();

              List<UserRepresentation> users = usersResource.search(email, true);

              if (users.isEmpty()) {
                log.debug("No Keycloak user found with email: {}", email);
                return null;
              }

              log.debug("Found Keycloak user: {}", users.get(0).getUsername());
              return users.get(0);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> log.error("Error searching Keycloak user: {}", error.getMessage()));
  }

  /** Delete user from Keycloak. */
  public Mono<Void> deleteUser(UUID keycloakId) {
    return Mono.fromRunnable(
            () -> {
              log.info("Deleting Keycloak user: {}", keycloakId);
              keycloak.realm(realm).users().delete(keycloakId.toString());
              log.info("Keycloak user deleted: {}", keycloakId);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then()
        .doOnError(error -> log.error("Error deleting Keycloak user: {}", error.getMessage()));
  }
}
