# Hybrid AI risk reviewer

Drools remains the authoritative decision engine. The optional Ollama reviewer receives only
numeric and categorical borrower facts; borrower names, email addresses, account numbers and
other direct identifiers are not included.

## Modes

| Mode | Behaviour |
| --- | --- |
| `off` | AI is disabled (default). |
| `shadow` | Stores the AI assessment for comparison; the Drools decision and amount are unchanged. |
| `guardrail` | AI may reduce an approved amount or turn it into `SKIP` for manual review. It can never increase an amount or override a Drools rejection. Provider errors fail closed to `SKIP`. |

## Ollama configuration

Run Ollama separately and make its API reachable by this service. Example environment:

```text
AI_ENABLED=true
AI_PROVIDER=ollama
AI_MODE=shadow
AI_BASE_URL=http://localhost:11434
AI_MODEL=llama3.2:3b
AI_TIMEOUT=10s
AI_PROMPT_VERSION=v1
```

Start with `shadow`. Review stored results in `borrower_evaluation` before enabling
`guardrail`. The model response must be structured JSON and is validated for score,
confidence, recommendation and reduction amount.

## Promotion checklist

1. Compare AI recommendations with historical repayment outcomes.
2. Measure false-rejection and false-approval rates by model and prompt version.
3. Review all `PROVIDER_ERROR` and `INVALID_RESPONSE` rows.
4. Approve a pinned model and prompt version.
5. Enable `guardrail` for a small lender cohort before wider use.


## Test endpoint

`POST /api/ai-risk/test` invokes only the configured AI provider. It does not run
Drools, create a borrower evaluation, or initiate an investment.

```bash
curl -X POST http://localhost:8081/api/ai-risk/test \
  -H "Content-Type: application/json" \
  -H "X-API-Key: <backend-api-key>" \
  -d '{
    "creditScore": 701,
    "lendenScore": 800,
    "income": 50000,
    "loanAmount": 5000,
    "interestRate": 36.48,
    "tenure": 4,
    "emi": 1250,
    "age": 35,
    "borrowerType": "SALARIED",
    "repeated": false,
    "trusted": false
  }'
```

When `AI_ENABLED=false`, the endpoint returns `status: "DISABLED"`. With Ollama
enabled, a successful response returns `status: "COMPLETED"` plus the structured
score, recommendation, confidence, concerns, positive factors, rationale, model,
prompt version, and latency. If backend API-key authentication is disabled, omit
the `X-API-Key` header.
