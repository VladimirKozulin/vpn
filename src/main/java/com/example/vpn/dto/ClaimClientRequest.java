package com.example.vpn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO для запроса привязки гостевого клиента к аккаунту
 */
@Data
public class ClaimClientRequest {
    
    @NotBlank(message = "UUID клиента обязателен")
    private String clientUuid;
}
