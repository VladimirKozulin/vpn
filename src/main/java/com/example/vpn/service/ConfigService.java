package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.example.vpn.model.VpnClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для генерации конфигурации клиента
 * Создает ссылки для подключения к VPN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {
    
    private final VpnProperties vpnProperties;
    
    /**
     * Генерирует VLESS ссылку для конкретного клиента
     * Формат: vless://UUID@SERVER:PORT?параметры#название
     */
    public String generateVlessLink(VpnClient client) {
        // Базовая часть: vless://UUID@адрес:порт
        String base = String.format("vless://%s@%s:%d",
            client.getUuid(),
            vpnProperties.getServerAddress(),
            vpnProperties.getXrayPort()
        );
        
        // Параметры подключения
        String params = "?encryption=none&type=tcp&security=none";
        
        // Название подключения (будет отображаться в клиенте)
        String name = "#" + (client.getDeviceInfo() != null ? 
            client.getDeviceInfo() : "VPN-Client");
        
        String vlessLink = base + params + name;
        
        log.info("Сгенерирована VLESS ссылка для клиента UUID: {}", client.getUuid());
        return vlessLink;
    }
}
