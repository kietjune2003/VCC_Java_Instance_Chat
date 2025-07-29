package com.example.chat.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface MessageService {

    Map<String, Object> sendMessage(String authHeader, String username, String message, MultipartFile file) throws IOException;

    List<Map<String, Object>> getMessages(String authHeader) throws InterruptedException;

    Resource getFile(String authHeader, String filename) throws IOException, SecurityException;
}
