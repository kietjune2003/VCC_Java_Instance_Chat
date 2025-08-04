package com.example.chat.service.impl;

import com.example.chat.entity.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendServiceImplTest {

    @InjectMocks
    private FriendServiceImpl friendService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final String AUTH_HEADER = "Bearer testtoken";
    private final String USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        friendService = new FriendServiceImpl(userRepository, jwtUtil, objectMapper);
    }

    // ✅ Tạo user mock với friends và requests
    private User createUser(String username, List<String> friends, List<String> requests) throws Exception {
        return User.builder()
                .username(username)
                .passwordHash("hash")
                .friendsJson(objectMapper.writeValueAsString(friends))
                .friendRequests(objectMapper.writeValueAsString(requests))
                .build();
    }

    // ✅ Test getFriends thành công
    @Test
    void testGetFriendsSuccess() throws Exception {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("alice");

        User user = createUser("alice", List.of("bob", "charlie"), List.of());
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));

        List<String> result = friendService.getFriends(AUTH_HEADER, USER_AGENT);

        assertEquals(2, result.size());
        assertTrue(result.contains("bob"));
    }

    // ✅ Test gửi lời mời kết bạn thành công
    @Test
    void testSendFriendRequestSuccess() throws Exception {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("alice");

        User bob = createUser("bob", List.of(), List.of());
        when(userRepository.findById("bob")).thenReturn(Optional.of(bob));

        String result = friendService.sendFriendRequest(AUTH_HEADER, USER_AGENT, "bob");

        assertEquals("Friend request sent.", result);
    }

    // ✅ Test gửi lời mời đã tồn tại
    @Test
    void testSendFriendRequestAlreadySent() throws Exception {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("alice");

        User bob = createUser("bob", List.of(), List.of("alice"));
        when(userRepository.findById("bob")).thenReturn(Optional.of(bob));

        String result = friendService.sendFriendRequest(AUTH_HEADER, USER_AGENT, "bob");

        assertEquals("Friend request already sent.", result);
    }

    // ✅ Test gửi lời mời đến chính mình
    @Test
    void testSendFriendRequestToSelf() {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("alice");

        assertThrows(IllegalArgumentException.class, () ->
                friendService.sendFriendRequest(AUTH_HEADER, USER_AGENT, "alice"));
    }

    // ✅ Test chấp nhận lời mời thành công
    @Test
    void testRespondToFriendRequestAccepted() throws Exception {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("bob");

        User bob = createUser("bob", new ArrayList<>(), List.of("alice"));
        User alice = createUser("alice", new ArrayList<>(), List.of());

        when(userRepository.findById("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById("alice")).thenReturn(Optional.of(alice));

        String result = friendService.respondToFriendRequest(AUTH_HEADER, USER_AGENT, "alice", true);

        assertEquals("Friend request accepted.", result);
    }

    // ✅ Test từ chối lời mời kết bạn
    @Test
    void testRespondToFriendRequestDeclined() throws Exception {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("bob");

        User bob = createUser("bob", new ArrayList<>(), List.of("alice"));
        User alice = createUser("alice", new ArrayList<>(), List.of());

        when(userRepository.findById("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById("alice")).thenReturn(Optional.of(alice));

        String result = friendService.respondToFriendRequest(AUTH_HEADER, USER_AGENT, "alice", false);

        assertEquals("Friend request declined.", result);
    }

    // ✅ Test phản hồi lời mời không tồn tại
    @Test
    void testRespondToFriendRequestNotFound() throws Exception {
        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("bob");

        User bob = createUser("bob", new ArrayList<>(), List.of());
        User alice = createUser("alice", new ArrayList<>(), List.of());

        when(userRepository.findById("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById("alice")).thenReturn(Optional.of(alice));

        String result = friendService.respondToFriendRequest(AUTH_HEADER, USER_AGENT, "alice", true);

        assertEquals("No friend request found.", result);
    }
}
