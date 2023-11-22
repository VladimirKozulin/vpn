package com.example.vpn.controller;

import com.example.vpn.model.VpnClient;
import com.example.vpn.service.ConfigService;
import com.example.vpn.service.QrCodeService;
import com.example.vpn.service.VpnClientService;
import com.example.vpn.service.XrayService;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * Контроллер главной страницы
 * Автоматически создает клиента и показывает QR код
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final VpnClientService vpnClientService;
    private final ConfigService configService;
    private final QrCodeService qrCodeService;
    private final XrayService xrayService;
    
    /**
     * Главная страница с QR кодом
     * GET /
     */
    @GetMapping("/")
    public String home(Model model) {
        try {
            log.info("📄 Загрузка главной страницы");
            
            // Получаем или создаем клиента
            List<VpnClient> clients = vpnClientService.getAllClients();
            VpnClient client;
            
            if (clients.isEmpty()) {
                // Создаем первого клиента автоматически
                log.info("👤 Клиентов нет, создаем автоматически");
                client = new VpnClient();
                client.setDeviceInfo("VPN Client");
                client.setIsActive(true);
                client = vpnClientService.createClient(client);
                
                // Перезапускаем Xray
                log.info("🔄 Перезапуск Xray с новым клиентом...");
                xrayService.restartXray();
                log.info("✅ Создан клиент с UUID: {}", client.getUuid());
            } else {
                // Берем первого клиента
                client = clients.get(0);
                log.info("👤 Используем существующего клиента UUID: {}", client.getUuid());
            }
            
            // Генерируем VLESS ссылку
            log.info("🔗 Генерация VLESS ссылки...");
            String vlessLink = configService.generateVlessLink(client);
            
            // Генерируем QR код и конвертируем в Base64 для встраивания в HTML
            log.info("📱 Генерация QR кода...");
            byte[] qrCode = qrCodeService.generateQrCode(vlessLink, 400, 400);
            String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCode);
            
            // Статус VPN
            boolean vpnRunning = xrayService.isRunning();
            log.info("🔌 Статус VPN: {}", vpnRunning ? "РАБОТАЕТ" : "ОСТАНОВЛЕН");
            
            model.addAttribute("client", client);
            model.addAttribute("vlessLink", vlessLink);
            model.addAttribute("qrCodeBase64", qrCodeBase64);
            model.addAttribute("vpnRunning", vpnRunning);
            
            log.info("✅ Главная страница успешно загружена");
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
