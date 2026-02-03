package com.example.vpn.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * VPN клиент - конечный пользователь VPN сервиса
 * Каждому клиенту выдается уникальный UUID для подключения
 * Может быть привязан к пользователю или быть гостевым (userId = null)
 */
@Data
public class VpnClient {
    
    // ID в базе данных
    private Long id;
    
    // ID пользователя-владельца (null для гостевых клиентов)
    private Long userId;
    
    // Уникальный UUID для Xray (генерируется автоматически)
    private String uuid;
    
    // Информация об устройстве (например: "Samsung Galaxy S21, Android 12")
    private String deviceInfo;
    
    // IP адрес с которого создан клиент
    private String ipAddress;
    
    // Страна по геолокации
    private String country;
    
    // Активен ли клиент (можно отключить без удаления)
    private Boolean isActive = true;
    
    // Лимит трафика в GB
    private Integer trafficLimitGb = 100;
    
    // Использовано трафика в GB
    private Double trafficUsedGb = 0.0;
    
    // Дата окончания подписки
    private LocalDateTime expiresAt;
    
    // Последнее подключение
    private LocalDateTime lastConnectedAt;
    
    // Дата создания
    private LocalDateTime createdAt;
}
