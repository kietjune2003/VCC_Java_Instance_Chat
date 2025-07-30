package com.example.chat.service.impl;

import com.example.chat.entity.Message;
import com.example.chat.service.OnlineUserService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserServiceImpl implements OnlineUserService {

    /**
     * Bản đồ chứa hàng đợi chờ tin nhắn cho từng user đang online.
     * Key: tên người dùng
     * Value: BlockingQueue dùng để chứa message mới gửi đến trong thời gian chờ long-polling
     */
    private final Map<String, BlockingQueue<Message>> userQueues = new ConcurrentHashMap<>();

    /**
     * Khi một user gửi yêu cầu GET /messages, họ sẽ chờ tối đa 10 giây để nhận tin nhắn mới.
     * Ta lưu queue của user vào danh sách đang chờ để khi có tin nhắn mới thì push vào hàng đợi này.
     * @param username tên người dùng đang chờ
     * @param queue BlockingQueue sẽ nhận tin nhắn nếu có trong thời gian long-polling
     */
    @Override
    public void waitForMessages(String username, BlockingQueue<Message> queue) {
        userQueues.put(username, queue);
    }

    /**
     * Khi có tin nhắn mới, service gửi tin sẽ gọi method này để kiểm tra xem user có đang online không.
     * Nếu có → gửi tin nhắn ngay thông qua queue.
     * @param username tên người nhận
     * @return queue nếu người nhận đang chờ; null nếu không
     */
    @Override
    public BlockingQueue<Message> getWaitingQueue(String username) {
        return userQueues.get(username);
    }

    /**
     * Sau khi long-polling kết thúc (dù có nhận được tin nhắn hay không),
     * cần loại bỏ user khỏi danh sách đang chờ để tránh memory leak.
     * @param username tên người dùng đã ngắt kết nối long-polling
     */
    @Override
    public void removeWaitingQueue(String username) {
        userQueues.remove(username);
    }
}
