# Security configuration

## Required secrets

The application requires these secrets at runtime. Keep them in a secret manager or protected
environment variables; do not commit them to source control.

| Variable | Purpose |
| --- | --- |
| `BACKEND_API_KEY` | Authenticates callers of `/api/**` |
| `OTP_CREDENTIAL_ENCRYPTION_KEY` | Encrypts lender OTP mailbox passwords at rest |

API authentication is enabled by default. It may be disabled explicitly for isolated local
development, but the application refuses to start with authentication disabled under the `prod`
Spring profile.

`OTP_CREDENTIAL_ENCRYPTION_KEY` must be a Base64-encoded 32-byte key. The same key must be retained
across restarts; losing or changing it makes existing credentials unreadable.

Generate a key in Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Generate a separate API key:

```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

## Existing databases

Flyway migration `V9` expands `lender.otp_password` to hold authenticated ciphertext. During the
first application startup, legacy plaintext values are encrypted with AES-256-GCM before the
application reports itself ready. New values written through JPA are encrypted automatically.

Back up the database and configure `OTP_CREDENTIAL_ENCRYPTION_KEY` before deploying this migration.
After deployment, verify that every value starts with `enc:v1:` without printing complete values:

```sql
SELECT id,
       CASE WHEN otp_password LIKE 'enc:v1:%' THEN 'ENCRYPTED' ELSE 'NOT_ENCRYPTED' END AS credential_state
FROM lender;
```

## Exposed credentials

Removing a secret from the latest branch does not invalidate it or remove it from earlier Git
commits. Any credential previously committed must be revoked at its provider first. Repository
history should then be rewritten in a separately coordinated maintenance operation, followed by a
force-push of all affected branches and tags and a fresh clone by every contributor.
