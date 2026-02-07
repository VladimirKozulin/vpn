package com.example.vpn.model;

import lombok.Data;
import java.util.List;

/**
 * Модель конфигурации Xray для генерации config.json
 * Поддерживает VLESS протокол с Reality для обхода DPI
 */
@Data
public class XrayConfig {
    
    private Log log;                  // Настройки логирования
    private Api api;                  // Настройки gRPC API
    private Stats stats;              // Настройки статистики
    private Policy policy;            // Политики для пользователей
    private List<Inbound> inbounds;   // Входящие подключения (от клиентов)
    private List<Outbound> outbounds; // Исходящие подключения (в интернет)
    private Routing routing;          // Правила маршрутизации
    
    @Data
    public static class Inbound {
        private String tag;            // Тег для идентификации inbound
        private int port;              // Порт для прослушивания
        private String protocol;       // Протокол (vless)
        private InboundSettings settings;
        private StreamSettings streamSettings;
    }
    
    @Data
    public static class InboundSettings {
        private List<Client> clients;  // Список разрешенных клиентов
        private String decryption = "none"; // VLESS не использует шифрование на уровне протокола
        private String address;        // Для dokodemo-door
    }
    
    @Data
    public static class Client {
        private String id;             // UUID клиента
        private String email;          // Опциональное имя клиента
        private String flow;           // Flow для XTLS (например "xtls-rprx-vision")
    }
    
    @Data
    public static class StreamSettings {
        private String network = "tcp";      // Тип транспорта (tcp, ws, grpc)
        private String security;             // "none", "tls", "reality"
        private RealitySettings realitySettings;  // Настройки Reality
    }
    
    @Data
    public static class RealitySettings {
        private boolean show = false;        // Debug режим
        private String dest;                 // Целевой сайт (например "www.microsoft.com:443")
        private List<String> serverNames;    // Список разрешённых SNI
        private String privateKey;           // Приватный ключ сервера (x25519)
        private List<String> shortIds;       // Список shortId для клиентов
        private String fingerprint = "chrome"; // uTLS fingerprint
        private Long maxTimeDiff = 0L;       // Максимальная разница времени (мс)
        private String minClientVer = "";    // Минимальная версия клиента
        private String maxClientVer = "";    // Максимальная версия клиента
    }
    
    @Data
    public static class Outbound {
        private String protocol;       // Протокол для исходящих (обычно "freedom")
        private String tag;            // Метка для идентификации
    }
    
    // === Новые классы для gRPC API ===
    
    @Data
    public static class Log {
        private String loglevel = "info"; // debug, info, warning, error, none
    }
    
    @Data
    public static class Api {
        private String tag;            // Тег для идентификации API
        private String listen;         // Адрес для прослушивания (например "127.0.0.1:10085")
        private List<String> services; // Список сервисов (HandlerService, StatsService, etc.)
    }
    
    @Data
    public static class Stats {
        // Пустой объект для включения статистики
    }
    
    @Data
    public static class Policy {
        private java.util.Map<String, PolicyLevel> levels;   // Политики по уровням
        private PolicySystem system;   // Системные политики
    }
    
    @Data
    public static class PolicyLevel {
        private boolean statsUserUplink = true;
        private boolean statsUserDownlink = true;
    }
    
    @Data
    public static class PolicySystem {
        private boolean statsInboundUplink = true;
        private boolean statsInboundDownlink = true;
        private boolean statsOutboundUplink = true;
        private boolean statsOutboundDownlink = true;
    }
    
    @Data
    public static class Routing {
        private List<RoutingRule> rules;
    }
    
    @Data
    public static class RoutingRule {
        private String type;           // "field"
        private List<String> inboundTag;
        private String outboundTag;
    }
}
