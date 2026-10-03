CREATE TABLE users (
    user_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE user_settings (
    setting_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    scan_on_browse     BOOLEAN     NOT NULL DEFAULT TRUE,
    show_notifications BOOLEAN     NOT NULL DEFAULT TRUE,
    risk_threshold     INTEGER     NOT NULL DEFAULT 50,
    theme              VARCHAR(50) NOT NULL DEFAULT 'light',
    language           VARCHAR(50) NOT NULL DEFAULT 'en',
    created_at         TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE domains (
    domain_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain        VARCHAR(255) NOT NULL UNIQUE,
    registrar     VARCHAR(255),
    created_date  DATE,
    updated_date  DATE,
    is_suspicious BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE urls (
    url_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    url          TEXT NOT NULL UNIQUE,
    domain_id    UUID NOT NULL REFERENCES domains(domain_id),
    is_ip        BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE scans (
    scan_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users(user_id),   -- nullable until JWT login exists
    url_id       UUID NOT NULL REFERENCES urls(url_id),
    domain_id    UUID NOT NULL REFERENCES domains(domain_id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    risk_score   INTEGER,
    risk_level   VARCHAR(20),
    summary      TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    completed_at TIMESTAMP
);

CREATE TABLE scan_results (
    result_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id        UUID NOT NULL REFERENCES scans(scan_id) ON DELETE CASCADE,
    analysis_type  VARCHAR(50) NOT NULL,
    result         JSONB,
    risk_score     INTEGER,
    risk_level     VARCHAR(20),
    explanation    TEXT,
    recommendation TEXT,
    model_used     VARCHAR(100),
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE scan_indicators (
    indicator_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id          UUID NOT NULL REFERENCES scans(scan_id) ON DELETE CASCADE,
    indicator_type   VARCHAR(50)  NOT NULL,
    indicator_name   VARCHAR(100) NOT NULL,
    value            TEXT,
    is_malicious     BOOLEAN NOT NULL DEFAULT FALSE,
    confidence_score INTEGER,
    details          JSONB,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE page_elements (
    element_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id       UUID NOT NULL REFERENCES scans(scan_id) ON DELETE CASCADE,
    element_type  VARCHAR(50) NOT NULL,
    element_value TEXT,
    is_suspicious BOOLEAN NOT NULL DEFAULT FALSE,
    reason        TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE threat_intel_records (
    threat_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_id        UUID REFERENCES domains(domain_id),
    scan_id          UUID REFERENCES scans(scan_id) ON DELETE CASCADE,
    source           VARCHAR(100) NOT NULL,
    indicator_type   VARCHAR(50),
    indicator_value  VARCHAR(255),
    threat_type      VARCHAR(100),
    confidence_score INTEGER,
    last_seen_at     TIMESTAMP,
    raw_data         JSONB,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE ai_agent_logs (
    log_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id       UUID NOT NULL REFERENCES scans(scan_id) ON DELETE CASCADE,
    agent_name    VARCHAR(100) NOT NULL,
    step          VARCHAR(100),
    input_data    JSONB,
    output_data   JSONB,
    status        VARCHAR(20),
    error_message TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reports (
    report_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users(user_id),
    scan_id      UUID NOT NULL REFERENCES scans(scan_id) ON DELETE CASCADE,
    report_type  VARCHAR(50),
    file_path    VARCHAR(255),
    generated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    scan_id         UUID REFERENCES scans(scan_id) ON DELETE CASCADE,
    type            VARCHAR(50),
    message         TEXT,
    is_read         BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE feedback (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(user_id),
    scan_id     UUID NOT NULL REFERENCES scans(scan_id) ON DELETE CASCADE,
    is_correct  BOOLEAN,
    comments    TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_scans_user_created ON scans(user_id, created_at);
CREATE INDEX idx_scans_url          ON scans(url_id);
CREATE INDEX idx_indicators_scan    ON scan_indicators(scan_id);
CREATE INDEX idx_threat_domain      ON threat_intel_records(domain_id);