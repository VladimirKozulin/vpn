package com.example.vpn.service;

import com.example.vpn.model.PendingClient;
import com.example.vpn.model.VpnClient;
import com.example.vpn.repository.VpnClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Сервис мониторинга подключений клиентов
 * Проверяет pending клиентов через 5 минут после создания
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class ConnectionMonitorService {
    
    private final PendingClientService pendingClientService;
    private final XrayGrpcClient xrayGrpcClient;
    private final VpnClientRepository vpnClientRepository;
    private final TaskScheduler taskScheduler;
    
    /**
     * Запланировать проверку клиента через 5 минут
     */
    public void scheduleCheck(String uuid) {
        PendingClient client = pendingClientService.get(uuid)
            .orElseThrow(() -> new IllegalStateException("Pending клиент не найден: " + uuid));
        
        // Планируем задачу на момент истечения
        Instant checkTime = client.getExpiresAt()
            .atZone(ZoneId.systemDefault())
            .toInstant();
        
        taskScheduler.schedule(() -> checkClient(uuid), checkTime);
        
        log.info("⏰ Запланирована проверка клиента {} на {}", uuid, client.getExpiresAt());
    }
    
    /**
     * Проверить клиента: подключился или нет
     */
    private void checkClient(String uuid) {
        log.info("🔍 Проверка клиента UUID: {}", uuid);
        
        // Проверяем что клиент всё ещё в pending
        PendingClient pendingClient = pendingClientService.get(uuid).orElse(null);
        if (pendingClient == null) {
            log.info("ℹ️ Клиент {} уже обработан или удалён", uuid);
            return;
        }
        
        try {
            // Запрашиваем статистику у Xray
            XrayGrpcClient.UserStats stats = xrayGrpcClient.getUserStats(uuid);
            
            if (stats.hasTraffic()) {
                // Клиент подключился! Сохраняем в БД
                log.info("✅ Клиент {} ПОДКЛЮЧИЛСЯ! Uplink: {}, Downlink: {}", 
                    uuid, stats.getUplink(), stats.getDownlink());
                
                VpnClient vpnClient = new VpnClient();
                vpnClient.setUuid(uuid);
                vpnClient.setDeviceInfo(pendingClient.getDeviceInfo());
                vpnClient.setIsActive(true);
                vpnClient.setFirstConnectedAt(LocalDateTime.now());
                vpnClient.setLastConnectedAt(LocalDateTime.now());
                
                vpnClientRepository.save(vpnClient);
                pendingClientService.remove(uuid);
                
                log.info("💾 Клиент {} сохранён в БД", uuid);
                
            } else {
                // Клиент НЕ подключился за 5 минут - удаляем
                log.warn("⏱️ Клиент {} НЕ подключился за 5 минут. Удаляем...", uuid);
                
                xrayGrpcClient.removeUser(uuid);
                pendingClientService.remove(uuid);
                
                log.info("🗑️ Клиент {} удалён из Xray и pending", uuid);
            }
            
        } catch (Exception e) {
            log.error("❌ Ошибка при проверке клиента {}", uuid, e);
            // В случае ошибки оставляем клиента в pending
        }
    }
}
