package com.example.vpn.security;

import com.example.vpn.model.User;
import com.example.vpn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для загрузки пользователя из базы данных
 * Интегрируется с Spring Security для аутентификации
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Загрузить пользователя по email (username в Spring Security)
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя по email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("Пользователь не найден: {}", email);
                return new UsernameNotFoundException("Пользователь не найден: " + email);
            });
        
        log.debug("Пользователь найден: {}, роль: {}", user.getEmail(), user.getRole());
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
    
    /**
     * Загрузить пользователя по ID
     */
    public User loadUserById(Long userId) {
        log.debug("Загрузка пользователя по ID: {}", userId);
        
        return userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("Пользователь не найден по ID: {}", userId);
                return new UsernameNotFoundException("Пользователь не найден с ID: " + userId);
            });
    }
}
