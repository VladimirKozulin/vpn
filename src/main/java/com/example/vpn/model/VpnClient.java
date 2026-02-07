package com.example.vpn.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * VPN клиент - конечный пользователь VPN сервиса
 * Связан с пользователем Keycloak через keycloakUserId
 */
@Data
@Entity
@Table(name = "vpn_clients", indexes = {
    @Index(name = "idx_keycloak_user_id", columnList = "keycloak_user_id"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_uuid", columnList = "uuid")
})
@Comment("VPN клиенты с интеграцией Keycloak")
public class VpnClient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Уникальный идентификатор записи")
    private Long id;
    
    @Column(name = "keycloak_user_id", unique = true, nullable = false)
    @Comment("ID пользователя из Keycloak")
    private UUID keycloakUserId;
    
    @Column(unique = true, nullable = false, length = 255)
    @Comment("Email пользователя из Keycloak")
    private String email;
    
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
    
    @Comment("Дата первого подключения к VPN")
    private LocalDateTime firstConnectedAt;
    
    @Comment("Дата последнего подключения к VPN")
    private LocalDateTime lastConnectedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
