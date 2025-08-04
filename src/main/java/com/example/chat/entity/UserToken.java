package com.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", referencedColumnName = "username")
    private User user;

    @Column(length = 512)
    private String accessToken;

    private LocalDateTime tokenCreatedAt;
    private LocalDateTime tokenExpiresAt;

    @Column(length = 512)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiresAt;
}
