package com.example.vpn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для взаимодействия с Xray через gRPC API
 * Работает БЕЗ перезапуска Xray!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XrayGrpcService {
    
    private final XrayGrpcClient grpcClient;
    
    /**
     * Добавить пользователя в Xray через gRPC (БЕЗ перезапуска!)
     */
    public void addUser(String uuid, String email) {
        log.info("🔧 Добавление пользователя: UUID={}, email={}", uuid, email);
        grpcClient.addUser(uuid, email);
    }
    
    /**
     * Удалить пользователя из Xray через gRPC (БЕЗ перезапуска!)
     */
    public void removeUser(String uuid) {
        log.info("🔧 Удаление пользователя: UUID={}", uuid);
        grpcClient.removeUser(uuid);
    }
    
    /**
     * Получить статистику пользователя (трафик) через gRPC
     */
    public UserStats getUserStats(String uuid) {
        XrayGrpcClient.UserStats stats = grpcClient.getUserStats(uuid);
        return new UserStats(stats.getUplink(), stats.getDownlink());
    }
    
    /**
     * Статистика пользователя
     */
    public static class UserStats {
        private final long uplink;
        private final long downlink;
        
        public UserStats(long uplink, long downlink) {
            this.uplink = uplink;
            this.downlink = downlink;
        }
        
        public long getUplink() {
            return uplink;
        }
        
        public long getDownlink() {
            return downlink;
        }
        
        public boolean hasTraffic() {
            return uplink > 0 || downlink > 0;
        }
        
        @Override
        public String toString() {
            return String.format("UserStats{uplink=%d, downlink=%d}", uplink, downlink);
        }
    }
}
