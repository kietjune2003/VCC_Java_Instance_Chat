package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    private final String VALID_USERNAME = "alice";
    private final String VALID_PASSWORD = "password123";
    private final String ACCESS_TOKEN = "access-token";
    private final String REFRESH_TOKEN = "refresh-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * ✅ Test đăng nhập thành công
     */
    @Test
    void testLoginSuccess() {
        User mockUser = User.builder()
                .username(VALID_USERNAME)
                .passwordHash(VALID_PASSWORD)
                .build();

        when(userRepository.findById(VALID_USERNAME)).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateAccessToken(VALID_USERNAME)).thenReturn(ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(VALID_USERNAME)).thenReturn(REFRESH_TOKEN);

        ResponseEntity<?> response = authService.login(VALID_USERNAME, VALID_PASSWORD);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, String> tokens = (Map<String, String>) response.getBody();
        assertEquals(ACCESS_TOKEN, tokens.get("accessToken"));
        assertEquals(REFRESH_TOKEN, tokens.get("refreshToken"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * ❌ Test đăng nhập sai mật khẩu
     */
    @Test
    void testLoginInvalidPassword() {
        User mockUser = User.builder()
                .username(VALID_USERNAME)
                .passwordHash("wrong-password")
                .build();

        when(userRepository.findById(VALID_USERNAME)).thenReturn(Optional.of(mockUser));

        ResponseEntity<?> response = authService.login(VALID_USERNAME, VALID_PASSWORD);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid credentials", response.getBody());
    }

    /**
     * ❌ Test đăng nhập khi không tồn tại user
     */
    @Test
    void testLoginUserNotFound() {
        when(userRepository.findById(VALID_USERNAME)).thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.login(VALID_USERNAME, VALID_PASSWORD);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid credentials", response.getBody());
    }

    /**
     * 🔄 Test refresh token hợp lệ
     */
    @Test
    void testRefreshTokenSuccess() {
        when(jwtUtil.validateToken(REFRESH_TOKEN)).thenReturn(VALID_USERNAME);
        when(jwtUtil.generateAccessToken(VALID_USERNAME)).thenReturn(ACCESS_TOKEN);

        ResponseEntity<?> response = authService.refreshToken(REFRESH_TOKEN);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(ACCESS_TOKEN, ((Map<String, String>) response.getBody()).get("accessToken"));
    }

    /**
     * ❌ Test refresh token không hợp lệ
     */
    @Test
    void testRefreshTokenInvalid() {
        when(jwtUtil.validateToken(REFRESH_TOKEN)).thenThrow(new JwtException("expired"));

        ResponseEntity<?> response = authService.refreshToken(REFRESH_TOKEN);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Refresh token invalid or expired", response.getBody());
    }

    /**
     * 🔐 Test truy cập tài nguyên được bảo vệ
     */
    @Test
    void testGetProtectedResource() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(VALID_USERNAME);

        ResponseEntity<?> response = authService.getProtectedResource("Bearer valid-token");
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Hello " + VALID_USERNAME, response.getBody());
    }
}
