package com.example.vpn.service;

import com.example.vpn.dto.AuthResponse;
import com.example.vpn.dto.LoginRequest;
import com.example.vpn.dto.RegisterRequest;
import com.example.vpn.model.RefreshToken;
import com.example.vpn.model.User;
import com.example.vpn.model.UserRole;
import com.example.vpn.repository.UserRepository;
import com.example.vpn.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Сервис для аутентификации и регистрации пользователей
 * Обновлено с поддержкой refresh токенов и AuthenticationManager
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    
    /**
     * Регистрация нового пользователя
     */
    public AuthResponse register(RegisterRequest request) {
        // Проверяем существует ли пользователь с таким email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        
        // Валидация пароля
        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Пароль должен быть минимум 6 символов");
        }
        
        // Создаем нового пользователя
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setRole(UserRole.USER);
        
        // Сохраняем в базу
        user = userRepository.create(user);
        
        // Генерируем JWT access токен
        String accessToken = jwtUtil.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
        
        // Генерируем refresh токен
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        log.info("Зарегистрирован новый пользователь: {}", user.getEmail());
        
        return new AuthResponse(
            accessToken,
            refreshToken.getToken(),
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
    
    /**
     * Вход в систему
     * Использует AuthenticationManager для проверки credentials
     */
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        
        // Аутентификация через Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );
        
        // Загружаем пользователя из базы
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        // Генерируем JWT access токен
        String accessToken = jwtUtil.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
        
        // Генерируем refresh токен
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        log.info("Пользователь вошел в систему: {}", user.getEmail());
        
        return new AuthResponse(
            accessToken,
            refreshToken.getToken(),
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
    
    /**
     * Получить информацию о текущем пользователе по email из SecurityContext
     */
    public User getCurrentUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
    
    /**
     * Получить информацию о текущем пользователе по токену (legacy метод)
     */
    public User getCurrentUser(String token) {
        if (token == null) {
            // Получаем из SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                return getCurrentUserByEmail(email);
            }
            throw new RuntimeException("Пользователь не аутентифицирован");
        }
        
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Невалидный токен");
        }
        
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
    
    /**
     * Обновить access токен используя refresh токен
     */
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        // Проверяем refresh токен
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);
        
        // Загружаем пользователя
        User user = userRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        
        // Генерируем новый access токен
        String newAccessToken = jwtUtil.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
        
        log.info("Access токен обновлен для пользователя: {}", user.getEmail());
        
        return new AuthResponse(
            newAccessToken,
            refreshTokenValue, // Возвращаем тот же refresh токен
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
    
    /**
     * Выход из системы (отзыв refresh токена)
     */
    public void logout(String refreshTokenValue) {
        try {
            refreshTokenService.revokeToken(refreshTokenValue);
            log.info("Пользователь вышел из системы");
        } catch (Exception e) {
            log.warn("Ошибка при logout: {}", e.getMessage());
            // Не бросаем исключение, чтобы logout всегда проходил успешно
        }
    }
    
    /**
     * Выход со всех устройств (отзыв всех refresh токенов пользователя)
     */
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Пользователь {} вышел со всех устройств", userId);
    }
}
