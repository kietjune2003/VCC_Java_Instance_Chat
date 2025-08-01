package com.example.chat.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface MessageService {

    /**
     * Gửi tin nhắn văn bản hoặc file đến người nhận
     * @param authHeader Header chứa access token
     * @param username Người nhận tin nhắn
     * @param message Nội dung tin nhắn văn bản (nếu có)
     * @param file File đính kèm (nếu có)
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return Map kết quả xử lý tin nhắn
     * @throws IOException lỗi xử lý file
     */
    Map<String, Object> sendMessage(String authHeader, String username, String message, MultipartFile file, String userAgent) throws IOException;

    /**
     * Lấy tin nhắn chờ từ người dùng bằng long polling tối đa 10 giây
     * @param authHeader Header chứa access token
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return Danh sách tin nhắn
     * @throws InterruptedException nếu có lỗi khi polling
     */
    List<Map<String, Object>> getMessages(String authHeader, String userAgent) throws InterruptedException;

    /**
     * Tải file đính kèm từ tin nhắn
     * @param authHeader Header chứa access token
     * @param filename Tên file cần tải
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return Tệp tài nguyên
     * @throws IOException nếu có lỗi khi xử lý file
     * @throws SecurityException nếu người dùng không có quyền truy cập
     */
    Resource getFile(String authHeader, String filename, String userAgent) throws IOException, SecurityException;
}
