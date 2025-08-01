package com.example.chat.service;

import java.util.List;

public interface FriendService {


    List<String> getFriends(String authHeader, String userAgent) throws Exception;
}
