CREATE TABLE borrower_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    gender_normalized VARCHAR(30) NULL,
    borrower_type_normalized VARCHAR(50) NULL,
    birth_year_estimate INT NULL,
    total_lent DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    successful_investment_count BIGINT NOT NULL DEFAULT 0,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_lent_at TIMESTAMP NULL,
    CONSTRAINT uk_borrower_profile_public_id UNIQUE (public_id),
    INDEX idx_borrower_profile_identity (normalized_name, gender_normalized, borrower_type_normalized, birth_year_estimate),
    INDEX idx_borrower_profile_name (normalized_name)
);

ALTER TABLE borrower_snapshot
    ADD COLUMN borrower_profile_id BIGINT NULL,
    ADD CONSTRAINT fk_borrower_snapshot_profile
        FOREIGN KEY (borrower_profile_id) REFERENCES borrower_profile(id),
    ADD INDEX idx_borrower_snapshot_profile (borrower_profile_id),
    ADD INDEX idx_borrower_snapshot_session_loan (session_id, loan_id);

ALTER TABLE investment
    ADD COLUMN borrower_profile_id BIGINT NULL,
    ADD CONSTRAINT fk_investment_borrower_profile
        FOREIGN KEY (borrower_profile_id) REFERENCES borrower_profile(id),
    ADD INDEX idx_investment_borrower_profile (borrower_profile_id),
    ADD INDEX idx_investment_session_loan (session_id, loan_id);
