package com.example.chat.service.impl;

import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.repository.MessageRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.MessageService;
import com.example.chat.util.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final OnlineUserServiceImpl onlineUserServiceImpl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String storagePath = "storage";

    /**
     * Trích xuất username từ Authorization header
     */
    private String extractUsername(String authHeader) {
        return jwtUtil.validateToken(authHeader.replace("Bearer ", "").trim());
    }

    /**
     * Gửi tin nhắn văn bản hoặc file đến người nhận
     * @param authHeader access token
     * @param username người nhận
     * @param message nội dung văn bản (nếu có)
     * @param file file đính kèm (nếu có)
     * @return Map kết quả status
     * @throws IOException lỗi xử lý file
     */
    @Override
    public Map<String, Object> sendMessage(String authHeader, String username, String message, MultipartFile file) throws IOException {
        String sender = extractUsername(authHeader);
        log.info("Sending message from '{}' to '{}'", sender, username);

        Optional<User> optionalReceiver = userRepository.findById(username);
        if (optionalReceiver.isEmpty()) {
            log.warn("Receiver '{}' not found", username);
            return Map.of("status", 404, "message", "User not found");
        }

        User receiver = optionalReceiver.get();

        // Kiểm tra bạn bè
        List<String> friends = objectMapper.readValue(receiver.getFriendsJson(), new TypeReference<>() {});
        if (!friends.contains(sender)) {
            log.warn("Sender '{}' is not a friend of '{}'", sender, username);
            return Map.of("status", 3);
        }

        boolean isFile = false;
        String content;

        // Xử lý tin nhắn file hoặc text
        if (file != null && !file.isEmpty()) {
            isFile = true;
            Files.createDirectories(Paths.get(storagePath));
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(storagePath, fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            content = "/api/file/" + fileName;
            log.info("Saved file message from '{}' to '{}' at '{}'", sender, username, fileName);
        } else if (message != null && !message.isBlank()) {
            content = message;
            log.info("Saved text message from '{}' to '{}'", sender, username);
        } else {
            log.warn("Empty message or file from '{}'", sender);
            throw new IllegalArgumentException("Message content or file is required");
        }

        Message msg = new Message(null, sender, username, content, isFile, LocalDateTime.now(), false);

        // Kiểm tra người nhận đang online để gửi ngay
        BlockingQueue<Message> queue = onlineUserServiceImpl.getWaitingQueue(username);
        if (queue != null) {
            queue.offer(msg);
            log.info("Delivered message to '{}' immediately", username);
            return Map.of("status", 1);
        }

        // Nếu offline thì lưu DB
        messageRepository.save(msg);
        log.info("Stored message for offline user '{}'", username);
        return Map.of("status", 2);
    }

    /**
     * Trả về tin nhắn chờ bằng long polling tối đa 10s
     */
    @Override
    public List<Map<String, Object>> getMessages(String authHeader) throws InterruptedException {
        String username = extractUsername(authHeader);
        log.info("User '{}' is polling for new messages", username);

        // Trả ngay nếu có message chưa nhận
        List<Message> pending = messageRepository.findByReceiverAndDeliveredFalse(username);
        if (!pending.isEmpty()) {
            pending.forEach(m -> m.setDelivered(true));
            messageRepository.saveAll(pending);
            log.info("Returned {} pending message(s) to '{}'", pending.size(), username);
            return toResponse(pending);
        }

        // Chờ tin nhắn mới tối đa 10s
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        onlineUserServiceImpl.waitForMessages(username, queue);

        Message newMessage = queue.poll(10, TimeUnit.SECONDS);
        onlineUserServiceImpl.removeWaitingQueue(username);

        if (newMessage != null) {
            newMessage.setDelivered(true);
            messageRepository.save(newMessage);
            log.info("Delivered real-time message to '{}'", username);
            return toResponse(List.of(newMessage));
        }

        log.info("No new message for '{}'", username);
        return Collections.emptyList();
    }

    /**
     * Trả về file nếu user có quyền
     */
    @Override
    public Resource getFile(String authHeader, String filename) throws IOException {
        String username = extractUsername(authHeader);
        log.info("User '{}' requests file '{}'", username, filename);

        List<Message> messages = messageRepository.findByReceiver(username);
        boolean authorized = messages.stream()
                .anyMatch(msg -> msg.getContent().contains(filename) && msg.isFile());

        if (!authorized) {
            log.warn("Unauthorized file access attempt by '{}'", username);
            throw new SecurityException("Access denied");
        }

        Path filePath = Paths.get(storagePath, filename);
        if (!Files.exists(filePath)) {
            log.error("File '{}' not found", filename);
            throw new NoSuchFileException("File not found");
        }

        return new FileSystemResource(filePath);
    }

    /**
     * Chuyển danh sách tin nhắn thành Map trả về JSON
     */
    private List<Map<String, Object>> toResponse(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message m : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("time", m.getTimestamp().toString());
            item.put("sender", m.getSender());
            item.put("message", m.getContent()); // Link hoặc text
            result.add(item);
        }
        return result;
    }
}
