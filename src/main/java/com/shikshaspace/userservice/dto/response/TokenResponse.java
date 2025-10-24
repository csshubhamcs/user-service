package com.shikshaspace.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO for Keycloak OAuth2 token endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("expires_in")
  private Long expiresIn;
}
