package com.example.vpn.service;

import com.example.vpn.model.User;
import com.example.vpn.model.UserRole;
import com.example.vpn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с пользователями из Keycloak
 * Синхронизирует данные между Keycloak и локальной БД
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {
    
    private final UserRepository userRepository;
    
    /**
     * Синхронизировать пользователя из Keycloak в локальную БД
     * Вызывается при каждом запросе для обеспечения актуальности данных
     */
    public User syncUserFromKeycloak(Jwt jwt) {
        String keycloakId = jwt.getSubject(); // UUID из Keycloak
        String email = jwt.getClaim("email");
        String name = jwt.getClaim("preferred_username");
        
        // Извлекаем роли из realm_access.roles
        UserRole role = extractRoleFromJwt(jwt);
        
        // Проверяем есть ли пользователь в нашей БД
        Optional<User> existingUser = userRepository.findByKeycloakId(keycloakId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Обновляем данные если изменились
            boolean needsUpdate = false;
            
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
                needsUpdate = true;
            }
            
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                needsUpdate = true;
            }
            
            if (user.getRole() != role) {
                user.setRole(role);
                needsUpdate = true;
            }
            
            // Обновляем время последнего входа
            user.setLastLoginAt(LocalDateTime.now());
            needsUpdate = true;
            
            if (needsUpdate) {
                userRepository.update(user);
                log.info("Обновлены данные пользователя: {}", email);
            }
            
            return user;
        }
        
        // Создаем нового пользователя
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setName(name != null ? name : email);
        user.setRole(role);
        user.setLastLoginAt(LocalDateTime.now());
        
        user = userRepository.create(user);
        log.info("Создан новый пользователь из Keycloak: {}", email);
        
        return user;
    }
    
    /**
     * Получить текущего пользователя из SecurityContext
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не аутентифицирован");
        }
        
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return syncUserFromKeycloak(jwt);
        }
        
        throw new RuntimeException("Неверный тип аутентификации");
    }
    
    /**
     * Получить текущий JWT токен
     */
    public Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        
        throw new RuntimeException("JWT токен не найден");
    }
    
    /**
     * Извлечь роль из JWT токена
     * Keycloak хранит роли в realm_access.roles
     */
    private UserRole extractRoleFromJwt(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            
            // Проверяем наличие роли admin
            if (roles.contains("admin") || roles.contains("ADMIN")) {
                return UserRole.ADMIN;
            }
        }
        
        return UserRole.USER;
    }
    
    /**
     * Проверить является ли текущий пользователь администратором
     */
    public boolean isCurrentUserAdmin() {
        try {
            User user = getCurrentUser();
            return user.getRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
}
