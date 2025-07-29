package com.example.chat.service;

import com.example.chat.entity.Message;

import java.util.concurrent.BlockingQueue;

public interface OnlineUserService {
    void waitForMessages(String username, BlockingQueue<Message> queue);
    BlockingQueue<Message> getWaitingQueue(String username);
    void removeWaitingQueue(String username);
}
