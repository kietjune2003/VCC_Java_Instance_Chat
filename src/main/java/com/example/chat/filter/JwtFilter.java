package com.example.chat.filter;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j // 👉 Ghi log bằng Slf4j
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String header = httpRequest.getHeader("Authorization");

        // 👉 Nếu có header Authorization bắt đầu bằng Bearer
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Cắt bỏ "Bearer "

            try {
                // ✅ Kiểm tra token access có hợp lệ không
                String username = jwtUtil.validateToken(token);
                log.debug("JWT validated successfully for user '{}'", username);

                chain.doFilter(request, response); // Cho phép tiếp tục
                return;

            } catch (ExpiredJwtException e) {
                // ⏰ Token đã hết hạn
                String username = e.getClaims().getSubject();
                log.warn("Access token expired for user '{}'", username);

                User user = userRepository.findById(username).orElse(null);

                // 🔁 Kiểm tra và dùng refresh token nếu còn hạn
                if (user != null && user.getRefreshToken() != null) {
                    boolean refreshTokenValid = !jwtUtil.isTokenExpired(user.getRefreshToken());

                    if (refreshTokenValid) {
                        String newAccessToken = jwtUtil.generateAccessToken(username);
                        user.setAccessToken(newAccessToken);
                        userRepository.save(user);

                        log.info("New access token issued via refresh token for '{}'", username);

                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setHeader("New-Access-Token", newAccessToken);
                        chain.doFilter(request, response);
                        return;
                    } else {
                        log.warn("Refresh token expired for '{}'", username);
                    }
                } else {
                    log.warn("User '{}' not found or no refresh token", username);
                }

                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired");
                return;

            } catch (JwtException e) {
                // ❌ Token không hợp lệ
                log.error("Invalid JWT token: {}", e.getMessage());
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }

        // 🛡️ Nếu không có token → không can thiệp, chuyển sang filter kế tiếp (có thể là public API)
        log.debug("No Authorization header found, continuing without authentication");
        chain.doFilter(request, response);
    }
}
