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
@Slf4j // 👉 Ghi log bằng Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * API đăng nhập. Trả về JWT access token và refresh token.
     * @param req Map chứa username và password
     * @return ResponseEntity chứa JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req, @RequestHeader("User-Agent") String userAgent) {
        String username = req.get("username");
        String password = req.get("password");

        // Log thông tin API được gọi
        log.info("API /api/login called for user '{}'", username);

        // Gọi service đăng nhập và trả về kết quả
        return authService.login(username, password, userAgent); // Truyền thêm userAgent vào service
    }

    /**
     * API làm mới token. Trả về một access token mới nếu refresh token còn hiệu lực.
     * @param req Map chứa refresh token
     * @return ResponseEntity chứa access token mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> req, @RequestHeader("User-Agent") String userAgent) {
        String refreshToken = req.get("refreshToken");

        // Log thông tin API được gọi
        log.info("API /api/refresh called with refresh token '{}'", refreshToken);

        // Gọi service làm mới token và trả về kết quả
        return authService.refreshToken(refreshToken, userAgent); // Truyền thêm userAgent vào service
    }

    /**
     * API truy cập tài nguyên bảo vệ. Kiểm tra tính hợp lệ của JWT access token.
     * @param authHeader Header chứa Bearer token
     * @return ResponseEntity chứa tài nguyên bảo vệ
     */
    @GetMapping("/protected")
    public ResponseEntity<?> protectedResource(@RequestHeader("Authorization") String authHeader, @RequestHeader("User-Agent") String userAgent) {
        // Log thông tin API được gọi
        log.info("API /api/protected called with Authorization header: '{}'", authHeader);

        // Gọi service để lấy tài nguyên bảo vệ
        return authService.getProtectedResource(authHeader, userAgent); // Truyền thêm userAgent vào service
    }
}
