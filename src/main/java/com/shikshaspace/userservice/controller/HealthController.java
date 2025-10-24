package com.shikshaspace.userservice.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Health check endpoint for monitoring. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

  @GetMapping
  public Mono<ResponseEntity<Map<String, String>>> health() {
    return Mono.just(
        ResponseEntity.ok(
            Map.of(
                "status", "UP",
                "service", "user-service",
                "timestamp", String.valueOf(System.currentTimeMillis()))));
  }
}
