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
     */
    public VpnClient createClient(VpnClient client) {
        log.info("Создание нового VPN клиента");
        
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
     * Получить всех клиентов
     */
    public List<VpnClient> getAllClients() {
        return repository.findAll();
    }
    
    /**
     * Получить клиента по ID
     */
    public Optional<VpnClient> getClientById(Long id) {
        return repository.findById(id);
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
     * Переключить активность клиента (вкл/выкл)
     */
    public VpnClient toggleClient(Long id) {
        Optional<VpnClient> clientOpt = repository.findById(id);
        
        if (clientOpt.isEmpty()) {
            throw new RuntimeException("Клиент не найден");
        }
        
        VpnClient client = clientOpt.get();
        client.setIsActive(!client.getIsActive());
        
        log.info("Переключение активности клиента ID: {} -> {}", id, client.getIsActive());
        
        return updateClient(client);
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
