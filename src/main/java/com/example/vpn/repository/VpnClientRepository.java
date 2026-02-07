package com.example.vpn.repository;

import com.example.vpn.model.VpnClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository для работы с VPN клиентами в БД
 */
@Repository
public interface VpnClientRepository extends JpaRepository<VpnClient, Long> {
    
    /**
     * Найти клиента по UUID для Xray
     */
    Optional<VpnClient> findByUuid(String uuid);
    
    /**
     * Найти клиента по Keycloak User ID
     */
    Optional<VpnClient> findByKeycloakUserId(UUID keycloakUserId);
    
    /**
     * Найти клиента по email
     */
    Optional<VpnClient> findByEmail(String email);
    
    /**
     * Проверить существует ли клиент с таким UUID
     */
    boolean existsByUuid(String uuid);
    
    /**
     * Проверить существует ли клиент с таким Keycloak User ID
     */
    boolean existsByKeycloakUserId(UUID keycloakUserId);
}
