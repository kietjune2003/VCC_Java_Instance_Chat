package com.example.chat.service.impl;

import com.example.chat.entity.Message;
import com.example.chat.service.OnlineUserService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserServiceImpl implements OnlineUserService {

    private final Map<String, BlockingQueue<Message>> userQueues = new ConcurrentHashMap<>();

    @Override
    public void waitForMessages(String username, BlockingQueue<Message> queue) {
        userQueues.put(username, queue);
    }

    @Override
    public BlockingQueue<Message> getWaitingQueue(String username) {
        return userQueues.get(username);
    }

    @Override
    public void removeWaitingQueue(String username) {
        userQueues.remove(username);
    }
}
