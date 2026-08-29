ALTER TABLE borrower_snapshot
    ADD COLUMN borrower_name VARCHAR(160) NULL AFTER loan_id,
    ADD COLUMN risk_category VARCHAR(50) NULL AFTER repeated,
    ADD COLUMN remaining_amount DECIMAL(12,2) NULL AFTER risk_category,
    ADD COLUMN repayment_frequency VARCHAR(50) NULL AFTER remaining_amount,
    ADD COLUMN panel_details JSON NULL AFTER repayment_frequency;
