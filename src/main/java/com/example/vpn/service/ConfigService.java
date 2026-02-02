package com.example.vpn.service;

import com.example.vpn.config.VpnProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Сервис для генерации конфигурации клиента
 * Создает ссылки и QR коды для подключения к VPN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {
    
    private final VpnProperties vpnProperties;
    
    /**
     * Генерирует VLESS ссылку для импорта в клиент (v2rayNG, NekoBox и т.д.)
     * Формат: vless://UUID@SERVER:PORT?параметры#название
     */
    public String generateVlessLink() {
        // Базовая часть: vless://UUID@адрес:порт
        String base = String.format("vless://%s@%s:%d",
            vpnProperties.getClientUuid(),
            vpnProperties.getServerAddress(),
            vpnProperties.getXrayPort()
        );
        
        // Параметры подключения
        // encryption=none - VLESS не использует дополнительное шифрование
        // type=tcp - используем TCP транспорт
        // security=none - пока без TLS (для начального тестирования)
        String params = "?encryption=none&type=tcp&security=none";
        
        // Название подключения (будет отображаться в клиенте)
        String name = "#MyVPN";
        
        String vlessLink = base + params + name;
        
        log.info("Сгенерирована VLESS ссылка: {}", vlessLink);
        return vlessLink;
    }
    
    /**
     * Генерирует QR код из VLESS ссылки
     * Возвращает изображение в формате Base64 для отображения в HTML
     */
    public String generateQrCode() throws WriterException, IOException {
        String vlessLink = generateVlessLink();
        
        // Создаем QR код размером 300x300 пикселей
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            vlessLink,
            BarcodeFormat.QR_CODE,
            300,
            300
        );
        
        // Конвертируем в PNG изображение
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        // Кодируем в Base64 для вставки в HTML
        byte[] imageBytes = outputStream.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        log.info("QR код успешно сгенерирован");
        return base64Image;
    }
    
    /**
     * Генерирует HTML страницу с QR кодом для удобного сканирования
     */
    public String generateQrPage() throws WriterException, IOException {
        String qrCodeBase64 = generateQrCode();
        String vlessLink = generateVlessLink();
        
        // Простая HTML страница с QR кодом и ссылкой
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>VPN Configuration</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        text-align: center;
                        padding: 50px;
                        background-color: #f0f0f0;
                    }
                    .container {
                        background: white;
                        padding: 30px;
                        border-radius: 10px;
                        display: inline-block;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    h1 {
                        color: #333;
                    }
                    .qr-code {
                        margin: 20px 0;
                    }
                    .link {
                        word-break: break-all;
                        background: #f5f5f5;
                        padding: 15px;
                        border-radius: 5px;
                        font-family: monospace;
                        font-size: 12px;
                        margin-top: 20px;
                    }
                    .instructions {
                        margin-top: 20px;
                        color: #666;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🔐 VPN Configuration</h1>
                    <div class="qr-code">
                        <img src="data:image/png;base64,%s" alt="QR Code"/>
                    </div>
                    <div class="instructions">
                        <p><strong>Инструкция:</strong></p>
                        <p>1. Установите v2rayNG на Android</p>
                        <p>2. Нажмите "+" → "Scan QR code"</p>
                        <p>3. Отсканируйте код выше</p>
                        <p>4. Подключитесь!</p>
                    </div>
                    <div class="link">
                        <strong>Или скопируйте ссылку:</strong><br/>
                        %s
                    </div>
                </div>
            </body>
            </html>
            """.formatted(qrCodeBase64, vlessLink);
    }
}
