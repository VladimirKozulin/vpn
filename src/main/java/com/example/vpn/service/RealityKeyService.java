package com.example.vpn.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Сервис для генерации Reality ключей
 * Использует xray x25519 для генерации пары ключей
 */
@Slf4j
@Service
public class RealityKeyService {
    
    /**
     * Генерирует пару ключей x25519 для Reality
     * Выполняет команду: xray x25519
     */
    public RealityKeys generateKeys(String xrayPath) throws IOException, InterruptedException {
        log.info("Генерация Reality ключей...");
        
        ProcessBuilder processBuilder = new ProcessBuilder(xrayPath, "x25519");
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("[xray x25519] {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Ошибка генерации ключей, код выхода: " + exitCode);
        }
        
        // Парсим вывод
        // Новый формат xray x25519:
        // PrivateKey: xxx (приватный ключ для сервера)
        // Password: yyy (публичный ключ для клиентов)
        String outputStr = output.toString();
        String privateKey = extractKey(outputStr, "PrivateKey");
        String publicKey = extractKey(outputStr, "Password");
        
        if (privateKey == null || publicKey == null) {
            log.error("Вывод xray x25519:\n{}", outputStr);
            throw new IOException("Не удалось распарсить ключи из вывода xray. " +
                "Ожидается формат: PrivateKey: xxx и Password: yyy");
        }
        
        RealityKeys keys = new RealityKeys();
        keys.setPrivateKey(privateKey);
        keys.setPublicKey(publicKey);
        
        log.info("✅ Reality ключи успешно сгенерированы");
        log.info("Private key: {}...", privateKey.substring(0, Math.min(20, privateKey.length())));
        log.info("Public key: {}...", publicKey.substring(0, Math.min(20, publicKey.length())));
        
        return keys;
    }
    
    /**
     * Извлекает ключ из вывода команды xray x25519
     */
    private String extractKey(String output, String prefix) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains(prefix)) {
                // Формат: "PrivateKey: значение" или "Password: значение"
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    return parts[1].trim();
                }
            }
        }
        return null;
    }
    
    /**
     * Модель для хранения пары ключей Reality
     */
    @Data
    public static class RealityKeys {
        private String privateKey;  // Приватный ключ (для сервера)
        private String publicKey;   // Публичный ключ (для клиентов)
    }
}
