package com.example.vpn.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация Spring Security
 * Настраивает JWT аутентификацию и разрешения для эндпоинтов
 * Обновлено для Spring Security 7 / Spring Boot 4
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    
    /**
     * Настройка цепочки фильтров безопасности
     * Обновлено для Spring Security 7
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF так как используем JWT (современный синтаксис)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Настраиваем CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Настраиваем авторизацию запросов (authorizeHttpRequests вместо authorizeRequests)
            .authorizeHttpRequests(auth -> auth
                // Публичные эндпоинты (регистрация, вход)
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                
                // Эндпоинты для администраторов
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // VPN клиенты - только для аутентифицированных пользователей
                .requestMatchers("/api/vpn/**", "/api/clients/**").authenticated()
                
                // Информация о текущем пользователе - только для аутентифицированных
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                
                // Все остальные запросы требуют аутентификации
                .anyRequest().authenticated()
            )
            
            // Отключаем сессии (используем JWT - stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Настраиваем провайдер аутентификации
            .authenticationProvider(authenticationProvider())
            
            // Добавляем JWT фильтр перед стандартным фильтром аутентификации
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * Настройка CORS для мобильного приложения
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Разрешаем запросы с любых источников (для мобильного приложения)
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Разрешенные HTTP методы
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Разрешенные заголовки
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Разрешаем отправку credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Bean для шифрования паролей
     * Используем BCrypt алгоритм с силой 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * AuthenticationProvider для интеграции с UserDetailsService
     * Связывает UserDetailsService и PasswordEncoder
     * В Spring Security 7 конструктор требует UserDetailsService
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
    
    /**
     * AuthenticationManager для программной аутентификации
     * Используется в AuthService для login
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
