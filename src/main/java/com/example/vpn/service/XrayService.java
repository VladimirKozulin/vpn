package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.example.vpn.model.VpnClient;
import com.example.vpn.model.XrayConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления процессом Xray
 * Отвечает за запуск, остановку и мониторинг VPN сервера
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XrayService {
    
    private final VpnProperties vpnProperties;
    private final VpnClientService vpnClientService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Хранит запущенный процесс Xray
    private Process xrayProcess;
    
    /**
     * Запускает Xray процесс с конфигурацией
     */
    public void startXray() throws IOException {
        // Проверяем, не запущен ли уже процесс
        if (isRunning()) {
            log.warn("Xray уже запущен, пропускаем старт");
            return;
        }
        
        // Запускаем процесс Xray
        log.info("Запуск Xray процесса...");
        ProcessBuilder processBuilder = new ProcessBuilder(
            vpnProperties.getXrayPath(),  // Путь к бинарнику xray
            "run",                         // Команда запуска
            "-c", vpnProperties.getConfigPath()  // Указываем файл конфигурации
        );
        
        // Перенаправляем вывод процесса в логи Java приложения
        processBuilder.redirectErrorStream(true);
        
        xrayProcess = processBuilder.start();
        
        // Запускаем поток для чтения логов Xray
        startLogReader();
        
        log.info("Xray успешно запущен на порту {}", vpnProperties.getXrayPort());
    }
    
    /**
     * Останавливает процесс Xray
     */
    public void stopXray() {
        if (xrayProcess != null && xrayProcess.isAlive()) {
            log.info("Остановка Xray процесса...");
            xrayProcess.destroy(); // Мягкая остановка
            
            try {
                // Ждем 5 секунд для корректного завершения
                if (!xrayProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Процесс не завершился, принудительное завершение");
                    xrayProcess.destroyForcibly(); // Жесткая остановка
                }
                log.info("Xray успешно остановлен");
            } catch (InterruptedException e) {
                log.error("Ошибка при остановке Xray", e);
                Thread.currentThread().interrupt();
            }
        } else {
            log.warn("Xray не запущен");
        }
    }
    
    /**
     * Перезапускает Xray (остановка + запуск)
     */
    public void restartXray() throws IOException {
        log.info("Перезапуск Xray...");
        stopXray();
        
        try {
            Thread.sleep(1000); // Пауза между остановкой и запуском
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        startXray();
    }
    
    /**
     * Проверяет, запущен ли процесс Xray
     */
    public boolean isRunning() {
        return xrayProcess != null && xrayProcess.isAlive();
    }
    
    /**
     * Генерирует конфигурационный файл для Xray в формате JSON
     * Загружает всех активных клиентов из конфига
     * Поддерживает Reality протокол для обхода DPI
     * Включает gRPC API для управления без перезапуска
     */
    public void generateConfigFile() throws IOException {
        XrayConfig config = new XrayConfig();
        
        // === Логирование ===
        XrayConfig.Log logConfig = new XrayConfig.Log();
        logConfig.setLoglevel("info");
        config.setLog(logConfig);
        
        // === gRPC API ===
        XrayConfig.Api api = new XrayConfig.Api();
        api.setTag("api");
        api.setListen(vpnProperties.getApiServer());
        api.setServices(List.of("HandlerService", "StatsService", "LoggerService"));
        config.setApi(api);
        
        // === Статистика ===
        config.setStats(new XrayConfig.Stats());
        
        // === Политики ===
        XrayConfig.Policy policy = new XrayConfig.Policy();
        
        // Создаём Map для уровней политик
        java.util.Map<String, XrayConfig.PolicyLevel> levels = new java.util.HashMap<>();
        levels.put("0", new XrayConfig.PolicyLevel());
        policy.setLevels(levels);
        
        XrayConfig.PolicySystem policySystem = new XrayConfig.PolicySystem();
        policySystem.setStatsInboundUplink(true);
        policySystem.setStatsInboundDownlink(true);
        policySystem.setStatsOutboundUplink(true);
        policySystem.setStatsOutboundDownlink(true);
        policy.setSystem(policySystem);
        config.setPolicy(policy);
        
        // === Настройка входящего подключения (inbound) для VPN ===
        XrayConfig.Inbound vpnInbound = new XrayConfig.Inbound();
        vpnInbound.setTag(vpnProperties.getInboundTag());
        vpnInbound.setPort(vpnProperties.getXrayPort());
        vpnInbound.setProtocol("vless");
        
        // Загружаем всех активных клиентов
        List<VpnClient> activeClients;
        try {
            activeClients = vpnClientService.getActiveClients();
            log.info("Найдено активных клиентов: {}", activeClients.size());
        } catch (Exception e) {
            // При первом запуске таблица может не существовать
            log.warn("Не удалось загрузить клиентов из БД (возможно первый запуск): {}", e.getMessage());
            activeClients = new ArrayList<>();
        }
        
        // Конвертируем в Xray клиентов
        List<XrayConfig.Client> xrayClients = activeClients.stream()
            .map(vpnClient -> {
                XrayConfig.Client client = new XrayConfig.Client();
                client.setId(vpnClient.getUuid());
                client.setEmail(vpnClient.getDeviceInfo() != null ? 
                    vpnClient.getDeviceInfo() : "client");
                // Для Reality с XTLS Vision
                if (vpnProperties.getReality().isEnabled()) {
                    client.setFlow("xtls-rprx-vision");
                }
                return client;
            })
            .collect(Collectors.toList());
        
        XrayConfig.InboundSettings vpnInboundSettings = new XrayConfig.InboundSettings();
        vpnInboundSettings.setClients(xrayClients);
        vpnInbound.setSettings(vpnInboundSettings);
        
        // Настройка транспорта с Reality
        XrayConfig.StreamSettings streamSettings = new XrayConfig.StreamSettings();
        streamSettings.setNetwork("tcp");
        
        if (vpnProperties.getReality().isEnabled()) {
            log.info("🔐 Reality протокол включен");
            streamSettings.setSecurity("reality");
            
            XrayConfig.RealitySettings realitySettings = new XrayConfig.RealitySettings();
            realitySettings.setShow(false);
            realitySettings.setDest(vpnProperties.getReality().getDest());
            realitySettings.setServerNames(vpnProperties.getReality().getServerNames());
            realitySettings.setPrivateKey(vpnProperties.getReality().getPrivateKey());
            realitySettings.setShortIds(vpnProperties.getReality().getShortIds());
            realitySettings.setFingerprint(vpnProperties.getReality().getFingerprint());
            
            streamSettings.setRealitySettings(realitySettings);
            
            log.info("Reality dest: {}", realitySettings.getDest());
            log.info("Reality serverNames: {}", realitySettings.getServerNames());
            log.info("Reality shortIds: {}", realitySettings.getShortIds());
        } else {
            log.warn("⚠️ Reality отключен - соединение НЕ защищено от DPI!");
            streamSettings.setSecurity("none");
        }
        
        vpnInbound.setStreamSettings(streamSettings);
        
        config.setInbounds(List.of(vpnInbound));
        
        // === Настройка исходящего подключения (outbound) - прямой доступ в интернет ===
        XrayConfig.Outbound outbound = new XrayConfig.Outbound();
        outbound.setProtocol("freedom"); // "freedom" = прямое подключение без прокси
        outbound.setTag("direct");
        
        config.setOutbounds(List.of(outbound));
        
        // Сохраняем конфиг в файл
        String jsonConfig = gson.toJson(config);
        Files.writeString(Path.of(vpnProperties.getConfigPath()), jsonConfig);
        
        log.info("✅ Конфигурационный файл создан с {} клиентами", xrayClients.size());
        log.info("🔧 gRPC API включен на {}", vpnProperties.getApiServer());
        log.debug("Содержимое конфига:\n{}", jsonConfig);
    }
    
    /**
     * Читает логи из процесса Xray и выводит их в наши логи
     */
    private void startLogReader() {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(xrayProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Xray] {}", line);
                }
            } catch (IOException e) {
                log.error("Ошибка чтения логов Xray", e);
            }
        }).start();
    }
}
