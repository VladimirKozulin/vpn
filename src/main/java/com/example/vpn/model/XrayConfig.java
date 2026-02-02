package com.example.vpn.model;

import lombok.Data;
import java.util.List;

/**
 * Модель конфигурации Xray для генерации config.json
 * Это упрощенная версия, содержащая только необходимые поля для VLESS протокола
 */
@Data
public class XrayConfig {
    
    private List<Inbound> inbounds;  // Входящие подключения (от клиентов)
    private List<Outbound> outbounds; // Исходящие подключения (в интернет)
    
    @Data
    public static class Inbound {
        private int port;              // Порт для прослушивания
        private String protocol;       // Протокол (vless)
        private InboundSettings settings;
        private StreamSettings streamSettings;
    }
    
    @Data
    public static class InboundSettings {
        private List<Client> clients;  // Список разрешенных клиентов
        private String decryption = "none"; // VLESS не использует шифрование на уровне протокола
    }
    
    @Data
    public static class Client {
        private String id;             // UUID клиента
        private String email;          // Опциональное имя клиента
    }
    
    @Data
    public static class StreamSettings {
        private String network = "tcp"; // Тип транспорта (tcp, ws, grpc)
        private TlsSettings security;   // Настройки TLS
    }
    
    @Data
    public static class TlsSettings {
        // Пока оставим пустым, TLS настроим позже
        // Для начального тестирования можно без TLS (небезопасно, но работает)
    }
    
    @Data
    public static class Outbound {
        private String protocol;       // Протокол для исходящих (обычно "freedom")
        private String tag;            // Метка для идентификации
    }
}
