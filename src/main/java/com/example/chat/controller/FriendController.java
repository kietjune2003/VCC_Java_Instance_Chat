package com.example.chat.controller;

import com.example.chat.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/friends")
    public ResponseEntity<?> getFriends(@RequestHeader("Authorization") String authHeader) throws Exception {
        return ResponseEntity.ok(friendService.getFriends(authHeader));
    }
}
