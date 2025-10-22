package com.shikshaspace.userservice.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

  @Id private UUID id;

  private UUID keycloakId;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private Boolean emailVerified;

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
