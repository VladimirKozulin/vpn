package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.google.protobuf.ByteString;
import com.xray.app.proxyman.command.*;
import com.xray.app.stats.command.*;
import com.xray.common.protocol.User;
import com.xray.common.serial.TypedMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * gRPC клиент для взаимодействия с Xray API
 * Работает БЕЗ перезапуска Xray!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XrayGrpcClient {
    
    private final VpnProperties vpnProperties;
    private ManagedChannel channel;
    private HandlerServiceGrpc.HandlerServiceBlockingStub handlerStub;
    private StatsServiceGrpc.StatsServiceBlockingStub statsStub;
    
    @PostConstruct
    public void init() {
        String[] parts = vpnProperties.getApiServer().split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build();
        
        handlerStub = HandlerServiceGrpc.newBlockingStub(channel);
        statsStub = StatsServiceGrpc.newBlockingStub(channel);
        
        log.info("✅ gRPC клиент инициализирован: {}:{}", host, port);
    }
    
    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("✅ gRPC клиент остановлен");
            } catch (InterruptedException e) {
                log.warn("⚠️ Ошибка при остановке gRPC клиента", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Добавить пользователя через gRPC (БЕЗ перезапуска!)
     */
    public void addUser(String uuid, String email) {
        try {
            log.info("🔧 Добавление пользователя через gRPC: UUID={}, email={}", uuid, email);
            
            // Создаём VLESS Account с UUID
            com.xray.proxy.vless.Account vlessAccount = com.xray.proxy.vless.Account.newBuilder()
                .setId(uuid)
                .setFlow("xtls-rprx-vision")  // Для Reality
                .build();
            
            // Упаковываем Account в TypedMessage
            TypedMessage accountMsg = TypedMessage.newBuilder()
                .setType("xray.proxy.vless.Account")
                .setValue(vlessAccount.toByteString())
                .build();
            
            // Создаём пользователя с Account
            User user = User.newBuilder()
                .setEmail(uuid)  // Используем UUID как email для уникальной идентификации
                .setLevel(0)
                .setAccount(accountMsg)
                .build();
            
            // Создаём операцию добавления
            AddUserOperation addOp = AddUserOperation.newBuilder()
                .setUser(user)
                .build();
            
            // Упаковываем операцию в TypedMessage
            TypedMessage operation = TypedMessage.newBuilder()
                .setType("xray.app.proxyman.command.AddUserOperation")
                .setValue(addOp.toByteString())
                .build();
            
            // Отправляем запрос
            AlterInboundRequest request = AlterInboundRequest.newBuilder()
                .setTag(vpnProperties.getInboundTag())
                .setOperation(operation)
                .build();
            
            handlerStub.alterInbound(request);
            
            log.info("✅ Пользователь добавлен через gRPC: {}", uuid);
            
        } catch (Exception e) {
            log.error("❌ Ошибка добавления пользователя через gRPC", e);
            throw new RuntimeException("Не удалось добавить пользователя", e);
        }
    }
    
    /**
     * Удалить пользователя через gRPC (БЕЗ перезапуска!)
     */
    public void removeUser(String uuid) {
        try {
            log.info("🔧 Удаление пользователя через gRPC: UUID={}", uuid);
            
            // Создаём операцию удаления (используем UUID как email)
            RemoveUserOperation removeOp = RemoveUserOperation.newBuilder()
                .setEmail(uuid)  // UUID используется как email
                .build();
            
            // Упаковываем в TypedMessage
            TypedMessage operation = TypedMessage.newBuilder()
                .setType("xray.app.proxyman.command.RemoveUserOperation")
                .setValue(removeOp.toByteString())
                .build();
            
            // Отправляем запрос
            AlterInboundRequest request = AlterInboundRequest.newBuilder()
                .setTag(vpnProperties.getInboundTag())
                .setOperation(operation)
                .build();
            
            handlerStub.alterInbound(request);
            
            log.info("✅ Пользователь удалён через gRPC: {}", uuid);
            
        } catch (Exception e) {
            log.error("❌ Ошибка удаления пользователя через gRPC", e);
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }
    
    /**
     * Получить статистику пользователя через gRPC
     */
    public UserStats getUserStats(String uuid) {
        try {
            log.debug("📊 Запрос статистики через gRPC для UUID: {}", uuid);
            
            QueryStatsRequest request = QueryStatsRequest.newBuilder()
                .setPattern("user>>>" + uuid + ">>>")
                .setReset(false)
                .build();
            
            QueryStatsResponse response = statsStub.queryStats(request);
            
            long uplink = 0;
            long downlink = 0;
            
            for (Stat stat : response.getStatList()) {
                if (stat.getName().contains("uplink")) {
                    uplink = stat.getValue();
                } else if (stat.getName().contains("downlink")) {
                    downlink = stat.getValue();
                }
            }
            
            log.debug("📊 Статистика {}: uplink={}, downlink={}", uuid, uplink, downlink);
            return new UserStats(uplink, downlink);
            
        } catch (Exception e) {
            log.error("❌ Ошибка получения статистики через gRPC", e);
            return new UserStats(0, 0);
        }
    }
    
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
