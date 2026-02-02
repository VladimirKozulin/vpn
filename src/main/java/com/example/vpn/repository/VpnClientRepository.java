package com.example.vpn.repository;

import com.example.vpn.model.VpnClient;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Репозиторий для работы с VPN клиентами в Tarantool
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VpnClientRepository {
    
    private final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * Создать нового клиента
     */
    public VpnClient save(VpnClient client) {
        try {
            // Генерируем UUID если его нет
            if (client.getUuid() == null) {
                client.setUuid(UUID.randomUUID().toString());
            }
            
            // Устанавливаем дату создания
            if (client.getCreatedAt() == null) {
                client.setCreatedAt(LocalDateTime.now());
            }
            
            String luaScript = """
                local seq = box.sequence.vpn_clients_seq:next()
                local tuple = box.space.vpn_clients:insert({
                    seq,
                    ...
                })
                return tuple
                """;
            
            List<Object> params = Arrays.asList(
                client.getUuid(),
                client.getDeviceInfo(),
                client.getIpAddress(),
                client.getCountry(),
                client.getIsActive(),
                client.getTrafficLimitGb(),
                client.getTrafficUsedGb(),
                client.getExpiresAt() != null ? client.getExpiresAt().format(FORMATTER) : null,
                client.getLastConnectedAt() != null ? client.getLastConnectedAt().format(FORMATTER) : null,
                client.getCreatedAt().format(FORMATTER)
            );
            
            List<?> result = tarantoolClient.eval(luaScript, params).get();
            
            // Парсим результат
            if (result != null && !result.isEmpty()) {
                List<?> tuple = (List<?>) result.getFirst();
                client.setId(((Number) tuple.getFirst()).longValue());
            }
            
            log.info("Создан VPN клиент с UUID: {}", client.getUuid());
            return client;
        } catch (Exception e) {
            log.error("Ошибка создания клиента", e);
            throw new RuntimeException("Не удалось создать клиента", e);
        }
    }
    
    /**
     * Найти всех клиентов
     */
    public List<VpnClient> findAll() {
        try {
            String luaScript = "return box.space.vpn_clients:select()";
            List<?> result = tarantoolClient.eval(luaScript).get();
            
            if (result == null || result.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<?> tuples = (List<?>) result.get(0);
            return tuples.stream()
                .map(this::tupleToClient)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка получения списка клиентов", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Найти клиента по ID
     */
    public Optional<VpnClient> findById(Long id) {
        try {
            String luaScript = "return box.space.vpn_clients:get(...)";
            List<?> result = tarantoolClient.eval(luaScript, Collections.singletonList(id)).get();
            
            if (result == null || result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            if (tuple == null) {
                return Optional.empty();
            }
            
            return Optional.of(tupleToClient(tuple));
        } catch (Exception e) {
            log.error("Ошибка поиска клиента по ID", e);
            return Optional.empty();
        }
    }
    
    /**
     * Найти клиента по UUID
     */
    public Optional<VpnClient> findByUuid(String uuid) {
        try {
            String luaScript = "return box.space.vpn_clients.index.uuid:get(...)";
            List<?> result = tarantoolClient.eval(luaScript, Collections.singletonList(uuid)).get();
            
            if (result == null || result.isEmpty()) {
                return Optional.empty();
            }
            
            List<?> tuple = (List<?>) result.get(0);
            if (tuple == null) {
                return Optional.empty();
            }
            
            return Optional.of(tupleToClient(tuple));
        } catch (Exception e) {
            log.error("Ошибка поиска клиента по UUID", e);
            return Optional.empty();
        }
    }
    
    /**
     * Найти всех активных клиентов
     */
    public List<VpnClient> findAllActive() {
        try {
            String luaScript = """
                local result = {}
                for _, tuple in box.space.vpn_clients:pairs() do
                    if tuple[6] == true then
                        table.insert(result, tuple)
                    end
                end
                return result
                """;
            
            List<?> result = tarantoolClient.eval(luaScript).get();
            
            if (result == null || result.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<?> tuples = (List<?>) result.get(0);
            return tuples.stream()
                .map(this::tupleToClient)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка получения активных клиентов", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Обновить клиента
     */
    public VpnClient update(VpnClient client) {
        try {
            String luaScript = """
                return box.space.vpn_clients:update(..., {
                    {'=', 3, ...},
                    {'=', 4, ...},
                    {'=', 5, ...},
                    {'=', 6, ...},
                    {'=', 7, ...},
                    {'=', 8, ...},
                    {'=', 9, ...},
                    {'=', 10, ...}
                })
                """;
            
            List<Object> params = Arrays.asList(
                client.getId(),
                client.getDeviceInfo(),
                client.getIpAddress(),
                client.getCountry(),
                client.getIsActive(),
                client.getTrafficLimitGb(),
                client.getTrafficUsedGb(),
                client.getExpiresAt() != null ? client.getExpiresAt().format(FORMATTER) : null,
                client.getLastConnectedAt() != null ? client.getLastConnectedAt().format(FORMATTER) : null
            );
            
            tarantoolClient.eval(luaScript, params).get();
            
            log.info("Обновлен клиент ID: {}", client.getId());
            return client;
        } catch (Exception e) {
            log.error("Ошибка обновления клиента", e);
            throw new RuntimeException("Не удалось обновить клиента", e);
        }
    }
    
    /**
     * Удалить клиента
     */
    public void deleteById(Long id) {
        try {
            String luaScript = "box.space.vpn_clients:delete(...)";
            tarantoolClient.eval(luaScript, Collections.singletonList(id)).get();
            log.info("Удален клиент с ID: {}", id);
        } catch (Exception e) {
            log.error("Ошибка удаления клиента", e);
            throw new RuntimeException("Не удалось удалить клиента", e);
        }
    }
    
    /**
     * Конвертирует tuple из Tarantool в VpnClient объект
     */
    private VpnClient tupleToClient(Object tupleObj) {
        List<?> tuple = (List<?>) tupleObj;
        
        VpnClient client = new VpnClient();
        client.setId(((Number) tuple.get(0)).longValue());
        client.setUuid((String) tuple.get(1));
        client.setDeviceInfo((String) tuple.get(2));
        client.setIpAddress((String) tuple.get(3));
        client.setCountry((String) tuple.get(4));
        client.setIsActive((Boolean) tuple.get(5));
        client.setTrafficLimitGb(((Number) tuple.get(6)).intValue());
        client.setTrafficUsedGb(((Number) tuple.get(7)).doubleValue());
        
        String expiresAt = (String) tuple.get(8);
        if (expiresAt != null) {
            client.setExpiresAt(LocalDateTime.parse(expiresAt, FORMATTER));
        }
        
        String lastConnectedAt = (String) tuple.get(9);
        if (lastConnectedAt != null) {
            client.setLastConnectedAt(LocalDateTime.parse(lastConnectedAt, FORMATTER));
        }
        
        client.setCreatedAt(LocalDateTime.parse((String) tuple.get(10), FORMATTER));
        
        return client;
    }
}
