# Import Selenium borrowers through APIs

Start the Selenium project's borrower migration API on port `8090`. Configure the risk engine
with the same migration key:

```text
SELENIUM_BORROWER_API_URL=http://localhost:8090/api/migration/borrowers
SELENIUM_BORROWER_API_KEY=<same-key>
SELENIUM_BORROWER_PAGE_SIZE=100
```

Trigger a bounded migration batch:

```http
POST /api/admin/migrations/selenium-borrowers?afterId=0&maxRecords=500
```

The response contains `lastProcessedId`. If `complete` is false, send the next request using that
value as `afterId`. Each source ID is fetched separately and failures are returned without losing
the resume cursor.

The migration is idempotent. It resolves borrower profiles using the existing identity rules,
creates a historical snapshot only if the loan ID is absent, and links existing snapshots whose
borrower profile is missing.
