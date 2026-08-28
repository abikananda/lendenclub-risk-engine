CREATE TABLE IF NOT EXISTS lender (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_lender_id VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    wallet_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    username VARCHAR(50) NOT NULL UNIQUE,
    mobile_number VARCHAR(15) NOT NULL UNIQUE,
    otp_username VARCHAR(50) NOT NULL UNIQUE,
    otp_password VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    lending_rules TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lending_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    lender_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    total_borrowers_scanned INT NOT NULL DEFAULT 0,
    total_borrowers_evaluated INT NOT NULL DEFAULT 0,
    total_investments INT NOT NULL DEFAULT 0,
    total_amount_invested DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    successful_investments INT NOT NULL DEFAULT 0,
    failed_investments INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_lender FOREIGN KEY (lender_id) REFERENCES lender(id),
    INDEX idx_session_id (session_id),
    INDEX idx_session_lender (lender_id),
    INDEX idx_session_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS borrower_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id VARCHAR(64) NOT NULL,
    credit_score INT NOT NULL,
    lenden_score INT NOT NULL,
    income DECIMAL(12,2) NOT NULL,
    loan_amount DECIMAL(12,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    tenure_months INT NOT NULL,
    emi DECIMAL(12,2) NOT NULL,
    age INT NOT NULL,
    borrower_type VARCHAR(50) NOT NULL,
    repeated BOOLEAN NOT NULL,
    raw_payload JSON NULL,
    scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(64) NOT NULL,
    INDEX idx_snapshot_loan (loan_id),
    INDEX idx_snapshot_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS borrower_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    investment_amount DECIMAL(12,2) NOT NULL,
    rule_name VARCHAR(100) NULL,
    rule_code VARCHAR(50) NULL,
    reason TEXT NULL,
    ai_risk_score DOUBLE NULL,
    engine_version VARCHAR(20) NULL,
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eval_loan (loan_id),
    INDEX idx_eval_session (session_id),
    INDEX idx_eval_decision (decision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS investment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    lender_id BIGINT NOT NULL,
    requested_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    external_investment_id VARCHAR(100) NULL UNIQUE,
    failure_reason TEXT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_inv_lender FOREIGN KEY (lender_id) REFERENCES lender(id),
    INDEX idx_inv_loan (loan_id),
    INDEX idx_inv_session (session_id),
    INDEX idx_inv_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS otp_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    consumed_at TIMESTAMP NULL,
    external_reference VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(64) NULL,
    session_id VARCHAR(64) NULL,
    loan_id VARCHAR(64) NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
