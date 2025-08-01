package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private String friendsJson; // JSON danh sách bạn bè
    @Column(length = 512)
    private String accessToken;
    private LocalDateTime tokenCreatedAt;
    private LocalDateTime tokenExpiresAt;

    @Column(length = 512)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiresAt;
}
