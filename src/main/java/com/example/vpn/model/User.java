package com.example.vpn.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Пользователь системы VPN
 * Может иметь несколько VPN клиентов (устройств)
 */
@Data
public class User {
    
    // ID в базе данных
    private Long id;
    
    // Email для входа (уникальный)
    private String email;
    
    // Хеш пароля (BCrypt)
    private String passwordHash;
    
    // Имя пользователя
    private String name;
    
    // Роль пользователя
    private UserRole role = UserRole.USER;
    
    // Дата создания аккаунта
    private LocalDateTime createdAt;
}
