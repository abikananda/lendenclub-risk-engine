CREATE TABLE lender_execution_lock (
    lender_id BIGINT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    owner_id VARCHAR(100) NOT NULL,
    acquired_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_execution_lock_lender FOREIGN KEY (lender_id) REFERENCES lender(id),
    INDEX idx_execution_lock_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
