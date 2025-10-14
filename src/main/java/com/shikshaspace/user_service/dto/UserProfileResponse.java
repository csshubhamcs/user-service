package com.shikshaspace.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    
    private Long id;
    private String keycloakId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String bio;
    private String profileImageUrl;
    
    // ShikshaSpace fields
    private List<String> technologyTags;
    private Integer experienceYears;
    private List<String> skills;
    private String designation;
    private String company;
    
    // Status
    private Boolean isActive;
    private Boolean isProfileComplete;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
