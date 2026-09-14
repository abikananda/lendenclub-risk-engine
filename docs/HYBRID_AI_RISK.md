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
