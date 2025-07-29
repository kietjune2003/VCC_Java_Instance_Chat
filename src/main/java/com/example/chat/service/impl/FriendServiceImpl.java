package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.FriendService;
import com.example.chat.util.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> getFriends(String authHeader) throws Exception {
        String token = authHeader.replace("Bearer", "").trim();

        String username = jwtUtil.validateToken(token);
        User user = userRepository.findById(username).orElseThrow();

        return objectMapper.readValue(user.getFriendsJson(), new TypeReference<>() {});
    }
}
