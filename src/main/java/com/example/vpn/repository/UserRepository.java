package com.example.vpn.repository;

import com.example.vpn.config.TarantoolConfig;
import com.example.vpn.model.User;
import com.example.vpn.model.UserRole;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями в Tarantool
 * Обновлено для работы с Keycloak
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {
    
    private final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Создать нового пользователя
     */
    public User create(User user) {
        try {
            // Генерируем новый ID из sequence
            List<?> seqResult = tarantoolClient.call("box.sequence.users_seq:next").get();
            Long newId = ((Number) seqResult.get(0)).longValue();
            user.setId(newId);
            user.setCreatedAt(LocalDateTime.now());
            
            if (user.getLastLoginAt() == null) {
                user.setLastLoginAt(LocalDateTime.now());
            }
            
            // Вставляем в базу
            // Формат: id, keycloak_id, email, name, role, created_at, last_login_at
            tarantoolClient.call(
                "box.space.users:insert",
                List.of(List.of(
                    user.getId(),
                    user.getKeycloakId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole().name(),
                    user.getCreatedAt().format(FORMATTER),
                    user.getLastLoginAt().format(FORMATTER)
                ))
            ).get();
            
            log.info("Создан пользователь с ID: {}, email: {}, keycloakId: {}", 
                user.getId(), user.getEmail(), user.getKeycloakId());
            return user;
            
        } catch (Exception e) {
            log.error("Ошибка создания пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать пользователя: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обновить данные пользователя
     */
    public User update(User user) {
        try {
            // Обновляем все поля кроме id и created_at
            tarantoolClient.call(
                "box.space.users:update",
                List.of(
                    user.getId(),
                    List.of(
                        List.of("=", 2, user.getKeycloakId()),
                        List.of("=", 3, user.getEmail()),
                        List.of("=", 4, user.getName()),
                        List.of("=", 5, user.getRole().name()),
                        List.of("=", 7, user.getLastLoginAt().format(FORMATTER))
                    )
                )
            ).get();
            
            log.info("Обновлен пользователь с ID: {}", user.getId());
            return user;
            
        } catch (Exception e) {
            log.error("Ошибка обновления пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обновить пользователя: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти пользователя по Keycloak ID
     */
    public Optional<User> findByKeycloakId(String keycloakId) {
        try {
            List<?> result = tarantoolClient.call(
                "box.space.users.index.keycloak_id:select",
                List.of(keycloakId)
            ).get();
            
            if (result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            if (tuple.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(mapToUser(tuple));
            
        } catch (Exception e) {
            log.error("Ошибка поиска пользователя по keycloakId: {}", keycloakId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Найти пользователя по email
     */
    public Optional<User> findByEmail(String email) {
        try {
            List<?> result = tarantoolClient.call(
                "box.space.users.index.email:select",
                List.of(email)
            ).get();
            
            if (result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            if (tuple.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(mapToUser(tuple));
            
        } catch (Exception e) {
            log.error("Ошибка поиска пользователя по email: {}", email, e);
            return Optional.empty();
        }
    }
    
    /**
     * Найти пользователя по ID
     */
    public Optional<User> findById(Long id) {
        try {
            List<?> result = tarantoolClient.call(
                "box.space.users:select",
                List.of(id)
            ).get();
            
            if (result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            if (tuple.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(mapToUser(tuple));
            
        } catch (Exception e) {
            log.error("Ошибка поиска пользователя по ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    /**
     * Проверить существует ли пользователь с таким email
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
    
    /**
     * Проверить существует ли пользователь с таким Keycloak ID
     */
    public boolean existsByKeycloakId(String keycloakId) {
        return findByKeycloakId(keycloakId).isPresent();
    }
    
    /**
     * Маппинг tuple из Tarantool в объект User
     * Формат: id, keycloak_id, email, name, role, created_at, last_login_at
     */
    private User mapToUser(List<?> tuple) {
        User user = new User();
        user.setId(((Number) tuple.get(0)).longValue());
        user.setKeycloakId((String) tuple.get(1));
        user.setEmail((String) tuple.get(2));
        user.setName((String) tuple.get(3));
        user.setRole(UserRole.valueOf((String) tuple.get(4)));
        user.setCreatedAt(LocalDateTime.parse((String) tuple.get(5), FORMATTER));
        user.setLastLoginAt(LocalDateTime.parse((String) tuple.get(6), FORMATTER));
        return user;
    }
}
