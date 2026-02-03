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
            // Tarantool возвращает результат sequence напрямую как Integer в первом элементе списка
            List<?> seqResult = tarantoolClient.call("box.sequence.users_seq:next").get();
            Long newId = ((Number) seqResult.get(0)).longValue();
            user.setId(newId);
            user.setCreatedAt(LocalDateTime.now());
            
            // Вставляем в базу
            // call принимает список аргументов, insert ожидает один аргумент - tuple (массив)
            // Поэтому оборачиваем tuple в список аргументов: [tuple]
            List<?> result = tarantoolClient.call(
                "box.space.users:insert",
                List.of(List.of(
                    user.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getName(),
                    user.getRole().name(),
                    user.getCreatedAt().format(FORMATTER)
                ))
            ).get();
            
            log.info("Создан пользователь с ID: {}, email: {}", user.getId(), user.getEmail());
            return user;
            
        } catch (Exception e) {
            log.error("Ошибка создания пользователя: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать пользователя: " + e.getMessage(), e);
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
            
            // Tarantool возвращает [[tuple]] или [[]] если не найдено
            if (result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            // Проверяем что tuple не пустой
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
            
            // Tarantool возвращает [[tuple]] или [[]] если не найдено
            if (result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            // Проверяем что tuple не пустой
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
     * Маппинг tuple из Tarantool в объект User
     */
    private User mapToUser(List<?> tuple) {
        User user = new User();
        user.setId(((Number) tuple.get(0)).longValue());
        user.setEmail((String) tuple.get(1));
        user.setPasswordHash((String) tuple.get(2));
        user.setName((String) tuple.get(3));
        user.setRole(UserRole.valueOf((String) tuple.get(4)));
        user.setCreatedAt(LocalDateTime.parse((String) tuple.get(5), FORMATTER));
        return user;
    }
}
