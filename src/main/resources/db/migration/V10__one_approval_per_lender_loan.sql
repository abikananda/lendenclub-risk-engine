-- Approval is a permanent claim for the lender/loan, shared by every rule and session.
CREATE TABLE loan_approval (
    lender_id BIGINT NOT NULL,
    loan_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    approved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invested_at TIMESTAMP NULL,
    PRIMARY KEY (lender_id, loan_id),
    CONSTRAINT fk_loan_approval_lender FOREIGN KEY (lender_id) REFERENCES lender(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Preserve earlier approvals, including loans whose investment status was never reported.
INSERT IGNORE INTO loan_approval (lender_id, loan_id, session_id)
SELECT s.lender_id, e.loan_id, MIN(e.session_id)
FROM borrower_evaluation e JOIN lending_session s ON s.session_id = e.session_id
WHERE e.decision = 'INVEST'
GROUP BY s.lender_id, e.loan_id;

INSERT IGNORE INTO loan_approval (lender_id, loan_id, session_id)
SELECT i.lender_id, i.loan_id, MIN(i.session_id)
FROM investment i WHERE i.status = 'SUCCESS'
GROUP BY i.lender_id, i.loan_id;

UPDATE loan_approval a JOIN investment i
    ON i.lender_id = a.lender_id AND i.loan_id = a.loan_id AND i.status = 'SUCCESS'
SET a.invested_at = CURRENT_TIMESTAMP
WHERE a.invested_at IS NULL;
