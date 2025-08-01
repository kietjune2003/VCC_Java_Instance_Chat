package com.example.chat.controller;

import com.example.chat.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FriendController {

    private final FriendService friendService;

    /**
     * API lấy danh sách bạn bè của người dùng.
     * @param authHeader Header chứa Bearer token
     * @param userAgent Thông tin User-Agent từ header yêu cầu
     * @return ResponseEntity chứa danh sách bạn bè
     */
    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(@RequestHeader("Authorization") String authHeader,
                                        @RequestHeader("User-Agent") String userAgent) throws Exception {
        log.info("API /api/friends called for user with userAgent '{}'", userAgent);

        // Gọi service lấy danh sách bạn bè và trả về kết quả
        return ResponseEntity.ok(friendService.getFriends(authHeader, userAgent)); // Truyền thêm userAgent vào service
    }
}
