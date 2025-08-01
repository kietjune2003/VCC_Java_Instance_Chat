package com.example.chat.controller;

import com.example.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j // Tạo biến log cho toàn controller
public class MessageController {

    private final MessageService messageService;

    /**
     * API gửi tin nhắn (text hoặc file)
     * @param authHeader Header chứa Authorization token
     * @param username Tên người dùng gửi tin nhắn
     * @param message Nội dung tin nhắn (nếu có)
     * @param file Tệp đính kèm (nếu có)
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity kết quả gửi tin nhắn
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("username") String username,
            @RequestPart(value = "message", required = false) String message,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestHeader("User-Agent") String userAgent // Lấy userAgent từ header yêu cầu
    ) {
        try {
            log.info("Received message send request from '{}'", username); // Log input

            Map<String, Object> result = messageService.sendMessage(authHeader, username, message, file, userAgent); // Truyền thêm userAgent vào service
            int status = (int) result.get("status");

            // Log kết quả trả về từ service
            switch (status) {
                case 1 -> log.info("Message delivered directly to '{}'", username);
                case 2 -> log.info("Message queued for '{}'", username);
                case 3 -> log.warn("Unauthorized message attempt: sender not in friend list of '{}'", username);
                case 404 -> log.warn("Message send failed: user '{}' not found", username);
            }

            if (status == 404) {
                return ResponseEntity.status(404).body(result.get("message"));
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid message request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            log.error("File handling error while sending message to '{}': {}", username, e.getMessage());
            return ResponseEntity.status(500).body("File handling error");
        }
    }

    /**
     * API lấy tin nhắn mới (long polling 10s)
     * @param authHeader Header chứa Authorization token
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity danh sách tin nhắn
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader("User-Agent") String userAgent // Lấy userAgent từ header yêu cầu
    ) {
        try {
            log.info("Polling for new messages");

            List<Map<String, Object>> messages = messageService.getMessages(authHeader, userAgent); // Truyền thêm userAgent vào service
            log.debug("Returned {} message(s)", messages.size());

            return ResponseEntity.ok(messages);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Polling thread was interrupted", e);
            return ResponseEntity.status(500).body("Interrupted");
        }
    }

    /**
     * API tải file đính kèm từ tin nhắn
     * @param authHeader Header chứa Authorization token
     * @param filename Tên file cần tải
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity chứa file đính kèm
     */
    @GetMapping("/file/{filename}")
    public ResponseEntity<?> getFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String filename,
            @RequestHeader("User-Agent") String userAgent // Lấy userAgent từ header yêu cầu
    ) {
        try {
            log.info("User requested file: {}", filename);

            Resource resource = messageService.getFile(authHeader, filename, userAgent); // Truyền thêm userAgent vào service
            log.debug("File '{}' successfully returned", filename);
            return ResponseEntity.ok(resource);

        } catch (SecurityException e) {
            log.warn("Access denied when requesting file '{}'", filename);
            return ResponseEntity.status(403).body("Forbidden");

        } catch (NoSuchFileException e) {
            log.warn("Requested file not found: {}", filename);
            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            log.error("Error while retrieving file '{}': {}", filename, e.getMessage());
            return ResponseEntity.status(500).body("File error");
        }
    }
}
