package com.example.vpn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO для ответа после успешной аутентификации
 * Содержит JWT access токен, refresh токен и информацию о пользователе
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    
    // JWT access токен для API запросов (короткоживущий)
    private String accessToken;
    
    // Refresh токен для обновления access токена (долгоживущий)
    private String refreshToken;
    
    // ID пользователя
    private Long userId;
    
    // Email пользователя
    private String email;
    
    // Имя пользователя
    private String name;
    
    // Роль пользователя
    private String role;
}
