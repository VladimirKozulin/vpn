package com.example.vpn.controller;

import com.example.vpn.dto.ClaimClientRequest;
import com.example.vpn.model.User;
import com.example.vpn.model.VpnClient;
import com.example.vpn.service.ConfigService;
import com.example.vpn.service.KeycloakUserService;
import com.example.vpn.service.VpnClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API контроллер для управления VPN клиентами
 * Обновлено для работы с Keycloak
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class VpnClientController {
    
    private final VpnClientService vpnClientService;
    private final ConfigService configService;
    private final KeycloakUserService keycloakUserService;
    
    /**
     * Создать нового VPN клиента
     * POST /api/clients
     * Требует аутентификацию через Keycloak
     */
    @PostMapping
    public ResponseEntity<?> createClient(
            @RequestBody VpnClient client,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            log.info("Запрос на создание клиента");
            
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            client.setUserId(user.getId());
            
            log.info("Создание клиента для пользователя ID: {}", user.getId());
            
            VpnClient created = vpnClientService.createClient(client);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Ошибка создания клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получить всех клиентов текущего пользователя
     * GET /api/clients/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyClients(@AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            List<VpnClient> clients = vpnClientService.getClientsByUserId(user.getId());
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            log.error("Ошибка получения клиентов пользователя", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
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
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getClient(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            
            if (!vpnClientService.isClientOwnedByUser(id, user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }
            
            return vpnClientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Ошибка получения клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Обновить клиента
     * PUT /api/clients/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(
            @PathVariable Long id,
            @RequestBody VpnClient client,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            
            if (!vpnClientService.isClientOwnedByUser(id, user.getId())) {
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
     */
    @PostMapping("/claim")
    public ResponseEntity<?> claimClient(
            @Valid @RequestBody ClaimClientRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            VpnClient claimed = vpnClientService.claimClient(request.getClientUuid(), user.getId());
            return ResponseEntity.ok(claimed);
        } catch (Exception e) {
            log.error("Ошибка привязки клиента", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Переключить активность клиента
     * POST /api/clients/{id}/toggle
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleClient(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            
            if (!vpnClientService.isClientOwnedByUser(id, user.getId())) {
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
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            
            if (!vpnClientService.isClientOwnedByUser(id, user.getId())) {
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
     */
    @GetMapping("/{id}/config")
    public ResponseEntity<?> getClientConfig(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            
            if (!vpnClientService.isClientOwnedByUser(id, user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }
            
            return vpnClientService.getClientById(id)
                .map(client -> {
                    String link = configService.generateVlessLink(client);
                    return ResponseEntity.ok(Map.of(
                        "link", link,
                        "instruction", "Скопируйте эту ссылку и вставьте в v2rayNG"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Ошибка получения конфига клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
