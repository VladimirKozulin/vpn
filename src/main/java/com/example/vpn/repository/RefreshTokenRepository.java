package com.example.vpn.repository;

import com.example.vpn.model.RefreshToken;
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
 * Репозиторий для работы с refresh токенами в Tarantool
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    
    private final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Создать новый refresh токен
     */
    public RefreshToken create(RefreshToken token) {
        try {
            List<?> seqResult = tarantoolClient.call("box.sequence.refresh_tokens_seq:next").get();
            Long newId = ((Number) seqResult.get(0)).longValue();
            token.setId(newId);
            token.setCreatedAt(LocalDateTime.now());
            
            tarantoolClient.call(
                "box.space.refresh_tokens:insert",
                List.of(List.of(
                    token.getId(),
                    token.getUserId(),
                    token.getToken(),
                    token.getExpiresAt().format(FORMATTER),
                    token.getCreatedAt().format(FORMATTER),
                    token.isRevoked()
                ))
            ).get();
            
            log.info("Создан refresh токен с ID: {} для пользователя: {}", token.getId(), token.getUserId());
            return token;
            
        } catch (Exception e) {
            log.error("Ошибка создания refresh токена: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать refresh токен: " + e.getMessage(), e);
        }
    }
    
    /**
     * Найти токен по значению
     */
    public Optional<RefreshToken> findByToken(String token) {
        try {
            List<?> result = tarantoolClient.call(
                "box.space.refresh_tokens.index.token:select",
                List.of(token)
            ).get();
            
            if (result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            if (tuple.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(mapToRefreshToken(tuple));
            
        } catch (Exception e) {
            log.error("Ошибка поиска refresh токена: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Отозвать токен (logout)
     */
    public void revokeToken(String token) {
        try {
            tarantoolClient.call(
                "box.space.refresh_tokens.index.token:update",
                List.of(token, List.of(List.of("=", 6, true)))
            ).get();
            
            log.info("Токен отозван: {}", token);
            
        } catch (Exception e) {
            log.error("Ошибка отзыва токена: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось отозвать токен: " + e.getMessage(), e);
        }
    }
    
    /**
     * Отозвать все токены пользователя
     */
    public void revokeAllUserTokens(Long userId) {
        try {
            List<?> result = tarantoolClient.call(
                "box.space.refresh_tokens.index.user_id:select",
                List.of(userId)
            ).get();
            
            for (Object obj : result) {
                List<?> tuple = (List<?>) obj;
                String token = (String) tuple.get(2);
                revokeToken(token);
            }
            
            log.info("Все токены пользователя {} отозваны", userId);
            
        } catch (Exception e) {
            log.error("Ошибка отзыва всех токенов пользователя: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Удалить истекшие токены (для периодической очистки)
     */
    public void deleteExpiredTokens() {
        try {
            String now = LocalDateTime.now().format(FORMATTER);
            tarantoolClient.call(
                "delete_expired_refresh_tokens",
                List.of(now)
            ).get();
            
            log.info("Истекшие токены удалены");
            
        } catch (Exception e) {
            log.error("Ошибка удаления истекших токенов: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Маппинг tuple в RefreshToken
     */
    private RefreshToken mapToRefreshToken(List<?> tuple) {
        RefreshToken token = new RefreshToken();
        token.setId(((Number) tuple.get(0)).longValue());
        token.setUserId(((Number) tuple.get(1)).longValue());
        token.setToken((String) tuple.get(2));
        token.setExpiresAt(LocalDateTime.parse((String) tuple.get(3), FORMATTER));
        token.setCreatedAt(LocalDateTime.parse((String) tuple.get(4), FORMATTER));
        token.setRevoked((Boolean) tuple.get(5));
        return token;
    }
}
