package com.shikshaspace.user_service.controller;


import com.shikshaspace.user_service.dto.UserProfileRequest;
import com.shikshaspace.user_service.dto.UserProfileResponse;
import com.shikshaspace.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Mono<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        return userService.getCurrentUserProfile(authentication);
    }

    @PutMapping("/profile")
    public Mono<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileRequest request) {
        return userService.updateProfile(authentication, request);
    }
}
