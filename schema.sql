-- Enable UUID extension for secure primary keys
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tenants Table (Represents enterprise clients using your platform)
CREATE TABLE tenants (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         name VARCHAR(255) NOT NULL,
                         subdomain VARCHAR(100) UNIQUE NOT NULL,
                         is_active BOOLEAN DEFAULT TRUE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. API Keys Table (Used by tenant client apps to authenticate inbound requests)
CREATE TABLE api_keys (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
                          key_hash VARCHAR(255) NOT NULL UNIQUE, -- Store hashed API keys, never plaintext
                          key_prefix VARCHAR(10) NOT NULL,       -- First few chars for client display (e.g., "sf_live_")
                          name VARCHAR(100) NOT NULL,            -- Description like "Production Server Key"
                          is_active BOOLEAN DEFAULT TRUE,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Audit Events Metadata Table (Tracks summary states for quick reference)
CREATE TABLE audit_events_meta (
                                   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
                                   event_type VARCHAR(100) NOT NULL,
                                   source_ip VARCHAR(45),
                                   risk_score INT DEFAULT 0,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance at scale
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_audit_meta_tenant ON audit_events_meta(tenant_id, created_at DESC);