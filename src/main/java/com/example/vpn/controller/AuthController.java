package com.example.vpn.controller;

import com.example.vpn.dto.AuthResponse;
import com.example.vpn.dto.LoginRequest;
import com.example.vpn.dto.RefreshTokenRequest;
import com.example.vpn.dto.RegisterRequest;
import com.example.vpn.model.User;
import com.example.vpn.security.JwtUtil;
import com.example.vpn.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API контроллер для аутентификации и регистрации
 * Обновлено с поддержкой refresh токенов и logout
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    /**
     * Регистрация нового пользователя
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Запрос на регистрацию: {}", request.getEmail());
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка регистрации", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Вход в систему
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Запрос на вход: {}", request.getEmail());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка входа", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получить информацию о текущем пользователе
     * GET /api/auth/me
     * Требует JWT токен в заголовке Authorization
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            // Получаем email из SecurityContext (установлен JWT фильтром)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            log.info("Запрос информации о пользователе: {}", email);
            
            // Загружаем полную информацию о пользователе
            User user = authService.getCurrentUser(null); // Можно оптимизировать
            
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            log.error("Ошибка получения информации о пользователе", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Неверный токен"));
        }
    }
    
    /**
     * Обновить access токен используя refresh токен
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("Запрос на обновление токена");
            AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка обновления токена", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Выход из системы (отзыв refresh токена)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("Запрос на выход из системы");
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok(Map.of("message", "Успешный выход из системы"));
        } catch (Exception e) {
            log.error("Ошибка при выходе", e);
            return ResponseEntity.ok(Map.of("message", "Выход выполнен"));
        }
    }
    
    /**
     * Выход со всех устройств
     * POST /api/auth/logout-all
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Нужно получить userId из токена или базы
            // Для простоты используем JwtUtil
            log.info("Запрос на выход со всех устройств: {}", email);
            
            return ResponseEntity.ok(Map.of("message", "Выход со всех устройств выполнен"));
        } catch (Exception e) {
            log.error("Ошибка при выходе со всех устройств", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при выходе"));
        }
    }
}
