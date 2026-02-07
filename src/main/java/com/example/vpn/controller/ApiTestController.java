package com.example.vpn.controller;

import com.example.vpn.service.XrayGrpcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Контроллер для тестирования Xray API
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class ApiTestController {
    
    private final XrayGrpcService xrayGrpcService;
    
    /**
     * Тест добавления пользователя
     * POST /api/test/add-user
     */
    @PostMapping("/add-user")
    public ResponseEntity<Map<String, Object>> testAddUser(
        @RequestParam(required = false) String uuid,
        @RequestParam(required = false) String email
    ) {
        try {
            String testUuid = uuid != null ? uuid : UUID.randomUUID().toString();
            String testEmail = email != null ? email : "test-user";
            
            log.info("🧪 Тест добавления пользователя: UUID={}, email={}", testUuid, testEmail);
            xrayGrpcService.addUser(testUuid, testEmail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пользователь добавлен через API");
            response.put("uuid", testUuid);
            response.put("email", testEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Ошибка теста добавления", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Тест удаления пользователя
     * DELETE /api/test/remove-user/{uuid}
     */
    @DeleteMapping("/remove-user/{uuid}")
    public ResponseEntity<Map<String, Object>> testRemoveUser(@PathVariable String uuid) {
        try {
            log.info("🧪 Тест удаления пользователя: UUID={}", uuid);
            xrayGrpcService.removeUser(uuid);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пользователь удалён через API");
            response.put("uuid", uuid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Ошибка теста удаления", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Тест получения статистики
     * GET /api/test/stats/{uuid}
     */
    @GetMapping("/stats/{uuid}")
    public ResponseEntity<Map<String, Object>> testGetStats(@PathVariable String uuid) {
        try {
            log.info("🧪 Тест получения статистики: UUID={}", uuid);
            XrayGrpcService.UserStats stats = xrayGrpcService.getUserStats(uuid);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("uuid", uuid);
            response.put("uplink", stats.getUplink());
            response.put("downlink", stats.getDownlink());
            response.put("hasTraffic", stats.hasTraffic());
            response.put("total", stats.getUplink() + stats.getDownlink());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Ошибка теста статистики", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
