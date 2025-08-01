package com.example.chat.service;

import org.springframework.http.ResponseEntity;

public interface AuthService {

    /**
     * API đăng nhập, trả về JWT access token và refresh token.
     * @param username Tên đăng nhập của người dùng
     * @param password Mật khẩu của người dùng
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity chứa JWT access token và refresh token
     */
    ResponseEntity<?> login(String username, String password, String userAgent);

    /**
     * API làm mới token, trả về một access token mới nếu refresh token còn hiệu lực.
     * @param refreshToken Token làm mới để lấy access token mới
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity chứa access token mới
     */
    ResponseEntity<?> refreshToken(String refreshToken, String userAgent);

    /**
     * API truy cập tài nguyên bảo vệ, kiểm tra tính hợp lệ của JWT access token.
     * @param authHeader Header chứa Bearer token
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity chứa tài nguyên bảo vệ
     */
    ResponseEntity<?> getProtectedResource(String authHeader, String userAgent);
}
