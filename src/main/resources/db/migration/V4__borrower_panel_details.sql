ALTER TABLE borrower_snapshot
    ADD COLUMN borrower_name VARCHAR(160) NULL AFTER loan_id,
    ADD COLUMN loan_type VARCHAR(80) NULL AFTER repeated,
    ADD COLUMN repayment_frequency VARCHAR(50) NULL AFTER loan_type,
    ADD COLUMN gender VARCHAR(30) NULL AFTER repayment_frequency,
    ADD COLUMN risk_category VARCHAR(50) NULL AFTER gender;
