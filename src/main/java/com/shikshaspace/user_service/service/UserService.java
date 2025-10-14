package com.shikshaspace.user_service.service;


import com.shikshaspace.user_service.dto.KeycloakUserData;
import com.shikshaspace.user_service.dto.UserProfileRequest;
import com.shikshaspace.user_service.dto.UserProfileResponse;
import com.shikshaspace.user_service.entity.User;
import com.shikshaspace.user_service.mapper.UserMapper;
import com.shikshaspace.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Keycloak keycloak;
    
    @Value("${keycloak.realm}")
    private String realm;

    public Mono<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        String keycloakId = extractKeycloakId(authentication);
        
        return userRepository.findByKeycloakId(keycloakId)
                .switchIfEmpty(syncUserFromKeycloak(keycloakId))
                .doOnNext(user -> user.setLastLoginAt(LocalDateTime.now()))
                .flatMap(userRepository::save)
                .map(userMapper::entityToResponse);
    }

    public Mono<UserProfileResponse> updateProfile(Authentication authentication, UserProfileRequest request) {
        String keycloakId = extractKeycloakId(authentication);
        
        return userRepository.findByKeycloakId(keycloakId)
                .switchIfEmpty(syncUserFromKeycloak(keycloakId))
                .doOnNext(user -> {
                    userMapper.updateEntityFromRequest(request, user);
                    user.setUpdatedAt(LocalDateTime.now());
                })
                .flatMap(userRepository::save)
                .map(userMapper::entityToResponse);
    }

    private Mono<User> syncUserFromKeycloak(String keycloakId) {
        return Mono.fromCallable(() -> {
            log.info("Syncing user from Keycloak: {}", keycloakId);
            
            UserRepresentation keycloakUser = keycloak.realm(realm)
                    .users()
                    .get(keycloakId)
                    .toRepresentation();
            
            KeycloakUserData userData = KeycloakUserData.builder()
                    .id(keycloakUser.getId())
                    .username(keycloakUser.getUsername())
                    .email(keycloakUser.getEmail())
                    .emailVerified(keycloakUser.isEmailVerified())
                    .firstName(keycloakUser.getFirstName())
                    .lastName(keycloakUser.getLastName())
                    .createdTimestamp(keycloakUser.getCreatedTimestamp())
                    .enabled(keycloakUser.isEnabled())
                    .build();
            
            User user = userMapper.keycloakToEntity(userData);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            return user;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(userRepository::save);
    }

    private String extractKeycloakId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
