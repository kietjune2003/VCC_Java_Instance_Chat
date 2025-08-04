package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.entity.UserToken;
import com.example.chat.repository.UserRepository;
import com.example.chat.repository.UserTokenRepository;
import com.example.chat.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    private final String USERNAME = "alice";
    private final String PASSWORD = "password123";
    private final String HASHED_PASSWORD = new BCryptPasswordEncoder().encode(PASSWORD);
    private final String ACCESS_TOKEN = "access-token";
    private final String REFRESH_TOKEN = "refresh-token";
    private final String USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(userRepository, jwtUtil, userTokenRepository);
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsById(USERNAME)).thenReturn(false);

        ResponseEntity<?> response = authService.register(USERNAME, PASSWORD);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Registration successful", response.getBody());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUsernameExists() {
        when(userRepository.existsById(USERNAME)).thenReturn(true);

        ResponseEntity<?> response = authService.register(USERNAME, PASSWORD);

        assertEquals(409, response.getStatusCodeValue());
        assertEquals("Username already exists", response.getBody());
    }

    @Test
    void testLoginSuccess() {
        User user = User.builder().username(USERNAME).passwordHash(HASHED_PASSWORD).build();

        when(userRepository.findById(USERNAME)).thenReturn(Optional.of(user));
        when(userTokenRepository.findValidTokensByUser(eq(USERNAME), any())).thenReturn(new ArrayList<>());
        when(jwtUtil.generateAccessToken(USERNAME, USER_AGENT)).thenReturn(ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(USERNAME, USER_AGENT)).thenReturn(REFRESH_TOKEN);

        ResponseEntity<?> response = authService.login(USERNAME, PASSWORD, USER_AGENT);

        assertEquals(200, response.getStatusCodeValue());
        Map<String, String> tokens = (Map<String, String>) response.getBody();
        assertEquals(ACCESS_TOKEN, tokens.get("accessToken"));
        assertEquals(REFRESH_TOKEN, tokens.get("refreshToken"));
        verify(userTokenRepository).save(any(UserToken.class));
    }

    @Test
    void testLoginWrongPassword() {
        User user = User.builder().username(USERNAME).passwordHash(new BCryptPasswordEncoder().encode("wrongPass")).build();

        when(userRepository.findById(USERNAME)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = authService.login(USERNAME, PASSWORD, USER_AGENT);

        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid credentials", response.getBody());
    }

    @Test
    void testLoginUserNotFound() {
        when(userRepository.findById(USERNAME)).thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.login(USERNAME, PASSWORD, USER_AGENT);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void testRefreshTokenSuccess() {
        when(jwtUtil.validateToken(REFRESH_TOKEN, USER_AGENT)).thenReturn(USERNAME);
        when(jwtUtil.generateAccessToken(USERNAME, USER_AGENT)).thenReturn(ACCESS_TOKEN);

        ResponseEntity<?> response = authService.refreshToken(REFRESH_TOKEN, USER_AGENT);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(ACCESS_TOKEN, ((Map<String, String>) response.getBody()).get("accessToken"));
    }

    @Test
    void testRefreshTokenInvalid() {
        when(jwtUtil.validateToken(REFRESH_TOKEN, USER_AGENT)).thenThrow(new JwtException("expired"));

        ResponseEntity<?> response = authService.refreshToken(REFRESH_TOKEN, USER_AGENT);

        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Refresh token invalid or expired", response.getBody());
    }

    @Test
    void testGetProtectedResourceSuccess() {
        when(jwtUtil.validateToken("Bearer token".substring(7), USER_AGENT)).thenReturn(USERNAME);

        ResponseEntity<?> response = authService.getProtectedResource("Bearer token", USER_AGENT);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Hello " + USERNAME, response.getBody());
    }

    @Test
    void testLogoutSuccess() {
        UserToken token = UserToken.builder().accessToken(ACCESS_TOKEN).build();

        when(jwtUtil.validateToken(ACCESS_TOKEN, USER_AGENT)).thenReturn(USERNAME);
        when(userTokenRepository.findByAccessToken(ACCESS_TOKEN)).thenReturn(Optional.of(token));

        ResponseEntity<?> response = authService.logout("Bearer " + ACCESS_TOKEN, USER_AGENT);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Logged out successfully", response.getBody());
        verify(userTokenRepository).delete(token);
    }

    @Test
    void testLogoutInvalidToken() {
        when(jwtUtil.validateToken(ACCESS_TOKEN, USER_AGENT)).thenThrow(new JwtException("invalid"));

        ResponseEntity<?> response = authService.logout("Bearer " + ACCESS_TOKEN, USER_AGENT);

        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid token", response.getBody());
    }
}
