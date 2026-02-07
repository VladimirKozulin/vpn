package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.example.vpn.model.VpnClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Сервис для генерации конфигурации клиента
 * Создает ссылки для подключения к VPN с поддержкой Reality
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {
    
    private final VpnProperties vpnProperties;
    
    /**
     * Генерирует VLESS ссылку для конкретного клиента
     * Формат: vless://UUID@SERVER:PORT?параметры#название
     * 
     * С Reality: vless://UUID@IP:443?encryption=none&flow=xtls-rprx-vision&security=reality
     *            &pbk=PUBLIC_KEY&fp=chrome&sni=www.microsoft.com&sid=SHORT_ID&type=tcp#NAME
     */
    public String generateVlessLink(String uuid, String deviceInfo) {
        // Базовая часть: vless://UUID@адрес:порт
        String base = String.format("vless://%s@%s:%d",
            uuid,
            vpnProperties.getServerAddress(),
            vpnProperties.getXrayPort()
        );
        
        // Параметры подключения
        StringBuilder params = new StringBuilder("?encryption=none&type=tcp");
        
        if (vpnProperties.getReality().isEnabled()) {
            // Reality параметры
            params.append("&security=reality");
            params.append("&flow=xtls-rprx-vision");
            params.append("&pbk=").append(vpnProperties.getReality().getPublicKey());
            params.append("&fp=").append(vpnProperties.getReality().getFingerprint());
            
            // SNI (Server Name Indication)
            if (!vpnProperties.getReality().getServerNames().isEmpty()) {
                String sni = vpnProperties.getReality().getServerNames().get(0);
                params.append("&sni=").append(sni);
            }
            
            // ShortId (берём первый непустой или пустой)
            String shortId = vpnProperties.getReality().getShortIds().stream()
                .filter(id -> !id.isEmpty())
                .findFirst()
                .orElse("");
            if (!shortId.isEmpty()) {
                params.append("&sid=").append(shortId);
            }
            
            log.info("✅ Сгенерирована VLESS+Reality ссылка для клиента UUID: {}", uuid);
        } else {
            // Без Reality (небезопасно!)
            params.append("&security=none");
            log.warn("⚠️ Сгенерирована VLESS ссылка БЕЗ Reality для клиента UUID: {}", uuid);
        }
        
        // Название подключения (будет отображаться в клиенте)
        String name = deviceInfo != null ? deviceInfo : "VPN-Client";

        name = URLEncoder.encode(name, StandardCharsets.UTF_8);

        String vlessLink = base + params.toString() + "#" + name;
        
        log.debug("VLESS ссылка: {}", vlessLink);
        return vlessLink;
    }
}
