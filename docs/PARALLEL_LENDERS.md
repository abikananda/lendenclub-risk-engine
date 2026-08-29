# Parallel lenders

Parallel lender runs are isolated by `lending_session.session_id` and selected explicitly with `lender.username`.

Each lender must have:

- `lender.active = TRUE`
- a unique `lender.username`
- one enabled `investment_config` row
- its own configured `investment_amount`
- its own configured comma-separated `lending_rules`

Two different lenders can start sessions at the same time through separate calls such as:

```http
GET /api/lender/data?username=abikananda
GET /api/lender/data?username=seconduser
```

Changing `investment_config` affects future sessions only because the amount and rules are snapshotted into `lending_session` when the session starts.
