package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.entity.UserToken;
import com.example.chat.repository.UserRepository;
import com.example.chat.repository.UserTokenRepository;
import com.example.chat.service.AuthService;
import com.example.chat.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j // Ghi log với Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserTokenRepository userTokenRepository;

    // ✅ Sử dụng BCrypt để mã hoá và kiểm tra mật khẩu
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * ✅ Đăng ký người dùng mới, mã hoá mật khẩu
     */
    @Override
    public ResponseEntity<?> register(String username, String password) {
        log.info("Register attempt for username: {}", username);

        if (userRepository.existsById(username)) {
            return ResponseEntity.status(409).body("Username already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .username(username)
                .passwordHash(hashedPassword)
                .friendsJson("[]") // Ban đầu không có bạn
                .build();

        userRepository.save(user);
        log.info("User '{}' registered successfully", username);
        return ResponseEntity.ok("Registration successful");
    }

    /**
     * ✅ Đăng nhập, kiểm tra mật khẩu, sinh token mới và quản lý tối đa 2 phiên đăng nhập
     */
    @Override
    public ResponseEntity<?> login(String username, String password, String userAgent) {
        log.info("Login attempt for username: {}", username);

        // 🔍 Tìm user theo username
        User user = userRepository.findById(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // 🔍 Lấy tất cả token còn hiệu lực của user
        List<UserToken> activeTokens = userTokenRepository.findValidTokensByUser(username, LocalDateTime.now());

        // 🧹 Nếu đã có 2 token → xoá token cũ nhất
        if (activeTokens.size() >= 2) {
            activeTokens.sort(Comparator.comparing(UserToken::getTokenCreatedAt));
            userTokenRepository.delete(activeTokens.get(0));
            log.info("Deleted oldest token for user '{}'", username);
        }

        // 🔐 Tạo access token và refresh token mới
        String accessToken = jwtUtil.generateAccessToken(username, userAgent);
        String refreshToken = jwtUtil.generateRefreshToken(username, userAgent);

        UserToken newToken = UserToken.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenCreatedAt(LocalDateTime.now())
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(10))
                .refreshTokenExpiresAt(LocalDateTime.now().plusDays(10))
                .build();

        userTokenRepository.save(newToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    /**
     * ✅ Làm mới access token bằng refresh token
     */
    @Override
    public ResponseEntity<?> refreshToken(String refreshToken, String userAgent) {
        log.debug("Refresh token request received");

        try {
            String username = jwtUtil.validateToken(refreshToken, userAgent); // Kiểm tra token hợp lệ và đúng userAgent
            String newAccessToken = jwtUtil.generateAccessToken(username, userAgent);

            log.info("Refresh token valid. New access token issued for '{}'", username);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (JwtException e) {
            log.warn("Refresh token invalid or expired: {}", e.getMessage());
            return ResponseEntity.status(401).body("Refresh token invalid or expired");
        }
    }

    /**
     * ✅ API dùng để test token có hợp lệ không
     */
    @Override
    public ResponseEntity<?> getProtectedResource(String authHeader, String userAgent) {
        String token = authHeader.replace("Bearer ", "");

        String username = jwtUtil.validateToken(token, userAgent);
        log.debug("Access token valid. Returning protected resource for '{}'", username);

        return ResponseEntity.ok("Hello " + username);
    }

    /**
     * ✅ Logout khỏi phiên hiện tại bằng cách xoá access token khỏi DB
     */
    @Override
    public ResponseEntity<?> logout(String authHeader, String userAgent) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.validateToken(token, userAgent);
            log.info("Logout requested by user '{}'", username);

            UserToken tokenEntity = userTokenRepository.findByAccessToken(token).orElse(null);
            if (tokenEntity != null) {
                userTokenRepository.delete(tokenEntity);
                log.info("Access token deleted for '{}'", username);
            }

            return ResponseEntity.ok("Logged out successfully");
        } catch (JwtException e) {
            log.warn("Logout failed: invalid token - {}", e.getMessage());
            return ResponseEntity.status(401).body("Invalid token");
        }
    }
}
