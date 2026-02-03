package com.example.vpn.service;

import com.example.vpn.model.RefreshToken;
import com.example.vpn.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис для работы с refresh токенами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Value("${jwt.refresh-expiration:2592000000}") // 30 дней в миллисекундах
    private Long refreshExpiration;
    
    /**
     * Создать новый refresh токен для пользователя
     */
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
        token.setRevoked(false);
        
        return refreshTokenRepository.create(token);
    }
    
    /**
     * Проверить валидность refresh токена
     */
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Refresh токен не найден"));
        
        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh токен отозван");
        }
        
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh токен истек");
        }
        
        return refreshToken;
    }
    
    /**
     * Отозвать токен (logout)
     */
    public void revokeToken(String token) {
        refreshTokenRepository.revokeToken(token);
    }
    
    /**
     * Отозвать все токены пользователя
     */
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }
}
