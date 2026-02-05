package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.example.vpn.model.VpnClient;
import com.example.vpn.model.XrayConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для управления VPN клиентами
 * Работает напрямую с конфигурационным файлом Xray
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VpnClientService {
    
    private final VpnProperties vpnProperties;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Создать нового VPN клиента
     * Автоматически генерирует UUID и добавляет в конфиг
     */
    public VpnClient createClient(VpnClient client) {
        log.info("Создание нового VPN клиента");
        
        // Генерируем UUID если его нет
        if (client.getUuid() == null) {
            client.setUuid(UUID.randomUUID().toString());
        }
        
        // Читаем текущий конфиг
        XrayConfig config = readConfig();
        
        // Добавляем нового клиента
        XrayConfig.Client xrayClient = new XrayConfig.Client();
        xrayClient.setId(client.getUuid());
        xrayClient.setEmail(client.getDeviceInfo() != null ? client.getDeviceInfo() : "client");
        
        if (config.getInbounds() != null && !config.getInbounds().isEmpty()) {
            XrayConfig.Inbound inbound = config.getInbounds().get(0);
            if (inbound.getSettings() == null) {
                inbound.setSettings(new XrayConfig.InboundSettings());
            }
            if (inbound.getSettings().getClients() == null) {
                inbound.getSettings().setClients(new ArrayList<>());
            }
            inbound.getSettings().getClients().add(xrayClient);
        }
        
        // Сохраняем конфиг
        saveConfig(config);
        
        log.info("Создан VPN клиент с UUID: {}", client.getUuid());
        return client;
    }
    
    /**
     * Получить клиента по UUID
     */
    public Optional<VpnClient> getClientByUuid(String uuid) {
        XrayConfig config = readConfig();
        
        if (config.getInbounds() == null || config.getInbounds().isEmpty()) {
            return Optional.empty();
        }
        
        XrayConfig.Inbound inbound = config.getInbounds().get(0);
        if (inbound.getSettings() == null || inbound.getSettings().getClients() == null) {
            return Optional.empty();
        }
        
        return inbound.getSettings().getClients().stream()
            .filter(c -> c.getId().equals(uuid))
            .map(c -> {
                VpnClient client = new VpnClient();
                client.setUuid(c.getId());
                client.setDeviceInfo(c.getEmail());
                client.setIsActive(true);
                return client;
            })
            .findFirst();
    }
    
    /**
     * Получить всех активных клиентов
     */
    public List<VpnClient> getActiveClients() {
        XrayConfig config = readConfig();
        
        if (config.getInbounds() == null || config.getInbounds().isEmpty()) {
            return new ArrayList<>();
        }
        
        XrayConfig.Inbound inbound = config.getInbounds().get(0);
        if (inbound.getSettings() == null || inbound.getSettings().getClients() == null) {
            return new ArrayList<>();
        }
        
        return inbound.getSettings().getClients().stream()
            .map(c -> {
                VpnClient client = new VpnClient();
                client.setUuid(c.getId());
                client.setDeviceInfo(c.getEmail());
                client.setIsActive(true);
                return client;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Получить всех клиентов
     */
    public List<VpnClient> getAllClients() {
        return getActiveClients();
    }
    
    /**
     * Обновить клиента
     */
    public VpnClient updateClient(String uuid, VpnClient updatedClient) {
        log.info("Обновление клиента UUID: {}", uuid);
        
        XrayConfig config = readConfig();
        
        if (config.getInbounds() != null && !config.getInbounds().isEmpty()) {
            XrayConfig.Inbound inbound = config.getInbounds().get(0);
            if (inbound.getSettings() != null && inbound.getSettings().getClients() != null) {
                inbound.getSettings().getClients().stream()
                    .filter(c -> c.getId().equals(uuid))
                    .findFirst()
                    .ifPresent(c -> {
                        if (updatedClient.getDeviceInfo() != null) {
                            c.setEmail(updatedClient.getDeviceInfo());
                        }
                    });
            }
        }
        
        saveConfig(config);
        return updatedClient;
    }
    
    /**
     * Удалить клиента
     */
    public void deleteClient(String uuid) {
        log.info("Удаление клиента UUID: {}", uuid);
        
        XrayConfig config = readConfig();
        
        if (config.getInbounds() != null && !config.getInbounds().isEmpty()) {
            XrayConfig.Inbound inbound = config.getInbounds().get(0);
            if (inbound.getSettings() != null && inbound.getSettings().getClients() != null) {
                inbound.getSettings().getClients().removeIf(c -> c.getId().equals(uuid));
            }
        }
        
        saveConfig(config);
    }
    
    /**
     * Читает конфигурацию из файла
     */
    private XrayConfig readConfig() {
        try {
            String json = Files.readString(Path.of(vpnProperties.getConfigPath()));
            return gson.fromJson(json, XrayConfig.class);
        } catch (IOException e) {
            log.error("Ошибка чтения конфига", e);
            return createDefaultConfig();
        }
    }
    
    /**
     * Сохраняет конфигурацию в файл
     */
    private void saveConfig(XrayConfig config) {
        try {
            String json = gson.toJson(config);
            Files.writeString(Path.of(vpnProperties.getConfigPath()), json);
            log.info("Конфигурация сохранена");
        } catch (IOException e) {
            log.error("Ошибка сохранения конфига", e);
            throw new RuntimeException("Не удалось сохранить конфигурацию", e);
        }
    }
    
    /**
     * Создает конфигурацию по умолчанию
     */
    private XrayConfig createDefaultConfig() {
        XrayConfig config = new XrayConfig();
        
        XrayConfig.Inbound inbound = new XrayConfig.Inbound();
        inbound.setPort(vpnProperties.getXrayPort());
        inbound.setProtocol("vless");
        
        XrayConfig.InboundSettings settings = new XrayConfig.InboundSettings();
        settings.setClients(new ArrayList<>());
        inbound.setSettings(settings);
        
        XrayConfig.StreamSettings streamSettings = new XrayConfig.StreamSettings();
        inbound.setStreamSettings(streamSettings);
        
        config.setInbounds(List.of(inbound));
        
        XrayConfig.Outbound outbound = new XrayConfig.Outbound();
        outbound.setProtocol("freedom");
        outbound.setTag("direct");
        
        config.setOutbounds(List.of(outbound));
        
        return config;
    }
}
