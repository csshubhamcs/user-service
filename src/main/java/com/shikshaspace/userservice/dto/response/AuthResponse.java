package com.shikshaspace.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;           // access_token from Keycloak
    private String refreshToken;     // refresh_token from Keycloak
    private Long expiresIn;         // expires_in from Keycloak
    private UUID userId;            // from database
    private String username;        // from database
    private String email;
}
