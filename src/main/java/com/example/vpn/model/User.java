package com.example.vpn.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Пользователь системы VPN
 * Может иметь несколько VPN клиентов (устройств)
 * Аутентификация через Keycloak
 */
@Data
public class User {
    
    // ID в базе данных
    private Long id;
    
    // UUID пользователя из Keycloak (subject из JWT)
    private String keycloakId;
    
    // Email для входа (уникальный)
    private String email;
    
    // Имя пользователя
    private String name;
    
    // Роль пользователя
    private UserRole role = UserRole.USER;
    
    // Дата создания аккаунта
    private LocalDateTime createdAt;
    
    // Дата последнего входа
    private LocalDateTime lastLoginAt;
}
