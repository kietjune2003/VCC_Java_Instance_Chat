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
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return List<String> danh sách username của bạn bè
     * @throws Exception nếu user không tồn tại hoặc dữ liệu JSON lỗi
     */
    @Override
    public List<String> getFriends(String authHeader, String userAgent) throws Exception {
        // Tách token từ header "Bearer <token>"
        String token = authHeader.replace("Bearer", "").trim();

        // ✅ Giải mã và kiểm tra token với userAgent
        String username = jwtUtil.validateToken(token, userAgent); // Kiểm tra token và userAgent
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
    /**
     * ✅ Gửi lời mời kết bạn
     */
    @Override
    public String sendFriendRequest(String authHeader, String userAgent, String toUsername) throws Exception {
        String token = authHeader.replace("Bearer", "").trim();
        String fromUsername = jwtUtil.validateToken(token, userAgent);

        if (fromUsername.equals(toUsername)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }

        User toUser = userRepository.findById(toUsername).orElseThrow(() ->
                new RuntimeException("User '" + toUsername + "' not found"));

        List<String> pendingRequests = parseJsonList(toUser.getFriendRequests());
        if (pendingRequests.contains(fromUsername)) {
            return "Friend request already sent.";
        }

        pendingRequests.add(fromUsername);
        toUser.setFriendRequests(toJsonString(pendingRequests));
        userRepository.save(toUser);

        log.info("User '{}' sent a friend request to '{}'", fromUsername, toUsername);
        return "Friend request sent.";
    }

    /**
     * ✅ Phản hồi lời mời kết bạn
     */
    @Override
    public String respondToFriendRequest(String authHeader, String userAgent, String fromUsername, boolean accepted) throws Exception {
        String token = authHeader.replace("Bearer", "").trim();
        String toUsername = jwtUtil.validateToken(token, userAgent); // người nhận lời mời

        if (toUsername.equals(fromUsername)) {
            throw new IllegalArgumentException("Invalid friend request.");
        }

        // Lấy người nhận và người gửi
        User toUser = userRepository.findById(toUsername).orElseThrow(() -> new RuntimeException("User not found"));
        User fromUser = userRepository.findById(fromUsername).orElseThrow(() -> new RuntimeException("Sender not found"));

        List<String> pendingRequests = parseJsonList(toUser.getFriendRequests());
        if (!pendingRequests.contains(fromUsername)) {
            return "No friend request found.";
        }

        pendingRequests.remove(fromUsername);
        toUser.setFriendRequests(toJsonString(pendingRequests));

        if (accepted) {
            List<String> toFriends = parseJsonList(toUser.getFriendsJson());
            List<String> fromFriends = parseJsonList(fromUser.getFriendsJson());

            if (!toFriends.contains(fromUsername)) {
                toFriends.add(fromUsername);
            }

            if (!fromFriends.contains(toUsername)) {
                fromFriends.add(toUsername);
            }

            toUser.setFriendsJson(toJsonString(toFriends));
            fromUser.setFriendsJson(toJsonString(fromFriends));

            userRepository.save(fromUser);
            userRepository.save(toUser);

            log.info("User '{}' accepted friend request from '{}'", toUsername, fromUsername);
            return "Friend request accepted.";
        } else {
            userRepository.save(toUser);
            log.info("User '{}' declined friend request from '{}'", toUsername, fromUsername);
            return "Friend request declined.";
        }
    }

    private List<String> parseJsonList(String json) throws Exception {
        if (json == null || json.isEmpty()) return new java.util.ArrayList<>();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String toJsonString(List<String> list) throws Exception {
        return objectMapper.writeValueAsString(list);
    }

}
