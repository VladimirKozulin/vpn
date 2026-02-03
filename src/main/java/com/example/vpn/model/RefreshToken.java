package com.example.vpn.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Refresh токен для обновления JWT токенов
 * Хранится в базе данных для возможности отзыва
 */
@Data
public class RefreshToken {
    
    // ID токена
    private Long id;
    
    // ID пользователя
    private Long userId;
    
    // Сам токен (UUID)
    private String token;
    
    // Дата истечения
    private LocalDateTime expiresAt;
    
    // Дата создания
    private LocalDateTime createdAt;
    
    // Отозван ли токен (для logout)
    private boolean revoked;
}
