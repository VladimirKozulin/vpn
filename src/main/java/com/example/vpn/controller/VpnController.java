package com.example.vpn.controller;

import com.example.vpn.service.XrayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * REST API контроллер для управления VPN сервером (Xray процессом)
 */
@Slf4j
@RestController
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
public class VpnController {
    
    private final XrayService xrayService;
    
    /**
     * Запускает VPN сервер (Xray процесс)
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
     * Перезапускает VPN сервер
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
            xrayService.restartXray();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "VPN сервер перезапущен",
                "running", true
            ));
        } catch (IOException e) {
            log.error("Ошибка при перезапуске VPN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Не удалось перезапустить VPN: " + e.getMessage()
                ));
        }
    }
}

