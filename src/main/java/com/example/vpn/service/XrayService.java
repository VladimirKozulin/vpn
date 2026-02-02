package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.example.vpn.model.XrayConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для управления процессом Xray
 * Отвечает за запуск, остановку и мониторинг VPN сервера
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XrayService {
    
    private final VpnProperties vpnProperties;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Хранит запущенный процесс Xray
    private Process xrayProcess;
    
    // Путь к файлу конфигурации
    private static final String CONFIG_FILE = "xray-config.json";
    
    /**
     * Запускает Xray процесс с сгенерированной конфигурацией
     */
    public void startXray() throws IOException {
        // Проверяем, не запущен ли уже процесс
        if (isRunning()) {
            log.warn("Xray уже запущен, пропускаем старт");
            return;
        }
        
        // Генерируем конфигурационный файл
        generateConfigFile();
        
        // Запускаем процесс Xray
        log.info("Запуск Xray процесса...");
        ProcessBuilder processBuilder = new ProcessBuilder(
            vpnProperties.getXrayPath(),  // Путь к бинарнику xray
            "run",                         // Команда запуска
            "-c", CONFIG_FILE              // Указываем файл конфигурации
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
     * Проверяет, запущен ли процесс Xray
     */
    public boolean isRunning() {
        return xrayProcess != null && xrayProcess.isAlive();
    }
    
    /**
     * Генерирует конфигурационный файл для Xray в формате JSON
     */
    private void generateConfigFile() throws IOException {
        XrayConfig config = new XrayConfig();
        
        // Настройка входящего подключения (inbound)
        XrayConfig.Inbound inbound = new XrayConfig.Inbound();
        inbound.setPort(vpnProperties.getXrayPort());
        inbound.setProtocol("vless");
        
        // Настройка клиента
        XrayConfig.Client client = new XrayConfig.Client();
        client.setId(vpnProperties.getClientUuid());
        client.setEmail("user@example.com");
        
        XrayConfig.InboundSettings inboundSettings = new XrayConfig.InboundSettings();
        inboundSettings.setClients(List.of(client));
        inbound.setSettings(inboundSettings);
        
        // Настройка транспорта (пока без TLS для простоты)
        XrayConfig.StreamSettings streamSettings = new XrayConfig.StreamSettings();
        inbound.setStreamSettings(streamSettings);
        
        config.setInbounds(List.of(inbound));
        
        // Настройка исходящего подключения (outbound) - прямой доступ в интернет
        XrayConfig.Outbound outbound = new XrayConfig.Outbound();
        outbound.setProtocol("freedom"); // "freedom" = прямое подключение без прокси
        outbound.setTag("direct");
        
        config.setOutbounds(List.of(outbound));
        
        // Сохраняем конфиг в файл
        String jsonConfig = gson.toJson(config);
        Files.writeString(Path.of(CONFIG_FILE), jsonConfig);
        
        log.info("Конфигурационный файл создан: {}", CONFIG_FILE);
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
