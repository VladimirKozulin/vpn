package com.example.vpn.controller;

import com.example.vpn.dto.ClaimClientRequest;
import com.example.vpn.model.VpnClient;
import com.example.vpn.security.JwtUtil;
import com.example.vpn.service.ConfigService;
import com.example.vpn.service.VpnClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API контроллер для управления VPN клиентами
 * Поддерживает как гостевой режим, так и авторизованных пользователей
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class VpnClientController {
    
    private final VpnClientService vpnClientService;
    private final ConfigService configService;
    private final JwtUtil jwtUtil;
    
    /**
     * Создать нового VPN клиента
     * POST /api/clients
     * 
     * БЕЗ токена - создает гостевого клиента (userId = null)
     * С токеном - создает клиента привязанного к пользователю
     */
    @PostMapping
    public ResponseEntity<VpnClient> createClient(
            @RequestBody VpnClient client,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("Запрос на создание клиента");
            
            // Если есть токен - извлекаем userId и привязываем клиента
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);
                client.setUserId(userId);
                log.info("Создание клиента для пользователя ID: {}", userId);
            } else {
                // Гостевой клиент
                client.setUserId(null);
                log.info("Создание гостевого клиента");
            }
            
            VpnClient created = vpnClientService.createClient(client);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Ошибка создания клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить всех клиентов текущего пользователя
     * GET /api/clients/my
     * Требует JWT токен
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyClients(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            
            List<VpnClient> clients = vpnClientService.getClientsByUserId(userId);
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            log.error("Ошибка получения клиентов пользователя", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Неверный токен"));
        }
    }
    
    /**
     * Получить всех клиентов (только для админов)
     * GET /api/clients
     */
    @GetMapping
    public ResponseEntity<List<VpnClient>> getAllClients() {
        List<VpnClient> clients = vpnClientService.getAllClients();
        return ResponseEntity.ok(clients);
    }
    
    /**
     * Получить клиента по ID
     * GET /api/clients/{id}
     * Требует JWT токен, возвращает только свои клиенты
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getClient(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            
            // Проверяем что клиент принадлежит пользователю
            if (!vpnClientService.isClientOwnedByUser(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }
            
            return vpnClientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Ошибка получения клиента", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Неверный токен"));
        }
    }
    
    /**
     * Обновить клиента
     * PUT /api/clients/{id}
     * Требует JWT токен, можно обновлять только свои клиенты
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(
            @PathVariable Long id,
            @RequestBody VpnClient client,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            
            // Проверяем что клиент принадлежит пользователю
            if (!vpnClientService.isClientOwnedByUser(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }
            
            client.setId(id);
            VpnClient updated = vpnClientService.updateClient(client);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Ошибка обновления клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Привязать гостевого клиента к аккаунту
     * POST /api/clients/claim
     * Требует JWT токен
     */
    @PostMapping("/claim")
    public ResponseEntity<?> claimClient(
            @Valid @RequestBody ClaimClientRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            
            VpnClient claimed = vpnClientService.claimClient(request.getClientUuid(), userId);
            return ResponseEntity.ok(claimed);
        } catch (Exception e) {
            log.error("Ошибка привязки клиента", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Переключить активность клиента (вкл/выкл)
     * POST /api/clients/{id}/toggle
     * Требует JWT токен
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleClient(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            
            // Проверяем что клиент принадлежит пользователю
            if (!vpnClientService.isClientOwnedByUser(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }
            
            VpnClient toggled = vpnClientService.toggleClient(id);
            return ResponseEntity.ok(toggled);
        } catch (Exception e) {
            log.error("Ошибка переключения клиента", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Удалить клиента
     * DELETE /api/clients/{id}
     * Требует JWT токен
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            
            // Проверяем что клиент принадлежит пользователю
            if (!vpnClientService.isClientOwnedByUser(id, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }
            
            vpnClientService.deleteClient(id);
            return ResponseEntity.ok(Map.of("message", "Клиент удален"));
        } catch (Exception e) {
            log.error("Ошибка удаления клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получить VLESS ссылку для клиента
     * GET /api/clients/{id}/config
     * Доступно для гостей (без токена) и авторизованных пользователей (только свои)
     */
    @GetMapping("/{id}/config")
    public ResponseEntity<?> getClientConfig(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Если есть токен - проверяем владельца
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);
                
                if (!vpnClientService.isClientOwnedByUser(id, userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Доступ запрещен"));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверный токен"));
            }
        }
        
        // Возвращаем конфиг
        return vpnClientService.getClientById(id)
            .map(client -> {
                String link = configService.generateVlessLink(client);
                return ResponseEntity.ok(Map.of(
                    "link", link,
                    "instruction", "Скопируйте эту ссылку и вставьте в v2rayNG"
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
