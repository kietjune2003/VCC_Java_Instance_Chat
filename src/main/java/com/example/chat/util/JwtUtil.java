package com.example.chat.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j // 👉 Cho phép ghi log bằng SLF4J (sẽ dùng log.debug/info/error...)
public class JwtUtil {

    // Khóa bí mật để ký JWT (phải >= 32 bytes)
    private final Key key;

    // Thời gian sống của access token: 10 phút
    private final long ACCESS_TOKEN_EXPIRATION = 10 * 60 * 1000;

    // Thời gian sống của refresh token: 10 ngày
    private final long REFRESH_TOKEN_EXPIRATION = 10L * 24 * 60 * 60 * 1000;

    public JwtUtil() {
        String secret = "my-super-secret-key-that-is-at-least-32-bytes!!";
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT Util initialized with secret key (length: {})", secret.length());
    }

    /**
     * 👉 Sinh access token có hiệu lực 10 phút
     */
    public String generateAccessToken(String username) {
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated access token for user '{}'", username);
        return token;
    }

    /**
     * 👉 Sinh refresh token có hiệu lực 10 ngày
     */
    public String generateRefreshToken(String username) {
        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated refresh token for user '{}'", username);
        return token;
    }

    /**
     * ✅ Kiểm tra token và trả về username (subject)
     * @throws JwtException nếu token không hợp lệ hoặc hết hạn
     */
    public String validateToken(String token) {
        try {
            String username = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            log.debug("Token validated successfully for user '{}'", username);
            return username;

        } catch (ExpiredJwtException e) {
            log.warn("Token expired for user '{}'", e.getClaims().getSubject());
            throw e;
        } catch (JwtException e) {
            log.error("Invalid JWT: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * ⏰ Kiểm tra token đã hết hạn chưa
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

            boolean expired = expiration.before(new Date());
            log.debug("Token expired: {}", expired);
            return expired;

        } catch (ExpiredJwtException e) {
            log.debug("Token is already expired (exception thrown)");
            return true;
        }
    }

    /**
     * 📦 Lấy toàn bộ thông tin claims (subject, issuedAt, expiration,...)
     */
    public Claims getAllClaims(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.debug("Claims extracted for user '{}'", claims.getSubject());
            return claims;

        } catch (JwtException e) {
            log.error("Failed to parse claims: {}", e.getMessage());
            throw e;
        }
    }
}
