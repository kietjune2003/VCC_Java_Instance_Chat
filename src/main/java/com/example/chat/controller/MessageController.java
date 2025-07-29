package com.example.chat.controller;

import com.example.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
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
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("username") String username,
            @RequestPart(value = "message", required = false) String message,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            Map<String, Object> result = messageService.sendMessage(authHeader, username, message, file);
            int status = (int) result.get("status");
            if (status == 404) {
                return ResponseEntity.status(404).body(result.get("message"));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("File handling error");
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(@RequestHeader("Authorization") String authHeader) {
        try {
            List<Map<String, Object>> messages = messageService.getMessages(authHeader);
            return ResponseEntity.ok(messages);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Interrupted");
        }
    }

    @GetMapping("/file/{filename}")
    public ResponseEntity<?> getFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String filename
    ) {
        try {
            Resource resource = messageService.getFile(authHeader, filename);
            return ResponseEntity.ok(resource);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body("Forbidden");
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(500).body("File error");
        }
    }
}
