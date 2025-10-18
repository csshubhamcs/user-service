package com.shikshaspace.userservice.service;

import com.shikshaspace.userservice.exception.KeycloakException;
import jakarta.ws.rs.core.Response;
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

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public Mono<UUID> createUser(String username, String email, String password,
                                 String firstName, String lastName) {
        return Mono.fromCallable(() -> {
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
                        throw new KeycloakException("Failed to create user in Keycloak: " + response.getStatusInfo());
                    }

                    String location = response.getLocation().getPath();
                    String keycloakId = location.substring(location.lastIndexOf('/') + 1);

                    log.info("Keycloak user created with ID: {}", keycloakId);
                    return UUID.fromString(keycloakId);

                }).subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Error creating Keycloak user: {}", e.getMessage(), e));
    }

    public Mono<Void> deleteUser(UUID keycloakId) {
        return Mono.fromRunnable(() -> {
                    log.info("Deleting Keycloak user: {}", keycloakId);

                    keycloak.realm(realm)
                            .users()
                            .delete(keycloakId.toString());

                    log.info("Keycloak user deleted: {}", keycloakId);

                }).subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnError(e -> log.error("Error deleting Keycloak user: {}", e.getMessage(), e));
    }
}
