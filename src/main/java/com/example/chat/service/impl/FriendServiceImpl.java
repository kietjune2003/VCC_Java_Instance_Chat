package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.FriendService;
import com.example.chat.util.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Kích hoạt ghi log
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Trả về danh sách bạn bè của người dùng từ access token
     * @param authHeader Header chứa access token dạng Bearer ...
     * @return List<String> danh sách username của bạn bè
     * @throws Exception nếu user không tồn tại hoặc dữ liệu JSON lỗi
     */
    @Override
    public List<String> getFriends(String authHeader) throws Exception {
        // Tách token từ header "Bearer <token>"
        String token = authHeader.replace("Bearer", "").trim();

        // ✅ Giải mã và kiểm tra token
        String username = jwtUtil.validateToken(token);
        log.info("Fetching friend list for user: {}", username);

        // ✅ Truy vấn thông tin người dùng từ DB
        User user = userRepository.findById(username).orElseThrow(() -> {
            log.warn("User '{}' not found when retrieving friends", username);
            return new RuntimeException("User not found");
        });

        // ✅ Phân tích chuỗi JSON thành danh sách String
        List<String> friends = objectMapper.readValue(user.getFriendsJson(), new TypeReference<>() {});
        log.info("User '{}' has {} friend(s)", username, friends.size());

        return friends;
    }
}
