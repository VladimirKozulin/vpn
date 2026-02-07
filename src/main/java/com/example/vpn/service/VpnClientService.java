package com.example.vpn.service;

import com.example.vpn.model.VpnClient;
import com.example.vpn.repository.VpnClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления VPN клиентами в БД
 * Работает только с подключившимися клиентами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnClientService {
    
    private final VpnClientRepository vpnClientRepository;
    
    /**
     * Получить клиента по UUID
     */
    public Optional<VpnClient> getClientByUuid(String uuid) {
        return vpnClientRepository.findByUuid(uuid);
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
     * Проверить существует ли клиент
     */
    public boolean existsByUuid(String uuid) {
        return vpnClientRepository.existsByUuid(uuid);
    }
}
