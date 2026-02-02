package com.example.vpn.controller;

import com.example.vpn.service.ConfigService;
import com.example.vpn.service.XrayService;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * REST API контроллер для управления VPN сервером
 * Предоставляет endpoints для запуска/остановки и получения конфигурации
 */
@Slf4j
@RestController
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
public class VpnController {
    
    private final XrayService xrayService;
    private final ConfigService configService;
    
    /**
     * Запускает VPN сервер (Xray процесс)
     * POST /api/vpn/start или GET для удобства из браузера
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startVpnPost() {
        return startVpn();
    }
    
    @GetMapping("/start")
    public ResponseEntity<Map<String, Object>> startVpnGet() {
        return startVpn();
    }
    
    private ResponseEntity<Map<String, Object>> startVpn() {
        try {
            log.info("Получен запрос на запуск VPN");
            xrayService.startXray();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "VPN сервер успешно запущен",
                "running", true
            ));
        } catch (IOException e) {
            log.error("Ошибка при запуске VPN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Не удалось запустить VPN: " + e.getMessage(),
                    "running", false
                ));
        }
    }
    
    /**
     * Останавливает VPN сервер
     * POST /api/vpn/stop или GET для удобства из браузера
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopVpnPost() {
        return stopVpn();
    }
    
    @GetMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopVpnGet() {
        return stopVpn();
    }
    
    private ResponseEntity<Map<String, Object>> stopVpn() {
        log.info("Получен запрос на остановку VPN");
        xrayService.stopXray();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "VPN сервер остановлен",
            "running", false
        ));
    }
    
    /**
     * Проверяет статус VPN сервера
     * GET /api/vpn/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean isRunning = xrayService.isRunning();
        
        return ResponseEntity.ok(Map.of(
            "running", isRunning,
            "message", isRunning ? "VPN сервер работает" : "VPN сервер остановлен"
        ));
    }
    
    /**
     * Возвращает VLESS ссылку для импорта в клиент
     * GET /api/vpn/config/link
     */
    @GetMapping("/config/link")
    public ResponseEntity<Map<String, String>> getConfigLink() {
        String vlessLink = configService.generateVlessLink();
        
        return ResponseEntity.ok(Map.of(
            "link", vlessLink,
            "instruction", "Скопируйте эту ссылку и вставьте в v2rayNG (Import from clipboard)"
        ));
    }
    
    /**
     * Возвращает HTML страницу с QR кодом
     * GET /api/vpn/config/qr
     * Открывайте в браузере для сканирования телефоном
     */
    @GetMapping(value = "/config/qr", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getConfigQr() {
        try {
            String htmlPage = configService.generateQrPage();
            return ResponseEntity.ok(htmlPage);
        } catch (WriterException | IOException e) {
            log.error("Ошибка генерации QR кода", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<h1>Ошибка генерации QR кода: " + e.getMessage() + "</h1>");
        }
    }
    
    /**
     * Перезапускает VPN сервер (остановка + запуск)
     * POST /api/vpn/restart или GET для удобства из браузера
     */
    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restartVpnPost() {
        return restartVpn();
    }
    
    @GetMapping("/restart")
    public ResponseEntity<Map<String, Object>> restartVpnGet() {
        return restartVpn();
    }
    
    private ResponseEntity<Map<String, Object>> restartVpn() {
        try {
            log.info("Получен запрос на перезапуск VPN");
            xrayService.stopXray();
            Thread.sleep(1000); // Небольшая пауза между остановкой и запуском
            xrayService.startXray();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "VPN сервер перезапущен",
                "running", true
            ));
        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при перезапуске VPN", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Не удалось перезапустить VPN: " + e.getMessage()
                ));
        }
    }
}
