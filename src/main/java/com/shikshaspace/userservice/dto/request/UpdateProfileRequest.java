package com.shikshaspace.userservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user profile. All fields are optional - only provided fields will be
 * updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

  private String firstName;

  private String lastName;

  @Min(value = 0, message = "Age must be positive")
  @Max(value = 150, message = "Age must be less than 150")
  private Integer age;

  @Size(max = 5000, message = "Bio must not exceed 5000 characters")
  private String bio;

  @Min(value = 0, message = "Experience must be positive")
  private Double experience;

  private String profileImageUrl;

  private String linkedinUrl;

  private String githubUrl;
}
