CREATE TABLE trusted_borrower (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    borrower_profile_id BIGINT NOT NULL UNIQUE,
    borrower_name VARCHAR(160) NOT NULL,
    trust_score DECIMAL(7,4) NULL,
    successful_repayment_count BIGINT NULL,
    total_repaid_amount DECIMAL(14,2) NULL,
    latest_credit_score INT NULL,
    latest_lenden_score INT NULL,
    qualification_reason VARCHAR(500) NULL,
    source VARCHAR(100) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    derived_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_trusted_borrower_profile
        FOREIGN KEY (borrower_profile_id) REFERENCES borrower_profile(id),
    INDEX idx_trusted_borrower_active (active),
    INDEX idx_trusted_borrower_name (borrower_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
