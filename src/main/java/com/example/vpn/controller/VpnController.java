package com.example.vpn.controller;

import com.example.vpn.service.XrayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API контроллер для проверки статуса VPN сервера
 * VPN запускается автоматически при старте приложения
 */
@Slf4j
@RestController
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
public class VpnController {
    
    private final XrayService xrayService;
    
    private static final String VPN_RUNNING_MESSAGE = "VPN сервер работает";
    private static final String VPN_STOPPED_MESSAGE = "VPN сервер остановлен";
    
    /**
     * Проверяет статус VPN сервера
     * GET /api/vpn/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean isRunning = xrayService.isRunning();
        
        Map<String, Object> response = new HashMap<>();
        response.put("running", isRunning);
        response.put("message", isRunning ? VPN_RUNNING_MESSAGE : VPN_STOPPED_MESSAGE);

        return ResponseEntity.ok(response);
    }

}

