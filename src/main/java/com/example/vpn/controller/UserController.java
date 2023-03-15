package com.example.vpn.controller;

import com.example.vpn.model.User;
import com.example.vpn.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API контроллер для работы с пользователями
 * Заменяет AuthController - авторизация теперь через Keycloak
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final KeycloakUserService keycloakUserService;
    
    /**
     * Получить информацию о текущем пользователе
     * GET /api/users/me
     * Автоматически синхронизирует данные с Keycloak при первом входе
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String email = jwt.getClaim("email");
            log.info("Запрос информации о пользователе: {}", email);
            
            // Синхронизируем пользователя с Keycloak
            User user = keycloakUserService.syncUserFromKeycloak(jwt);
            
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "keycloakId", user.getKeycloakId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt().toString(),
                "lastLoginAt", user.getLastLoginAt().toString()
            ));
        } catch (Exception e) {
            log.error("Ошибка получения информации о пользователе", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Ошибка получения данных пользователя"));
        }
    }
    
    /**
     * Получить информацию о JWT токене (для отладки)
     * GET /api/users/token-info
     */
    @GetMapping("/token-info")
    public ResponseEntity<?> getTokenInfo(@AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(Map.of(
                "subject", jwt.getSubject(),
                "email", jwt.getClaim("email"),
                "name", jwt.getClaim("preferred_username"),
                "issuedAt", jwt.getIssuedAt().toString(),
                "expiresAt", jwt.getExpiresAt().toString(),
                "claims", jwt.getClaims()
            ));
        } catch (Exception e) {
            log.error("Ошибка получения информации о токене", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Ошибка получения данных токена"));
        }
    }
}
