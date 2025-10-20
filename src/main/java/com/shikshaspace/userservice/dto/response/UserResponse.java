package com.shikshaspace.userservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  private UUID id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;

  private Integer age;
  private String bio;
  private Double experience;

  private String profileImageUrl;
  private String linkedinUrl;
  private String githubUrl;

  private Boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
