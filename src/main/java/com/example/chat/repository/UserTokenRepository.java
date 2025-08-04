package com.example.chat.repository;

import com.example.chat.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    @Query("SELECT ut FROM UserToken ut WHERE ut.user.username = :username AND ut.tokenExpiresAt > :now")
    List<UserToken> findValidTokensByUser(@Param("username") String username, @Param("now") LocalDateTime now);

    Optional<UserToken> findByAccessToken(String token);
}
