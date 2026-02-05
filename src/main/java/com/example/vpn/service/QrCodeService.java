package com.example.vpn.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для генерации QR кодов
 * Используется для создания QR кодов с VLESS ссылками
 */
@Slf4j
@Service
public class QrCodeService {
    
    /**
     * Генерирует QR код из текста и возвращает как массив байтов PNG изображения
     * 
     * @param text текст для кодирования (VLESS ссылка)
     * @param width ширина QR кода в пикселях
     * @param height высота QR кода в пикселях
     * @return массив байтов PNG изображения
     */
    public byte[] generateQrCode(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        // Настройки для QR кода
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // Минимальный отступ
        
        // Генерируем битовую матрицу QR кода
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        
        // Конвертируем в PNG изображение
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        log.info("QR код сгенерирован, размер: {}x{}", width, height);
        return outputStream.toByteArray();
    }
    
    /**
     * Генерирует QR код стандартного размера (300x300)
     */
    public byte[] generateQrCode(String text) throws WriterException, IOException {
        return generateQrCode(text, 300, 300);
    }
}
