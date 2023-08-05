package com.example.vpn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

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
    
    // Путь к конфигурационному файлу Xray
    private String configPath;
    
    // Reality настройки
    private Reality reality = new Reality();
    
    @Data
    public static class Reality {
        // Включить Reality протокол
        private boolean enabled = true;
        
        // Целевой сайт для маскировки (например "www.microsoft.com:443")
        private String dest = "www.microsoft.com:443";
        
        // Список SNI (Server Name Indication)
        private List<String> serverNames = List.of("www.microsoft.com");
        
        // Приватный ключ (генерируется автоматически если пустой)
        private String privateKey = "";
        
        // Публичный ключ (генерируется автоматически если пустой)
        private String publicKey = "";
        
        // Список shortIds (пустая строка = можно подключаться без shortId)
        private List<String> shortIds = List.of("", "6ba85179e30d4fc2");
        
        // uTLS fingerprint
        private String fingerprint = "chrome";
    }
}

