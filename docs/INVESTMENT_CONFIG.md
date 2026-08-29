# Investment configuration

`lender` stores permanent account information. Per-run budget and lending rules are read from `investment_config`.

Each lender should normally have exactly one row.

Example initial rows:

```sql
INSERT INTO investment_config (lender_id, investment_amount, lending_rules, enabled)
SELECT id, 10000.00, 'REPEATED_LENDERS_HIGH_RISK,BULK_LENDERS', TRUE
FROM lender
WHERE external_lender_id = 'LENDER_A';

INSERT INTO investment_config (lender_id, investment_amount, lending_rules, enabled)
SELECT id, 20000.00, 'GOOD_BUSINESS_LENDERS,REPEATED_LENDERS_LOW_RISK', TRUE
FROM lender
WHERE external_lender_id = 'LENDER_B';
```

Before a future run, change only that lender's config row, for example:

```sql
UPDATE investment_config ic
JOIN lender l ON l.id = ic.lender_id
SET ic.investment_amount = 15000.00,
    ic.lending_rules = 'REPEATED_LENDERS_HIGH_RISK,BULK_LENDERS',
    ic.enabled = TRUE
WHERE l.external_lender_id = 'LENDER_A';
```

`lending_rules` stores comma-separated `LendingRule` enum codes.

The backend starts a specific lender with:

```http
GET /api/lender/data?lenderId=LENDER_A
```

At session creation, `investment_amount` and `lending_rules` are copied into `lending_session`. Changing `investment_config` later affects only future sessions.
