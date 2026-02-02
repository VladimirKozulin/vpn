package com.example.vpn.config;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Конфигурация подключения к Tarantool
 */
@Slf4j
@Configuration
public class TarantoolConfig {
    
    @Value("${tarantool.host}")
    private String host;
    
    @Value("${tarantool.port}")
    private int port;
    
    @Value("${tarantool.username}")
    private String username;
    
    @Value("${tarantool.password}")
    private String password;
    
    @Bean
    @Lazy
    public TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> tarantoolClient() {
        log.info("Подключение к Tarantool: {}:{}", host, port);
        
        try {
            // Даем Tarantool время на запуск
            Thread.sleep(2000);
            
            TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client = 
                TarantoolClientFactory.createClient()
                    .withAddress(host, port)
                    .withCredentials(username, password)
                    .build();
            
            log.info("Успешное подключение к Tarantool");
            return client;
        } catch (Exception e) {
            log.error("Ошибка подключения к Tarantool", e);
            throw new RuntimeException("Не удалось подключиться к Tarantool", e);
        }
    }
}
