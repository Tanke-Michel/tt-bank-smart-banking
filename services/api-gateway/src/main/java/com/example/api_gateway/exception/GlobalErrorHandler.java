package com.example.api_gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global error handler for the reactive gateway.
 *
 * Catches errors that occur at the gateway level (NOT inside downstream services)
 * and returns structured JSON instead of the default Spring Whitelabel error page.
 *
 * Key cases handled:
 *   - ConnectException      : downstream service is down → 503 Service Unavailable
 *   - ResponseStatusException: gateway-level 404, 405 etc. → pass status through
 *   - Any other Throwable   : → 500 Internal Server Error
 *
 * @Order(-1) ensures this runs before Spring Boot's DefaultErrorWebExceptionHandler.
 */
@Slf4j
@Component
@Order(-1)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;

        if (ex instanceof ConnectException) {
            // Downstream service is unreachable
            status  = HttpStatus.SERVICE_UNAVAILABLE;
            message = "The requested service is temporarily unavailable. Please try again later.";
            log.error("Downstream service connection failed: {}", ex.getMessage());

        } else if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
            log.warn("ResponseStatusException: {} {}", status, message);

        } else {
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected gateway error occurred. Please try again later.";
            log.error("Unhandled gateway exception for path {}: {}",
                    exchange.getRequest().getPath(), ex.getMessage(), ex);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        body.put("path",      exchange.getRequest().getPath().value());
        body.put("timestamp", LocalDateTime.now().toString());

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException jpe) {
            bytes = ("{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
