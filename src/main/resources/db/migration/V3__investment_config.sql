CREATE TABLE investment_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lender_id BIGINT NOT NULL,
    investment_amount DECIMAL(12,2) NOT NULL,
    lending_rules VARCHAR(1000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_investment_config_lender UNIQUE (lender_id),
    CONSTRAINT fk_investment_config_lender FOREIGN KEY (lender_id) REFERENCES lender(id),
    INDEX idx_investment_config_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE lending_session
    ADD COLUMN configured_investment_amount DECIMAL(12,2) NULL AFTER lender_id,
    ADD COLUMN configured_lending_rules VARCHAR(1000) NULL AFTER configured_investment_amount;
