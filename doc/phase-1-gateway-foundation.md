mkdir -p docs && cat << 'EOF' > docs/phase-1-gateway-foundation.md
# Phase 1: API Ingestion Gateway & Multi-Tenant Foundation

## 1. Overview & Objectives
Phase 1 established the foundational backend architecture for **SentinelFlow**. The goal was to build a high-performance, secure, multi-tenant API Ingestion Gateway capable of receiving raw enterprise logs, validating tenant security credentials, enforcing rate limits, and persisting metadata.

### Core Architecture Pillars Built:
* **Multi-Tenant Database Design:** Relational separation of enterprise clients (`tenants`), secure authentication tokens (`api_keys`), and audit tracking (`audit_events_meta`).
* **Stateless Key Hashing & Validation:** Secure handling of client API keys using SHA-256 hashing (plaintext keys are never persisted).
* **Distributed Rate Limiting:** Token-bucket rate-limiting using Redis to protect the gateway from noisy neighbors and denial-of-service traffic spikes.
* **Automated Integration Testing:** End-to-end HTTP validation using Spring Boot `MockMvc` coupled with persistent container orchestration.

---

## 2. Infrastructure & Local Stack (`docker-compose.yml`)
We spun up local containerized dependencies for transactional state storage and caching:
* **PostgreSQL (v15):** Relational metadata store for tenants and hashed keys.
* **Redis (v7):** In-memory cache for fast tenant rate-limiting checks.

---

## 3. Database Schema Design (`schema.sql`)
The relational data model enforces strict foreign-key constraints and indexing for high-throughput lookups:

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enterprise Clients (Tenants)
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Secure Hashed API Keys
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);