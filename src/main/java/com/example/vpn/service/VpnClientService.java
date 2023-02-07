package com.example.vpn.service;

import com.example.vpn.model.VpnClient;
import com.example.vpn.repository.VpnClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления VPN клиентами
 * Поддерживает как гостевых клиентов (userId = null), так и привязанных к пользователям
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnClientService {
    
    private final VpnClientRepository repository;
    
    @Lazy
    private final XrayService xrayService;
    
    /**
     * Создать нового VPN клиента
     * Автоматически генерирует UUID и перезапускает Xray
     * @param client - клиент (userId может быть null для гостей)
     */
    public VpnClient createClient(VpnClient client) {
        log.info("Создание нового VPN клиента, userId: {}", client.getUserId());
        
        // Сохраняем в БД (UUID генерируется автоматически)
        VpnClient saved = repository.save(client);
        
        // Перезапускаем Xray чтобы применить новый конфиг
        try {
            xrayService.restartXray();
            log.info("Xray перезапущен с новым клиентом");
        } catch (Exception e) {
            log.error("Ошибка перезапуска Xray", e);
            // Не откатываем создание клиента, можно перезапустить вручную
        }
        
        return saved;
    }
    
    /**
     * Получить клиента по ID
     */
    public Optional<VpnClient> getClientById(Long id) {
        return repository.findById(id);
    }
    
    /**
     * Получить клиента по UUID
     */
    public Optional<VpnClient> getClientByUuid(String uuid) {
        return repository.findByUuid(uuid);
    }
    
    /**
     * Получить всех активных клиентов
     */
    public List<VpnClient> getActiveClients() {
        return repository.findAllActive();
    }
    
    /**
     * Обновить клиента
     */
    public VpnClient updateClient(VpnClient client) {
        log.info("Обновление клиента ID: {}", client.getId());
        
        VpnClient updated = repository.update(client);
        
        // Перезапускаем Xray если изменился статус активности
        try {
            xrayService.restartXray();
        } catch (Exception e) {
            log.error("Ошибка перезапуска Xray", e);
        }
        
        return updated;
    }
    
    /**
     * Привязать гостевого клиента к пользователю
     * @param clientUuid - UUID гостевого клиента
     * @param userId - ID пользователя
     */
    public VpnClient claimClient(String clientUuid, Long userId) {
        // Находим клиента по UUID
        VpnClient client = repository.findByUuid(clientUuid)
            .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        
        // Проверяем что клиент гостевой (userId == null)
        if (client.getUserId() != null) {
            throw new RuntimeException("Клиент уже привязан к пользователю");
        }
        
        // Привязываем к пользователю
        client.setUserId(userId);
        VpnClient updated = repository.update(client);
        
        log.info("Клиент UUID: {} привязан к пользователю ID: {}", clientUuid, userId);
        
        return updated;
    }
    
    /**
     * Проверить принадлежит ли клиент пользователю
     */
    public boolean isClientOwnedByUser(Long clientId, Long userId) {
        Optional<VpnClient> clientOpt = repository.findById(clientId);
        
        if (clientOpt.isEmpty()) {
            return false;
        }
        
        VpnClient client = clientOpt.get();
        return client.getUserId() != null && client.getUserId().equals(userId);
    }
    
    /**
     * Удалить клиента
     */
    public void deleteClient(Long id) {
        log.info("Удаление клиента ID: {}", id);
        
        repository.deleteById(id);
        
        // Перезапускаем Xray чтобы убрать клиента из конфига
        try {
            xrayService.restartXray();
        } catch (Exception e) {
            log.error("Ошибка перезапуска Xray", e);
        }
    }
    
    /**
     * Обновить время последнего подключения
     */
    public void updateLastConnected(String uuid) {
        Optional<VpnClient> clientOpt = repository.findByUuid(uuid);
        
        if (clientOpt.isPresent()) {
            VpnClient client = clientOpt.get();
            client.setLastConnectedAt(LocalDateTime.now());
            repository.update(client);
            log.info("Обновлено время подключения для UUID: {}", uuid);
        }
    }
}
