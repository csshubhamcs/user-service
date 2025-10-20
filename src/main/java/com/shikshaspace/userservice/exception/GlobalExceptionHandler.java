package com.shikshaspace.userservice.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for reactive REST APIs Handles security, validation, and runtime
 * exceptions
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle Spring Security 6 AuthorizationDeniedException Thrown when @PreAuthorize fails (e.g.,
   * missing ADMIN role)
   */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleAuthorizationDenied(
      AuthorizationDeniedException ex) {
    log.warn("Authorization denied: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied. Insufficient permissions.");
  }

  /** Handle legacy AccessDeniedException (Spring Security 5 compatibility) */
  @ExceptionHandler(AccessDeniedException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied. Insufficient permissions.");
  }

  /** Handle authentication failures (invalid/missing JWT token) */
  @ExceptionHandler(AuthenticationException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleAuthenticationException(
      AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getMessage());
    return createErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
        "Authentication required. Please provide a valid token.");
  }

  /** Handle validation errors (@Valid annotation failures) */
  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleValidationErrors(
      WebExchangeBindException ex) {
    log.error("Validation error: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "VALIDATION_ERROR");
    errorResponse.put("message", "Invalid request data");
    errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
    errorResponse.put("timestamp", LocalDateTime.now().toString());
    errorResponse.put(
        "errors",
        ex.getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList());

    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
  }

  /** Handle malformed request body */
  @ExceptionHandler(ServerWebInputException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleServerWebInputException(
      ServerWebInputException ex) {
    log.error("Invalid request: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request format");
  }

  /** Handle custom application exceptions */
  @ExceptionHandler(KeycloakException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleKeycloakException(KeycloakException ex) {
    log.error("Keycloak error: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "KEYCLOAK_ERROR", ex.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleResourceNotFound(
      ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
  }

  /**
   * Handle WebClient 401 Unauthorized from Keycloak This catches authentication failures when
   * calling Keycloak token endpoint MUST come before generic WebClientResponseException handler
   */
  @ExceptionHandler(WebClientResponseException.Unauthorized.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleWebClientUnauthorized(
      WebClientResponseException.Unauthorized ex) {
    log.warn("Authentication failed - Invalid credentials: {}", ex.getMessage());

    return createErrorResponse(
        HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid username or password");
  }

  /**
   * Handle other WebClient errors (400, 404, 500, etc.) MUST come before generic Exception handler
   */
  @ExceptionHandler(WebClientResponseException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleWebClientException(
      WebClientResponseException ex) {
    log.error("WebClient error - Status {}: {}", ex.getStatusCode(), ex.getMessage());

    // Convert HttpStatusCode to HttpStatus
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

    return createErrorResponse(
        status, "EXTERNAL_SERVICE_ERROR", "Error communicating with authentication service");
  }

  /** Handle all other unexpected exceptions This is the catch-all handler (must be last) */
  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);

    return createErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");
  }

  /** Helper method to create consistent error responses */
  private Mono<ResponseEntity<Map<String, Object>>> createErrorResponse(
      HttpStatus status, String error, String message) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", error);
    errorResponse.put("message", message);
    errorResponse.put("status", status.value());
    errorResponse.put("timestamp", LocalDateTime.now().toString());

    return Mono.just(ResponseEntity.status(status).body(errorResponse));
  }
}
