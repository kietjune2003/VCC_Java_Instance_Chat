package com.example.chat.filter;

import com.example.chat.entity.UserToken;
import com.example.chat.repository.UserTokenRepository;
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
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final UserTokenRepository userTokenRepository;

    /**
     * ✅ Bộ lọc JWT để xác thực mỗi request gửi vào hệ thống
     * Nếu token hợp lệ → cho qua.
     * Nếu token hết hạn nhưng refresh token còn hạn → cấp lại access token mới.
     * Nếu token không hợp lệ → trả về lỗi 401.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String header = httpRequest.getHeader("Authorization");

        // ✅ Kiểm tra có Authorization header dạng Bearer không
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Loại bỏ "Bearer "
            String userAgent = httpRequest.getHeader("User-Agent"); // Lấy thông tin thiết bị

            try {
                // ✅ Xác thực access token và kiểm tra khớp userAgent
                String username = jwtUtil.validateToken(token, userAgent);
                log.debug("JWT validated for user '{}' and userAgent matched", username);

                // ✅ Token hợp lệ → tiếp tục filter chain
                chain.doFilter(request, response);
                return;

            } catch (ExpiredJwtException e) {
                // ⏰ Access token đã hết hạn
                String username = e.getClaims().getSubject();
                log.warn("Access token expired for '{}'", username);

                // 🔍 Tìm access token trong database (bảng user_tokens)
                UserToken userToken = userTokenRepository.findByAccessToken(token).orElse(null);
                if (userToken == null) {
                    log.warn("Access token not found in DB");
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }

                // 🧪 Kiểm tra refresh token còn hạn không
                if (userToken.getRefreshTokenExpiresAt().isAfter(LocalDateTime.now())) {
                    // ✅ Refresh token còn hạn → cấp lại access token mới
                    String newAccessToken = jwtUtil.generateAccessToken(username, userAgent);

                    // 🔁 Cập nhật token mới vào DB
                    userToken.setAccessToken(newAccessToken);
                    userToken.setTokenCreatedAt(LocalDateTime.now());
                    userToken.setTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
                    userTokenRepository.save(userToken);

                    log.info("Issued new access token for user '{}'", username);

                    // ✅ Đưa access token mới vào response header
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setHeader("New-Access-Token", newAccessToken);

                    // Cho phép đi tiếp filter chain
                    chain.doFilter(request, response);
                    return;
                } else {
                    // ❌ Refresh token cũng hết hạn → bắt đăng nhập lại
                    log.warn("Refresh token expired for '{}'", username);
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired");
                    return;
                }

            } catch (JwtException e) {
                // ❌ Access token không hợp lệ (giả mạo, sai chữ ký, sai user-agent...)
                log.error("Invalid JWT token: {}", e.getMessage());
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }

        // ⛔ Không có Authorization header → cho qua (có thể là request public)
        log.debug("No Authorization header found, continuing filter chain");
        chain.doFilter(request, response);
    }
}
