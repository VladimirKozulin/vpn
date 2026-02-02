package com.example.vpn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для главной страницы
 * Перенаправляет на страницу с QR кодом
 */
@Controller
public class HomeController {
    
    /**
     * Главная страница - перенаправляет на QR код
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/api/vpn/config/qr";
    }
}
