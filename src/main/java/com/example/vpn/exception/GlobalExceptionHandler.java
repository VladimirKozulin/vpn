package com.example.vpn.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для REST API
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Обработка ошибок валидации (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Ошибка валидации: {}", errors);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse("Ошибка валидации", errors));
    }
    
    /**
     * Обработка RuntimeException с кастомными сообщениями
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime исключение: {}", ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(createErrorResponse(ex.getMessage(), null));
    }
    
    /**
     * Обработка всех остальных исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Необработанное исключение: {}", ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("Внутренняя ошибка сервера", null));
    }
    
    /**
     * Создать стандартный ответ с ошибкой
     */
    private Map<String, Object> createErrorResponse(String message, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("error", message);
        
        if (details != null) {
            response.put("details", details);
        }
        
        return response;
    }
}
