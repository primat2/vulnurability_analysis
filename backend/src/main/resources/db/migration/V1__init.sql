CREATE TABLE artifact (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    ecosystem   VARCHAR(16)  NOT NULL CHECK (ecosystem IN ('MAVEN','NPM')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_artifact_name_eco UNIQUE (name, ecosystem)
);

CREATE TABLE vulnerability (
    id          BIGSERIAL PRIMARY KEY,
    cve_id      VARCHAR(64)  NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vulnerability_cve UNIQUE (cve_id)
);

CREATE TABLE ticket (
    id           BIGSERIAL PRIMARY KEY,
    jira_url     VARCHAR(512) NOT NULL,
    artifact_id  BIGINT       NOT NULL REFERENCES artifact(id),
    deadline     DATE,
    actual       BOOLEAN,                -- null = не триажено
    comment      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_ticket_jira_url UNIQUE (jira_url)
);

CREATE TABLE ticket_vulnerability (
    ticket_id        BIGINT NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    vulnerability_id BIGINT NOT NULL REFERENCES vulnerability(id) ON DELETE CASCADE,
    PRIMARY KEY (ticket_id, vulnerability_id)
);

CREATE INDEX idx_ticket_artifact ON ticket(artifact_id);
CREATE INDEX idx_ticket_actual   ON ticket(actual);
CREATE INDEX idx_tv_vuln         ON ticket_vulnerability(vulnerability_id);
