package com.example.vpn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Главный класс Spring Boot приложения для VPN сервера
 * Управляет Xray процессом через REST API
 */
@SpringBootApplication
@EnableConfigurationProperties
public class VpnApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpnApplication.class, args);
    }

}
