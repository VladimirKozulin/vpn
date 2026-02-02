package com.example.vpn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация VPN сервера из application.yml
 */
@Data
@Component
@ConfigurationProperties(prefix = "vpn")
public class VpnProperties {
    
    // Путь к исполняемому файлу Xray
    private String xrayPath;
    
    // Порт, на котором Xray будет слушать VPN подключения
    private int xrayPort;
    
    // Адрес сервера (IP или домен)
    private String serverAddress;
    
    // UUID для идентификации клиента
    private String clientUuid;
}
