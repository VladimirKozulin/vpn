package com.example.vpn.model;

import lombok.Data;

/**
 * VPN клиент - конечный пользователь VPN сервиса
 * Каждому клиенту выдается уникальный UUID для подключения
 */
@Data
public class VpnClient {
    
    // Уникальный UUID для Xray (генерируется автоматически)
    private String uuid;
    
    // Информация об устройстве (например: "Samsung Galaxy S21, Android 12")
    private String deviceInfo;
    
    // Активен ли клиент (можно отключить без удаления)
    private Boolean isActive = true;
}
