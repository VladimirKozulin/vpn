package com.example.vpn.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Временный клиент, ожидающий подключения
 * Хранится в памяти (не в БД)
 * Если за 5 минут не подключился - удаляется
 */
@Data
public class PendingClient {
    
    // Уникальный UUID клиента
    private String uuid;
    
    // Информация об устройстве
    private String deviceInfo;
    
    // Время создания
    private LocalDateTime createdAt;
    
    // Время истечения (createdAt + 5 минут)
    private LocalDateTime expiresAt;
    
    public PendingClient(String uuid, String deviceInfo) {
        this.uuid = uuid;
        this.deviceInfo = deviceInfo;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }
    
    /**
     * Проверяет истёк ли срок ожидания
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
