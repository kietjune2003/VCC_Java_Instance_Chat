package com.example.chat.service;

import java.util.List;

public interface FriendService {


    List<String> getFriends(String authHeader, String userAgent) throws Exception;

    String sendFriendRequest(String authHeader, String userAgent, String toUsername) throws Exception;

    String respondToFriendRequest(String authHeader, String userAgent, String fromUsername, boolean accepted) throws Exception;
}
