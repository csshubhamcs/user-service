package com.shikshaspace.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakUserData {
    private String id;
    private String username;
    private String email;
    private Boolean emailVerified;
    private String firstName;
    private String lastName;
    private Long createdTimestamp;
    private Boolean enabled;
}
