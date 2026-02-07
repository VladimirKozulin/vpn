package com.example.vpn.service;

import com.example.vpn.model.PendingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления временными (pending) клиентами
 * Хранит клиентов в памяти до их подключения или истечения TTL
 */
@Slf4j
@Service
public class PendingClientService {
    
    private final ConcurrentHashMap<String, PendingClient> pendingClients = new ConcurrentHashMap<>();
    
    /**
     * Добавить pending клиента
     */
    public void add(PendingClient client) {
        pendingClients.put(client.getUuid(), client);
        log.info("➕ Добавлен pending клиент UUID: {}, истекает: {}", 
            client.getUuid(), client.getExpiresAt());
    }
    
    /**
     * Удалить pending клиента
     */
    public void remove(String uuid) {
        PendingClient removed = pendingClients.remove(uuid);
        if (removed != null) {
            log.info("➖ Удалён pending клиент UUID: {}", uuid);
        }
    }
    
    /**
     * Получить pending клиента по UUID
     */
    public Optional<PendingClient> get(String uuid) {
        return Optional.ofNullable(pendingClients.get(uuid));
    }
    
    /**
     * Получить всех pending клиентов
     */
    public List<PendingClient> getAll() {
        return new ArrayList<>(pendingClients.values());
    }
    
    /**
     * Проверить существует ли pending клиент
     */
    public boolean exists(String uuid) {
        return pendingClients.containsKey(uuid);
    }
    
    /**
     * Получить количество pending клиентов
     */
    public int count() {
        return pendingClients.size();
    }
}
