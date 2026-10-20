package com.fde.sentinelflow.service;

import com.fde.sentinelflow.model.ApiKey;
import com.fde.sentinelflow.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class ApiKeyAuthService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public Optional<ApiKey> validateApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }

        String hashedKey = hashApiKey(rawApiKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(hashedKey);

        // ADD THIS LINE FOR DEBUGGING:
        System.out.println("DEBUG - Generated Hash for [" + rawApiKey + "] is: "  + hashedKey);
        // Ensure key exists and belongs to an active tenant
        if (apiKeyOpt.isPresent() && apiKeyOpt.get().isActive() && apiKeyOpt.get().getTenant().isActive()) {
            return apiKeyOpt;
        }

        return Optional.empty();
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing API key", e);
        }
    }
}
