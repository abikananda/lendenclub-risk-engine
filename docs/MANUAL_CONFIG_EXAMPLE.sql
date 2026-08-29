-- Replace usernames and values before running.
-- Run once per lender to create the config row.

INSERT INTO investment_config (lender_id, investment_amount, lending_rules, enabled)
SELECT id, 10000.00, 'REPEATED_LENDERS_HIGH_RISK,BULK_LENDERS', TRUE
FROM lender
WHERE username = 'abikananda';

INSERT INTO investment_config (lender_id, investment_amount, lending_rules, enabled)
SELECT id, 20000.00, 'GOOD_BUSINESS_LENDERS,REPEATED_LENDERS_LOW_RISK', TRUE
FROM lender
WHERE username = 'seconduser';

-- Before each run, update only the amount/rules you want to use.

UPDATE investment_config ic
JOIN lender l ON l.id = ic.lender_id
SET ic.investment_amount = 15000.00,
    ic.lending_rules = 'REPEATED_LENDERS_HIGH_RISK,BULK_LENDERS',
    ic.enabled = TRUE
WHERE l.username = 'abikananda';

UPDATE investment_config ic
JOIN lender l ON l.id = ic.lender_id
SET ic.investment_amount = 25000.00,
    ic.lending_rules = 'GOOD_BUSINESS_LENDERS,REPEATED_LENDERS_LOW_RISK',
    ic.enabled = TRUE
WHERE l.username = 'seconduser';
