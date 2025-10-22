package com.shikshaspace.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterExternalRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  // No password – external provider handles auth

  @NotBlank(message = "First name is required")
  private String firstName;

  @NotBlank(message = "Last name is required")
  private String lastName;

  @NotBlank(message = "Keycloak ID is required for external registration")
  private UUID keycloakId; // From Google/Keycloak sub
}
