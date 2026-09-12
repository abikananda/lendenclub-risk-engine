# Selenium borrower migration

This importer migrates historical rows from the Selenium project's `borrower_loans` table into
the risk engine's `borrower_profile` and `borrower_snapshot` tables.

The migration is idempotent:

- profiles are resolved through the existing name, borrower type, and estimated birth-year rules;
- existing snapshots are linked to the resolved profile when their profile is missing;
- a historical snapshot is created only when the loan ID does not already exist.

## Run from PowerShell

Configure the Selenium database and enable the one-time startup import:

```powershell
$env:SELENIUM_BORROWER_IMPORT_ENABLED="true"
$env:SELENIUM_BORROWER_IMPORT_RUN_ON_STARTUP="true"
$env:SELENIUM_SOURCE_DB_URL="jdbc:mysql://localhost:3306/pfmp?useSSL=false&serverTimezone=Asia/Kolkata"
$env:SELENIUM_SOURCE_DB_USERNAME="root"
$env:SELENIUM_SOURCE_DB_PASSWORD="your-password"
mvn spring-boot:run
```

The application logs the source-row, profile, snapshot, linked, existing, and skipped counts.
After a successful migration, set both import flags to `false` for normal application startup.

Running the import again is safe and provides a useful verification that no duplicate snapshots
are created.
