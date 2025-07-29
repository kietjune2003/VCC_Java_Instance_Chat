package com.example.chat.repository;

import com.example.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByReceiverAndDeliveredFalse(String receiver);
    List<Message> findByReceiver(String receiver);
}
