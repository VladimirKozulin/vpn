package com.example.vpn.listener;

import com.example.vpn.service.XrayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Слушатель события готовности приложения
 * Автоматически запускает Xray при старте Spring Boot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupListener {
    
    private final XrayService xrayService;
    
    /**
     * Запускается когда Spring Boot приложение полностью готово
     * Автоматически инициализирует и запускает Xray процесс
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== Приложение готово, инициализация VPN сервера ===");
        
        try {
            xrayService.startXray();
            log.info("✅ VPN сервер успешно запущен при старте приложения");
        } catch (Exception e) {
            log.error("❌ Ошибка при запуске VPN сервера", e);
            // Не прерываем запуск приложения, но логируем ошибку
        }
    }
}
