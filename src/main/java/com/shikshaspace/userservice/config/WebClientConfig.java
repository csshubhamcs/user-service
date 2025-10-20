package com.shikshaspace.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class WebClientConfig {

  // check later need or not
  @Bean
  public WebClient.Builder webClientBuilder() {
    log.info("Initializing WebClient builder");
    return WebClient.builder();
  }
}
