package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.AuthService;
import com.example.chat.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public ResponseEntity<?> login(String username, String password) {
        User user = userRepository.findById(username).orElse(null);
        if (user == null || !user.getPasswordHash().equals(password)) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String accessToken = jwtUtil.generateAccessToken(username);
        String refreshToken = jwtUtil.generateRefreshToken(username);

        user.setAccessToken(accessToken);
        user.setTokenCreatedAt(LocalDateTime.now());
        user.setTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(10));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    @Override
    public ResponseEntity<?> refreshToken(String refreshToken) {
        try {
            String username = jwtUtil.validateToken(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(username);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (JwtException e) {
            return ResponseEntity.status(401).body("Refresh token invalid or expired");
        }
    }

    @Override
    public ResponseEntity<?> getProtectedResource(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.validateToken(token);
        return ResponseEntity.ok("Hello " + username);
    }
}
