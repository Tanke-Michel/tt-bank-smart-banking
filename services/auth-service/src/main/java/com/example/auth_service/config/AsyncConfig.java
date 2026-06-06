package com.example.auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async support so EmailService.send() runs on a separate thread
 * and never blocks the HTTP response.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot auto-configures a thread pool for @Async tasks.
    // No further configuration is needed for the current load.
}
