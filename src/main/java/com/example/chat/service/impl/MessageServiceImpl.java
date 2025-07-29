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
public class MessageServiceImpl implements MessageService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final OnlineUserServiceImpl onlineUserServiceImpl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String storagePath = "storage";

    private String extractUsername(String authHeader) {
        return jwtUtil.validateToken(authHeader.replace("Bearer ", "").trim());
    }

    @Override
    public Map<String, Object> sendMessage(String authHeader, String username, String message, MultipartFile file) throws IOException {
        String sender = extractUsername(authHeader);
        Optional<User> optionalReceiver = userRepository.findById(username);
        if (optionalReceiver.isEmpty()) {
            return Map.of("status", 404, "message", "User not found");
        }

        User receiver = optionalReceiver.get();

        List<String> friends = objectMapper.readValue(receiver.getFriendsJson(), new TypeReference<>() {});
        if (!friends.contains(sender)) {
            return Map.of("status", 3);
        }

        boolean isFile = false;
        String content;

        if (file != null && !file.isEmpty()) {
            isFile = true;
            Files.createDirectories(Paths.get(storagePath));
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(storagePath, fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            content = "/api/file/" + fileName;
        } else if (message != null && !message.isBlank()) {
            content = message;
        } else {
            throw new IllegalArgumentException("Message content or file is required");
        }

        Message msg = new Message(null, sender, username, content, isFile, LocalDateTime.now(), false);

        BlockingQueue<Message> queue = onlineUserServiceImpl.getWaitingQueue(username);
        if (queue != null) {
            queue.offer(msg);
            return Map.of("status", 1);
        }

        messageRepository.save(msg);
        return Map.of("status", 2);
    }

    @Override
    public List<Map<String, Object>> getMessages(String authHeader) throws InterruptedException {
        String username = extractUsername(authHeader);

        List<Message> pending = messageRepository.findByReceiverAndDeliveredFalse(username);
        if (!pending.isEmpty()) {
            pending.forEach(m -> m.setDelivered(true));
            messageRepository.saveAll(pending);
            return toResponse(pending);
        }

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        onlineUserServiceImpl.waitForMessages(username, queue);

        Message newMessage = queue.poll(10, TimeUnit.SECONDS);
        onlineUserServiceImpl.removeWaitingQueue(username);

        if (newMessage != null) {
            newMessage.setDelivered(true);
            messageRepository.save(newMessage);
            return toResponse(List.of(newMessage));
        }

        return Collections.emptyList();
    }

    @Override
    public Resource getFile(String authHeader, String filename) throws IOException, SecurityException {
        String username = extractUsername(authHeader);

        List<Message> messages = messageRepository.findByReceiver(username);
        boolean authorized = messages.stream()
                .anyMatch(msg -> msg.getContent().contains(filename) && msg.isFile());

        if (!authorized) {
            throw new SecurityException("Access denied");
        }

        Path filePath = Paths.get(storagePath, filename);
        if (!Files.exists(filePath)) {
            throw new NoSuchFileException("File not found");
        }

        return new FileSystemResource(filePath);
    }

    private List<Map<String, Object>> toResponse(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Message m : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("time", m.getTimestamp().toString());
            item.put("sender", m.getSender());
            item.put("message", m.getContent());
            result.add(item);
        }

        return result;
    }
}
