package com.example.chat.controller;

import com.example.chat.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j

public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        log.info("API /api/login called");
        return authService.login(username, password);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> req) {
        String refreshToken = req.get("refreshToken");
        return authService.refreshToken(refreshToken);
    }

    @GetMapping("/protected")
    public ResponseEntity<?> protectedResource(@RequestHeader("Authorization") String authHeader) {
        return authService.getProtectedResource(authHeader);
    }
}
