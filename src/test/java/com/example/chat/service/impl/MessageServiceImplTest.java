package com.example.chat.service.impl;

import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageServiceImplTest {

    @InjectMocks
    private MessageServiceImpl messageService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private OnlineUserServiceImpl onlineUserService;

    private final String VALID_TOKEN = "Bearer mocktoken";
    private final String SENDER = "alice";
    private final String RECEIVER = "bob";
    private final String USER_AGENT = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageService = new MessageServiceImpl(jwtUtil, userRepository, messageRepository, onlineUserService);
    }

    // ✅ Helper method tạo User cho test
    private User createUser(String username, String friendsJson) {
        return new User(
                username,
                "hashedPassword",
                friendsJson,
                null,
                null,
                null,
                null,
                null
        );
    }

    // ✅ Test gửi tin nhắn text thành công (người nhận không online) với userAgent
    @Test
    void testSendTextMessageSuccessWithUserAgent() throws IOException {
        User receiver = createUser(RECEIVER, "[\"alice\"]");

        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn(SENDER); // Validate token với userAgent
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));
        when(onlineUserService.getWaitingQueue(RECEIVER)).thenReturn(null);

        Map<String, Object> result = messageService.sendMessage(VALID_TOKEN, RECEIVER, "Hello", null, USER_AGENT);

        assertEquals(2, result.get("status"));
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    // ✅ Test gửi tin nhắn khi không phải bạn bè với userAgent
    @Test
    void testSendMessageNotFriendWithUserAgent() throws IOException {
        User receiver = createUser(RECEIVER, "[\"charlie\"]");

        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn(SENDER); // Validate token với userAgent
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));

        Map<String, Object> result = messageService.sendMessage(VALID_TOKEN, RECEIVER, "Hello", null, USER_AGENT);

        assertEquals(3, result.get("status"));
        verify(messageRepository, never()).save(any());
    }

    // ✅ Test gửi file thành công với userAgent
    @Test
    void testSendFileSuccessWithUserAgent() throws IOException {
        User receiver = createUser(RECEIVER, "[\"alice\"]");
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "file content".getBytes());

        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn(SENDER); // Validate token với userAgent
        when(userRepository.findById(RECEIVER)).thenReturn(Optional.of(receiver));
        when(onlineUserService.getWaitingQueue(RECEIVER)).thenReturn(null);

        Map<String, Object> result = messageService.sendMessage(VALID_TOKEN, RECEIVER, null, file, USER_AGENT);

        assertEquals(2, result.get("status"));
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    // ✅ Test get file hợp lệ với userAgent
    @Test
    void testGetFileAuthorizedWithUserAgent() throws IOException {
        Message message = new Message(
                1L,
                SENDER,
                RECEIVER,
                "/api/file/test.txt",
                true,
                LocalDateTime.now(),
                true
        );

        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn(RECEIVER); // Validate token với userAgent
        when(messageRepository.findByReceiver(RECEIVER)).thenReturn(List.of(message));

        Resource res = messageService.getFile(VALID_TOKEN, "test.txt", USER_AGENT);

        assertNotNull(res);
    }

    // ✅ Test get file khi không có quyền với userAgent
    @Test
    void testGetFileUnauthorizedWithUserAgent() {
        Message message = new Message(
                1L,
                SENDER,
                RECEIVER,
                "/api/file/other.txt",
                true,
                LocalDateTime.now(),
                true
        );

        when(jwtUtil.validateToken(anyString(), eq(USER_AGENT))).thenReturn("someoneElse"); // Validate token với userAgent
        when(messageRepository.findByReceiver("someoneElse")).thenReturn(List.of(message));

        assertThrows(SecurityException.class, () ->
                messageService.getFile(VALID_TOKEN, "test.txt", USER_AGENT));
    }
}
