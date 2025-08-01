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

            // Lấy thông tin User-Agent từ header request
            String userAgent = httpRequest.getHeader("User-Agent");

            try {
                // ✅ Kiểm tra token access có hợp lệ không và kiểm tra userAgent
                String username = jwtUtil.validateToken(token, userAgent);
                log.debug("JWT validated successfully for user '{}' with matching userAgent", username);

                // Chuyển tiếp request nếu token hợp lệ
                chain.doFilter(request, response);
                return;

            } catch (ExpiredJwtException e) {
                // ⏰ Token đã hết hạn
                String username = e.getClaims().getSubject();
                log.warn("Access token expired for user '{}'", username);

                // Tìm người dùng trong cơ sở dữ liệu
                User user = userRepository.findById(username).orElse(null);

                // 🔁 Kiểm tra và dùng refresh token nếu còn hạn
                if (user != null && user.getRefreshToken() != null) {
                    boolean refreshTokenValid = !jwtUtil.isTokenExpired(user.getRefreshToken());

                    if (refreshTokenValid) {
                        // Tạo lại access token mới từ refresh token
                        String newAccessToken = jwtUtil.generateAccessToken(username, userAgent);
                        user.setAccessToken(newAccessToken);
                        userRepository.save(user);

                        log.info("New access token issued via refresh token for '{}'", username);

                        // Đưa token mới vào header response
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setHeader("New-Access-Token", newAccessToken);

                        // Tiếp tục filter chain với token mới
                        chain.doFilter(request, response);
                        return;
                    } else {
                        log.warn("Refresh token expired for '{}'", username);
                    }
                } else {
                    log.warn("User '{}' not found or no refresh token", username);
                }

                // Trả về lỗi nếu refresh token đã hết hạn
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
