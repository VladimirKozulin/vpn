package com.example.vpn.listener;

import com.example.vpn.config.VpnProperties;
import com.example.vpn.service.RealityKeyService;
import com.example.vpn.service.XrayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Слушатель события готовности приложения
 * Автоматически запускает Xray при старте Spring Boot
 * Генерирует Reality ключи если их нет
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupListener {
    
    private final XrayService xrayService;
    private final VpnProperties vpnProperties;
    private final RealityKeyService realityKeyService;
    
    /**
     * Запускается когда Spring Boot приложение полностью готово
     * Автоматически инициализирует и запускает Xray процесс
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=".repeat(60));
        log.info("🚀 Инициализация VPN сервера");
        log.info("=".repeat(60));
        log.info("📍 IP адрес сервера: {}", vpnProperties.getServerAddress());
        log.info("🔌 Порт VPN: {}", vpnProperties.getXrayPort());
        
        try {
            // Проверяем и генерируем Reality ключи если нужно
            if (vpnProperties.getReality().isEnabled()) {
                log.info("🔐 Reality протокол: ВКЛЮЧЕН");
                
                if (vpnProperties.getReality().getPrivateKey().isEmpty() || 
                    vpnProperties.getReality().getPublicKey().isEmpty()) {
                    
                    log.info("🔑 Reality ключи не найдены, генерируем новые...");
                    RealityKeyService.RealityKeys keys = realityKeyService.generateKeys(
                        vpnProperties.getXrayPath()
                    );
                    
                    // Обновляем properties (только в памяти, не сохраняем в файл)
                    vpnProperties.getReality().setPrivateKey(keys.getPrivateKey());
                    vpnProperties.getReality().setPublicKey(keys.getPublicKey());
                    
                    log.info("✅ Reality ключи сгенерированы");
                    log.info("📋 ВАЖНО: Сохраните эти ключи в application.yml:");
                    log.info("vpn.reality.private-key: {}", keys.getPrivateKey());
                    log.info("vpn.reality.public-key: {}", keys.getPublicKey());
                } else {
                    log.info("✅ Reality ключи загружены из конфигурации");
                    log.info("Private key: {}...", vpnProperties.getReality().getPrivateKey()
                        .substring(0, Math.min(20, vpnProperties.getReality().getPrivateKey().length())));
                    log.info("Public key: {}...", vpnProperties.getReality().getPublicKey()
                        .substring(0, Math.min(20, vpnProperties.getReality().getPublicKey().length())));
                }
                
                log.info("🎭 Маскировка под: {}", vpnProperties.getReality().getDest());
                log.info("🏷️  SNI: {}", vpnProperties.getReality().getServerNames());
            } else {
                log.warn("⚠️  Reality протокол: ОТКЛЮЧЕН");
                log.warn("⚠️  Соединение НЕ защищено от DPI!");
            }
            
            // Генерируем конфигурационный файл
            xrayService.generateConfigFile();
            
            // Запускаем Xray
            xrayService.startXray();
            
            log.info("=".repeat(60));
            log.info("✅ VPN сервер запущен");
            log.info("🌐 Веб-интерфейс: https://localhost:8080/");
            log.info("=".repeat(60));
        } catch (Exception e) {
            log.error("=".repeat(60));
            log.error("❌ Ошибка при запуске VPN сервера", e);
            log.error("=".repeat(60));
        }
    }
}
