package com.shikshaspace.user_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequest {
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100)
    private String lastName;
    
    private String mobileNumber;
    private String bio;
    private String profileImageUrl;
    
    // ShikshaSpace fields
    private List<String> technologyTags;
    
    @Min(value = 0)
    @Max(value = 50)
    private Integer experienceYears;
    
    private List<String> skills;
    private String designation;
    private String company;
}
