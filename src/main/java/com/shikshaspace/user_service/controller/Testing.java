package com.shikshaspace.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Testing {

    @GetMapping("/user/test")
    public String test() {
        return "test for user service";
    }
}
