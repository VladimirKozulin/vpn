package com.example.vpn.security;

import com.example.vpn.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT фильтр для аутентификации запросов
 * Извлекает JWT токен из заголовка Authorization и устанавливает аутентификацию
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.debug("=== JWT Filter: обработка запроса {} ===", requestURI);
        
        try {
            // Извлекаем токен из заголовка Authorization
            String authHeader = request.getHeader("Authorization");
            log.debug("Authorization header: {}", authHeader != null ? "присутствует" : "отсутствует");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.debug("Токен извлечён, длина: {}", token.length());
                
                // Валидируем токен
                if (jwtUtil.validateToken(token)) {
                    log.debug("Токен валидный");
                    
                    // Извлекаем данные из токена
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    Long userId = jwtUtil.extractUserId(token);
                    
                    log.debug("Данные из токена: email={}, role={}, userId={}", email, role, userId);
                    
                    // Создаём аутентификацию
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                    
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Устанавливаем аутентификацию в SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT аутентификация установлена для пользователя: {}", email);
                } else {
                    log.warn("Невалидный JWT токен для запроса: {}", requestURI);
                }
            } else {
                log.debug("Токен отсутствует, запрос без аутентификации");
            }
        } catch (Exception e) {
            log.error("Ошибка обработки JWT токена для {}: {}", requestURI, e.getMessage(), e);
            // Не блокируем запрос, просто логируем ошибку
        }
        
        log.debug("=== JWT Filter: передача запроса дальше по цепочке ===");
        // Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }
}
