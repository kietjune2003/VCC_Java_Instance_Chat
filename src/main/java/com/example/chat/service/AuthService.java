package com.example.chat.service;

import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> login(String username, String password);
    ResponseEntity<?> refreshToken(String refreshToken);
    ResponseEntity<?> getProtectedResource(String authHeader);
}
