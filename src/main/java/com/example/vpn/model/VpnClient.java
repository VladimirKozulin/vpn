package com.example.vpn.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * VPN клиент - конечный пользователь VPN сервиса
 * Сохраняется в БД только после реального подключения
 */
@Data
@Entity
@Table(name = "vpn_clients")
@Comment("VPN клиенты, которые успешно подключились к серверу")
public class VpnClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Уникальный идентификатор записи")
    private Long id;
    
    @Column(unique = true, nullable = false, length = 36)
    @Comment("Уникальный UUID для Xray")
    private String uuid;
    
    @Column(length = 255)
    @Comment("Информация об устройстве клиента")
    private String deviceInfo;
    
    @Column(nullable = false)
    @Comment("Активен ли клиент (можно отключить без удаления)")
    private Boolean isActive = true;
    
    @Column(nullable = false, updatable = false)
    @Comment("Дата создания записи в БД")
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    @Comment("Дата первого подключения к VPN")
    private LocalDateTime firstConnectedAt;
    
    @Comment("Дата последнего подключения к VPN")
    private LocalDateTime lastConnectedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (firstConnectedAt == null) {
            firstConnectedAt = LocalDateTime.now();
        }
    }
}
