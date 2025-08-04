package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String username;

    private String passwordHash;

    @Column(columnDefinition = "TEXT")
    private String friendsJson;
    @Column(columnDefinition = "TEXT")
    private String friendRequests; // JSON danh sách lời mời kết bạn (danh sách username)

}
