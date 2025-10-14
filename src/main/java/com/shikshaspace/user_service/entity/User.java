package com.shikshaspace.user_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User {

    @Id
    private Long id;
    
    private String keycloakId;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String profileImageUrl;
    private String bio;
    
    // ShikshaSpace specific
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
