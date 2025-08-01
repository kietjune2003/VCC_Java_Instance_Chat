package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.AuthService;
import com.example.chat.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j // Ghi log với Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * ✅ Đăng nhập và sinh access token + refresh token
     * @param username Tên đăng nhập của người dùng
     * @param password Mật khẩu của người dùng
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     */
    @Override
    public ResponseEntity<?> login(String username, String password, String userAgent) {
        log.info("Login attempt for username: {}", username);

        User user = userRepository.findById(username).orElse(null);

        if (user == null || !user.getPasswordHash().equals(password)) {
            log.warn("Login failed for '{}': invalid credentials", username);
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // ✅ Sinh token mới, thêm userAgent vào trong quá trình tạo token
        String accessToken = jwtUtil.generateAccessToken(username, userAgent);
        String refreshToken = jwtUtil.generateRefreshToken(username, userAgent);

        // ✅ Lưu thông tin token và thời gian vào DB
        user.setAccessToken(accessToken);
        user.setTokenCreatedAt(LocalDateTime.now());
        user.setTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(10));
        userRepository.save(user);

        log.info("User '{}' logged in successfully. Tokens issued.", username);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    /**
     * ✅ Làm mới access token bằng refresh token
     * @param refreshToken Refresh token của người dùng
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     */
    @Override
    public ResponseEntity<?> refreshToken(String refreshToken, String userAgent) {
        log.debug("Refresh token request received");

        try {
            String username = jwtUtil.validateToken(refreshToken, userAgent); // Kiểm tra thêm userAgent
            String newAccessToken = jwtUtil.generateAccessToken(username, userAgent); // Sinh access token mới

            log.info("Refresh token valid. New access token issued for user '{}'", username);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (JwtException e) {
            log.warn("Refresh token invalid or expired: {}", e.getMessage());
            return ResponseEntity.status(401).body("Refresh token invalid or expired");
        }
    }

    /**
     * ✅ Lấy tài nguyên protected để test token
     * @param authHeader Header chứa Bearer token
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     */
    @Override
    public ResponseEntity<?> getProtectedResource(String authHeader, String userAgent) {
        String token = authHeader.replace("Bearer ", "");

        String username = jwtUtil.validateToken(token, userAgent); // Kiểm tra token với userAgent
        log.debug("Access token valid. Returning protected resource for '{}'", username);

        return ResponseEntity.ok("Hello " + username);
    }
}
