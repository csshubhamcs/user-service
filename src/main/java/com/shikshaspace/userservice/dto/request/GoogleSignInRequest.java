package com.shikshaspace.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for Google Sign-In authentication. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleSignInRequest {

  @NotBlank(message = "Google ID token is required")
  private String googleIdToken;
}
