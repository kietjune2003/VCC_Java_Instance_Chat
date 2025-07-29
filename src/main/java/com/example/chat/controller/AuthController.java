package com.example.chat.controller;

import com.example.chat.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
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
