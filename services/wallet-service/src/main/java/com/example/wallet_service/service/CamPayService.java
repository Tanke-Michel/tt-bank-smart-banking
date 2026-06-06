package com.example.wallet_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * CamPay Mobile Money client (MTN / Orange Money, Cameroon).
 * Uses the temporary-token auth flow: POST /token/ with {username,password}
 * returns a short-lived token, used as "Authorization: Token <token>".
 */
@Slf4j
@Service
public class CamPayService {

    @Value("${campay.base-url:https://demo.campay.net/api/}")
    private String baseUrl;

    @Value("${campay.username:}")
    private String username;

    @Value("${campay.password:}")
    private String password;

    @Value("${campay.token:}")
    private String permanentToken;

    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Obtain a fresh access token from username/password. Falls back to the permanent token. */
    private String getToken() {
        // Prefer the temporary token flow if username/password are set
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            try {
                String url = baseUrl + "token/";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, String> creds = new HashMap<>();
                creds.put("username", username);
                creds.put("password", password);
                ResponseEntity<String> res = http.exchange(url, HttpMethod.POST,
                        new HttpEntity<>(creds, headers), String.class);
                JsonNode json = mapper.readTree(res.getBody());
                String t = json.path("token").asText(null);
                if (t != null && !t.isBlank()) {
                    log.info("CamPay temporary token acquired");
                    return t;
                }
            } catch (Exception e) {
                log.warn("CamPay token endpoint failed ({}), falling back to permanent token", e.getMessage());
            }
        }
        return permanentToken;
    }

    public String collect(String phone, String amount, String description, String externalRef) {
        String token = getToken();
        String url = baseUrl + "collect/";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token " + token);

        Map<String, String> body = new HashMap<>();
        body.put("amount", amount);
        body.put("currency", "XAF");
        body.put("from", phone);
        body.put("description", description);
        body.put("external_reference", externalRef == null ? "" : externalRef);

        try {
            ResponseEntity<String> res = http.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
            JsonNode json = mapper.readTree(res.getBody());
            String reference = json.path("reference").asText(null);
            log.info("CamPay collect initiated: ref={} phone={} amount={}", reference, phone, amount);
            return reference;
        } catch (Exception e) {
            log.error("CamPay collect failed: {}", e.getMessage());
            throw new RuntimeException("Mobile money request failed: " + e.getMessage());
        }
    }

    public String status(String reference) {
        String token = getToken();
        String url = baseUrl + "transaction/" + reference + "/";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + token);
        try {
            ResponseEntity<String> res = http.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            JsonNode json = mapper.readTree(res.getBody());
            return json.path("status").asText("PENDING");
        } catch (Exception e) {
            log.error("CamPay status check failed: {}", e.getMessage());
            return "PENDING";
        }
    }
}
