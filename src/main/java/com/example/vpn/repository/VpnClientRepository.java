package com.example.vpn.repository;

import com.example.vpn.model.VpnClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository для работы с VPN клиентами в БД
 */
@Repository
public interface VpnClientRepository extends JpaRepository<VpnClient, Long> {
    
    /**
     * Найти клиента по UUID
     */
    Optional<VpnClient> findByUuid(String uuid);
    
    /**
     * Проверить существует ли клиент с таким UUID
     */
    boolean existsByUuid(String uuid);
}
