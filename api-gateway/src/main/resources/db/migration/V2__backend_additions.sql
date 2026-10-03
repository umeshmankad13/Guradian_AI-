-- API returns a confidence value
ALTER TABLE scans ADD COLUMN confidence NUMERIC(3,2)
    CHECK (confidence BETWEEN 0 AND 1);

-- Device management (extension installs)
CREATE TABLE devices (
    device_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    browser      VARCHAR(50),
    ext_version  VARCHAR(20),
    revoked      BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE scans ADD COLUMN device_id UUID REFERENCES devices(device_id);

-- Audit logging
CREATE TABLE audit_logs (
    log_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID REFERENCES users(user_id) ON DELETE SET NULL,
    action     VARCHAR(100) NOT NULL,
    resource   VARCHAR(100),
    ip_hash    VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);