package com.example.vpn.service;

import com.example.vpn.dto.AuthResponse;
import com.example.vpn.dto.LoginRequest;
import com.example.vpn.dto.RegisterRequest;
import com.example.vpn.model.User;
import com.example.vpn.model.UserRole;
import com.example.vpn.repository.UserRepository;
import com.example.vpn.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Сервис для аутентификации и регистрации пользователей
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * Регистрация нового пользователя
     */
    public AuthResponse register(RegisterRequest request) {
        // Проверяем существует ли пользователь с таким email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        
        // Создаем нового пользователя
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(UserRole.USER);
        
        // Сохраняем в базу
        user = userRepository.create(user);
        
        // Генерируем JWT токен
        String token = jwtUtil.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
        
        log.info("Зарегистрирован новый пользователь: {}", user.getEmail());
        
        return new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
    
    /**
     * Вход в систему
     */
    public AuthResponse login(LoginRequest request) {
        // Ищем пользователя по email
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Неверный email или пароль"));
        
        // Проверяем пароль
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Неверный email или пароль");
        }
        
        // Генерируем JWT токен
        String token = jwtUtil.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
        
        log.info("Пользователь вошел в систему: {}", user.getEmail());
        
        return new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
    
    /**
     * Получить информацию о текущем пользователе по токену
     */
    public User getCurrentUser(String token) {
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
}
