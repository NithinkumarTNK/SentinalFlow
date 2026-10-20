package com.fde.sentinelflow.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;

    private String name;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public boolean isActive() {
        return isActive;
    }
}