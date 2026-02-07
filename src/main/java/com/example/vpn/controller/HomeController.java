package com.example.vpn.controller;

import com.example.vpn.model.PendingClient;
import com.example.vpn.service.*;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/**
 * Контроллер главной страницы
 * Генерирует новую ссылку при каждом обращении
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ConfigService configService;
    private final QrCodeService qrCodeService;
    private final XrayService xrayService;
    private final XrayGrpcService xrayGrpcService;
    private final PendingClientService pendingClientService;
    private final ConnectionMonitorService connectionMonitorService;
    
    /**
     * Главная страница с QR кодом
     * GET /
     * Каждое обращение создаёт нового pending клиента
     */
    @GetMapping("/")
    public String home(Model model) {
        try {
            log.info("📄 Загрузка главной страницы - генерация нового клиента");
            
            // Генерируем новый UUID для клиента
            String uuid = UUID.randomUUID().toString();
            String deviceInfo = "VPN Client";
            
            // Добавляем в Xray (пока через конфиг, TODO: через gRPC)
            log.info("🔧 Добавление клиента в Xray: {}", uuid);
            xrayGrpcService.addUser(uuid, deviceInfo);
            
            // Сохраняем в pending (в памяти)
            PendingClient pendingClient = new PendingClient(uuid, deviceInfo);
            pendingClientService.add(pendingClient);
            
            // Планируем проверку через 5 минут
            connectionMonitorService.scheduleCheck(uuid);
            
            // Генерируем VLESS ссылку
            log.info("🔗 Генерация VLESS ссылки...");
            String vlessLink = configService.generateVlessLink(uuid, deviceInfo);
            
            // Генерируем QR код
            log.info("📱 Генерация QR кода...");
            byte[] qrCode = qrCodeService.generateQrCode(vlessLink, 400, 400);
            String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCode);
            
            // Статус VPN
            boolean vpnRunning = xrayService.isRunning();
            log.info("🔌 Статус VPN: {}", vpnRunning ? "РАБОТАЕТ" : "ОСТАНОВЛЕН");
            
            model.addAttribute("uuid", uuid);
            model.addAttribute("vlessLink", vlessLink);
            model.addAttribute("qrCodeBase64", qrCodeBase64);
            model.addAttribute("vpnRunning", vpnRunning);
            model.addAttribute("expiresAt", pendingClient.getExpiresAt());
            
            log.info("✅ Главная страница загружена. UUID: {}, истекает: {}", 
                uuid, pendingClient.getExpiresAt());
            return "index";
            
        } catch (WriterException | IOException e) {
            log.error("❌ Ошибка генерации QR кода", e);
            model.addAttribute("error", "Ошибка генерации QR кода: " + e.getMessage());
            return "error";
        } catch (Exception e) {
            log.error("❌ Ошибка загрузки главной страницы", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
