# Dimos Ledger

A double-entry ledger system built with Spring Boot and PostgreSQL. It manages fund transfers between accounts with full auditability — every transfer creates two journal entries (debit and credit), account balances are protected by HMAC-SHA256 checksums, and transfers are idempotent via a correlation ID.

## Tech Stack

- Java 25 + Spring Boot 4.0.1
- PostgreSQL 16
- Liquibase (schema migrations)
- Testcontainers (integration tests)
- Swagger UI at `/swagger-ui.html`

---

## Running Locally

**Prerequisites:** Docker, Java 25

```bash
# Start the database
docker compose up -d

# Run the application
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

On startup, a seed account is created automatically:

| Account Reference        | Currency | Balance      |
|--------------------------|----------|--------------|
| `Main-SYP-Topup-account` | SYP      | 1,000,000.00 |

This account acts as the funding source for manual testing and demos.

---

## How to Test It

### 1. Create an account

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "currencyCode": "SYP"
  }'
```

**Response:**
```json
{
  "accountReference": "ACC-A1B2C3D4",
  "userId": "user-123",
  "currencyCode": "SYP",
  "balance": 0,
  "createdAt": "2026-03-29T10:00:00"
}
```

Take note of the `accountReference` — you'll use it as the receiver in the next step.

---

### 2. Transfer funds from the topup account

```bash
curl -X POST http://localhost:8080/api/v1/operations/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "correlationId": "unique-id-001",
    "senderAccountReference": "Main-SYP-Topup-account",
    "receiverAccountReference": "ACC-A1B2C3D4",
    "amount": 500.00
  }'
```

**Response:**
```json
{
  "transaction": {
    "correlationId": "unique-id-001",
    "transactionReference": "...",
    "senderAccountReference": "Main-SYP-Topup-account",
    "receiverAccountReference": "ACC-A1B2C3D4",
    "amount": 500.00,
    "currency": "SYP",
    "status": "COMPLETED",
    "entries": [
      { "accountReference": "Main-SYP-Topup-account", "amount": -500.00, "updatedBalance": 999500.00 },
      { "accountReference": "ACC-A1B2C3D4",           "amount":  500.00, "updatedBalance":    500.00 }
    ]
  }
}
```

The `correlationId` must be unique per transfer. Resending the same one returns a `DUPLICATE_CORRELATION_ID` error — this is intentional (idempotency protection).

---

### 3. Check account balance

```bash
# By account ID (returned from the create call)
curl http://localhost:8080/api/v1/accounts/{id}

# All accounts for a user
curl "http://localhost:8080/api/v1/accounts?userId=user-123"
```

---

## Running Tests

```bash
./gradlew test
```

Integration tests spin up a real PostgreSQL instance via Testcontainers — no setup needed.

---

## API Reference

| Method | Endpoint                        | Description                      |
|--------|---------------------------------|----------------------------------|
| POST   | `/api/v1/accounts`              | Create an account                |
| GET    | `/api/v1/accounts/{id}`         | Get account by ID                |
| GET    | `/api/v1/accounts?userId=`      | Get all accounts for a user      |
| POST   | `/api/v1/operations/transfer`   | Transfer funds between accounts  |

Full interactive docs: `http://localhost:8080/swagger-ui.html`

---

## What's Not Implemented Yet

### Authentication & Authorization
No auth layer exists — all endpoints are open. A production system would need:
- JWT or OAuth2 to authenticate callers
- Authorization rules so a user can only access their own accounts
- A separate service account identity for system-initiated operations (e.g. the topup account)

### Charges & Fees
Transfers execute at face value with no fee deduction. Missing:
- Fee calculation per transfer (flat or percentage-based)
- Fee ledger entries posted to a designated revenue account
- Currency-specific fee configuration

### Additional Operation Types
Only `TRANSFER` is implemented. A real ledger would also need:
- **Deposit** — credit an account from an external source
- **Withdrawal** — debit an account to an external destination
- **Reversal** — cancel a completed transaction and restore balances
- **Hold / Release** — reserve funds without transferring them

### Pool Accounts
No concept of pooled or aggregated accounts exists. Missing:
- Pool accounts that aggregate balances across multiple sub-accounts
- Sub-account to pool linkage
- Consolidated balance reporting across a pool

### Currency Decimal Precision
All amounts are stored with a fixed 4 decimal places regardless of currency. A production system would need:
- A `decimalPlaces` field per currency (e.g. 2 for USD/EUR, 0 for JPY, 3 for KWD)
- Validation that transfer amounts don't exceed the allowed precision for the currency
- Rounding rules enforced at the service layer before persisting

### Transaction History & Inquiry
The service layer has `TransactionService` with history and inquiry logic, but no REST endpoints are exposed for it yet. The DTOs (`TransactionHistoryRequest`, `TransactionInquiryRequest`) are defined but not wired to a controller.
