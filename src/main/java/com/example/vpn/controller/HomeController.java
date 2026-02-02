package com.example.vpn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер для главной страницы
 * Перенаправляет на UI админку
 */
@Controller
public class HomeController {
    
    /**
     * Главная страница - перенаправляет на админку
     * TODO: Создать UI для управления клиентами
     */
    @GetMapping("/")
    public String home() {
        // Пока перенаправляем на API документацию
        return "redirect:/api/clients";
    }
}
