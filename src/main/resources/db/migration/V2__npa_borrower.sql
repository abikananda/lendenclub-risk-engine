CREATE TABLE npa_borrower (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    borrower_name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    hit_count BIGINT NOT NULL DEFAULT 0,
    last_hit_at TIMESTAMP NULL,
    last_session_id VARCHAR(64) NULL,
    last_loan_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_npa_borrower_normalized_name UNIQUE (normalized_name),
    INDEX idx_npa_borrower_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
