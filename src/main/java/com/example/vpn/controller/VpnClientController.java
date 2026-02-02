package com.example.vpn.controller;

import com.example.vpn.model.VpnClient;
import com.example.vpn.service.ConfigService;
import com.example.vpn.service.VpnClientService;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST API контроллер для управления VPN клиентами
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class VpnClientController {
    
    private final VpnClientService vpnClientService;
    private final ConfigService configService;
    
    /**
     * Создать нового VPN клиента
     * POST /api/clients
     */
    @PostMapping
    public ResponseEntity<VpnClient> createClient(@RequestBody VpnClient client) {
        try {
            log.info("Запрос на создание клиента");
            VpnClient created = vpnClientService.createClient(client);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Ошибка создания клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить всех клиентов
     * GET /api/clients
     */
    @GetMapping
    public ResponseEntity<List<VpnClient>> getAllClients() {
        List<VpnClient> clients = vpnClientService.getAllClients();
        return ResponseEntity.ok(clients);
    }
    
    /**
     * Получить клиента по ID
     * GET /api/clients/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VpnClient> getClient(@PathVariable Long id) {
        return vpnClientService.getClientById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Обновить клиента
     * PUT /api/clients/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<VpnClient> updateClient(@PathVariable Long id, @RequestBody VpnClient client) {
        try {
            client.setId(id);
            VpnClient updated = vpnClientService.updateClient(client);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Ошибка обновления клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Переключить активность клиента (вкл/выкл)
     * POST /api/clients/{id}/toggle
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<VpnClient> toggleClient(@PathVariable Long id) {
        try {
            VpnClient toggled = vpnClientService.toggleClient(id);
            return ResponseEntity.ok(toggled);
        } catch (Exception e) {
            log.error("Ошибка переключения клиента", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null);
        }
    }
    
    /**
     * Удалить клиента
     * DELETE /api/clients/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteClient(@PathVariable Long id) {
        try {
            vpnClientService.deleteClient(id);
            return ResponseEntity.ok(Map.of("message", "Клиент удален"));
        } catch (Exception e) {
            log.error("Ошибка удаления клиента", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Получить VLESS ссылку для клиента
     * GET /api/clients/{id}/config
     */
    @GetMapping("/{id}/config")
    public ResponseEntity<Map<String, String>> getClientConfig(@PathVariable Long id) {
        return vpnClientService.getClientById(id)
            .map(client -> {
                String link = configService.generateVlessLink(client);
                return ResponseEntity.ok(Map.of(
                    "link", link,
                    "instruction", "Скопируйте эту ссылку и вставьте в v2rayNG"
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Получить QR код для клиента
     * GET /api/clients/{id}/qr
     */
    @GetMapping(value = "/{id}/qr", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getClientQr(@PathVariable Long id) {
        try {
            return vpnClientService.getClientById(id)
                .map(client -> {
                    try {
                        String html = configService.generateQrPage(client);
                        return ResponseEntity.ok(html);
                    } catch (WriterException | IOException e) {
                        log.error("Ошибка генерации QR кода", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .<String>body("<h1>Ошибка генерации QR кода</h1>");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Ошибка получения QR кода", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<h1>Ошибка: " + e.getMessage() + "</h1>");
        }
    }
}
