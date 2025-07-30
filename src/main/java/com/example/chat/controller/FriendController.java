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

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(@RequestHeader("Authorization") String authHeader) throws Exception {
        log.info("API /api/friends called");
        return ResponseEntity.ok(friendService.getFriends(authHeader));
    }
}
