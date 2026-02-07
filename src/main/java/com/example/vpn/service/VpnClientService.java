package com.example.vpn.service;

import com.example.vpn.model.VpnClient;
import com.example.vpn.repository.VpnClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для управления VPN клиентами в БД
 * Интегрирован с Keycloak для аутентификации
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnClientService {
    
    private final VpnClientRepository vpnClientRepository;
    
    /**
     * Получить клиента по UUID для Xray
     */
    public Optional<VpnClient> getClientByUuid(String uuid) {
        return vpnClientRepository.findByUuid(uuid);
    }
    
    /**
     * Получить клиента по Keycloak User ID
     */
    public Optional<VpnClient> getClientByKeycloakUserId(UUID keycloakUserId) {
        return vpnClientRepository.findByKeycloakUserId(keycloakUserId);
    }
    
    /**
     * Получить клиента по email
     */
    public Optional<VpnClient> getClientByEmail(String email) {
        return vpnClientRepository.findByEmail(email);
    }
    
    /**
     * Получить всех активных клиентов
     */
    public List<VpnClient> getActiveClients() {
        return vpnClientRepository.findAll().stream()
            .filter(VpnClient::getIsActive)
            .toList();
    }
    
    /**
     * Получить всех клиентов
     */
    public List<VpnClient> getAllClients() {
        return vpnClientRepository.findAll();
    }
    
    /**
     * Создать или получить клиента по данным из Keycloak
     * @param keycloakUserId UUID пользователя из Keycloak
     * @param email Email пользователя из Keycloak
     * @param xrayUuid UUID для Xray (если создаём нового)
     * @return VpnClient
     */
    @Transactional
    public VpnClient getOrCreateClient(UUID keycloakUserId, String email, String xrayUuid) {
        log.debug("🔍 Поиск клиента по Keycloak ID: {}", keycloakUserId);
        
        Optional<VpnClient> existingClient = vpnClientRepository.findByKeycloakUserId(keycloakUserId);
        
        if (existingClient.isPresent()) {
            log.info("✅ Найден существующий клиент: {}", email);
            return existingClient.get();
        }
        
        log.info("➕ Создание нового клиента: {}", email);
        VpnClient newClient = new VpnClient();
        newClient.setKeycloakUserId(keycloakUserId);
        newClient.setEmail(email);
        newClient.setUuid(xrayUuid);
        newClient.setIsActive(true);
        
        return vpnClientRepository.save(newClient);
    }
    
    /**
     * Сохранить клиента
     */
    public VpnClient saveClient(VpnClient client) {
        return vpnClientRepository.save(client);
    }
    
    /**
     * Удалить клиента
     */
    public void deleteClient(String uuid) {
        vpnClientRepository.findByUuid(uuid)
            .ifPresent(vpnClientRepository::delete);
    }
    
    /**
     * Проверить существует ли клиент по UUID
     */
    public boolean existsByUuid(String uuid) {
        return vpnClientRepository.existsByUuid(uuid);
    }
    
    /**
     * Проверить существует ли клиент по Keycloak User ID
     */
    public boolean existsByKeycloakUserId(UUID keycloakUserId) {
        return vpnClientRepository.existsByKeycloakUserId(keycloakUserId);
    }
}
