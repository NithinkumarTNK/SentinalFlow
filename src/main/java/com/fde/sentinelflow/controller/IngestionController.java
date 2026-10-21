package com.fde.sentinelflow.controller;

import com.fde.sentinelflow.model.ApiKey;
import com.fde.sentinelflow.repository.ApiKeyRepository;
import com.fde.sentinelflow.service.ApiKeyAuthService;
import com.fde.sentinelflow.service.KafkaEventProducer;
import com.fde.sentinelflow.service.RateLimiterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class IngestionController {

    private final ApiKeyAuthService authService;
    private final RateLimiterService rateLimiterService;
    private final KafkaEventProducer kafkaEventProducer;
    private final ApiKeyRepository apiKeyRepository;

    public IngestionController(ApiKeyAuthService authService,
                               RateLimiterService rateLimiterService,
                               KafkaEventProducer kafkaEventProducer, ApiKeyRepository apiKeyRepository) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.kafkaEventProducer = kafkaEventProducer;
        this.apiKeyRepository = apiKeyRepository;
    }

    @PostMapping("/events")
    public ResponseEntity<IngestionResponse> ingestEvent(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody AuditEventRequest request) {

        // 1. Validate API Key against PostgreSQL
        Optional<ApiKey> validKeyOpt = authService.validateApiKey(apiKey);
        if (validKeyOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or inactive API Key");
        }

        String tenantId = validKeyOpt.get().getTenant().getId().toString();

        // 2. Check Redis Rate Limiter per Tenant
        if (!rateLimiterService.isAllowed(tenantId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }

        // 3. Publish asynchronously to Apache Kafka
        kafkaEventProducer.publishEvent(tenantId, request.eventType(), request.sourceIp(), request.payload());

        return ResponseEntity.accepted().body(new IngestionResponse("SUCCESS", "Event accepted and queued for processing"));
    }
    // Example snippet inside your IngestionController or Security Interceptor
    public boolean hasPermission(String apiKey, String requiredScope) {
        // Look up key from Redis or PostgreSQL cache and check if scopes contain requiredScope
        return apiKeyRepository.findByKeyHash(apiKey)
                .map(key -> key.getScopes().contains(requiredScope))
                .orElse(false);
    }
}


record AuditEventRequest(
        @NotBlank String eventType,
        @NotBlank String sourceIp,
        String payload
) {}

record IngestionResponse(String status, String message) {}