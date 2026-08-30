ALTER TABLE borrower_evaluation
    ADD COLUMN rule_version VARCHAR(30) NULL AFTER rule_code,
    ADD COLUMN ruleset_version VARCHAR(50) NULL AFTER rule_version;

CREATE TABLE lender_execution_lock (
    lender_id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    owner_id VARCHAR(100) NOT NULL,
    acquired_at TIMESTAMP(6) NOT NULL,
    heartbeat_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_execution_lock_lender FOREIGN KEY (lender_id) REFERENCES lender(id),
    INDEX idx_execution_lock_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workflow_checkpoint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    loan_id VARCHAR(64) NULL,
    rule_name VARCHAR(100) NULL,
    state VARCHAR(40) NOT NULL,
    message TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_checkpoint_session FOREIGN KEY (session_id) REFERENCES lending_session(session_id),
    INDEX idx_checkpoint_session (session_id),
    INDEX idx_checkpoint_loan (loan_id),
    INDEX idx_checkpoint_state (state),
    INDEX idx_checkpoint_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
