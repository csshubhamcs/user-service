package com.shikshaspace.userservice.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

  private String error;
  private String message;
  private int status;
  private LocalDateTime timestamp;
  private String path;
}
