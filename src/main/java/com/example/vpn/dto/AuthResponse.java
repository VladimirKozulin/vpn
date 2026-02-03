package com.example.vpn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO для ответа после успешной аутентификации
 * Содержит JWT токен и информацию о пользователе
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    
    // JWT токен для последующих запросов
    private String token;
    
    // ID пользователя
    private Long userId;
    
    // Email пользователя
    private String email;
    
    // Имя пользователя
    private String name;
    
    // Роль пользователя
    private String role;
}
