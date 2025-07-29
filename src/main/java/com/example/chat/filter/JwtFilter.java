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
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String header = httpRequest.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String username = jwtUtil.validateToken(token);
                // Token hợp lệ -> cho qua
                chain.doFilter(request, response);
                return;
            } catch (ExpiredJwtException e) {
                String username = e.getClaims().getSubject();
                User user = userRepository.findById(username).orElse(null);

                if (user != null && user.getRefreshToken() != null) {
                    boolean refreshTokenValid = !jwtUtil.isTokenExpired(user.getRefreshToken());

                    if (refreshTokenValid) {
                        String newAccessToken = jwtUtil.generateAccessToken(username);
                        user.setAccessToken(newAccessToken);
                        userRepository.save(user);

                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setHeader("New-Access-Token", newAccessToken);
                        chain.doFilter(request, response);
                        return;
                    }
                }
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired");
                return;
            } catch (JwtException e) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }

        chain.doFilter(request, response); // Không có token → vẫn xử lý (nếu cần bảo vệ thì dùng security config)
    }
}
