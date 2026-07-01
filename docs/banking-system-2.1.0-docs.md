# Added Features Summary — June 24, 2026

## #1 — Transaction Filtering

Added flexible filtering support to transaction history. All filters are optional and can be combined together. If no filters are provided, all transactions for the account are returned.

**Endpoint**

```http
GET /api/transactions/account/{accountId}?type=DEPOSIT&from=2026-01-01&to=2026-06-01&minAmount=100&maxAmount=5000
```

**Available filters**

| Parameter | Type | Description |
|---|---|---|
| type | TransactionType | DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN |
| from | LocalDate | Start date (inclusive) |
| to | LocalDate | End date (inclusive) |
| minAmount | BigDecimal | Minimum transaction amount |
| maxAmount | BigDecimal | Maximum transaction amount |

**Modified files**
- `TransactionRepository` — added `JpaSpecificationExecutor`
- `TransactionService` — `getByAccountId()` now supports dynamic filters
- `TransactionController` — added optional query parameters

---

## #2 — Bank Account Status Filtering

Added account status filtering for both user-specific and admin account views. Filters are optional and return all accounts when omitted.

**Endpoints**

```http
GET /api/accounts/users/{id}?status=ACTIVE
GET /api/accounts?status=FROZEN
```

**Available filters**

| Parameter | Type | Description |
|---|---|---|
| status | AccountStatus | ACTIVE, FROZEN, CLOSED |

**Notes**
- `GET /api/accounts/users/{id}` filters accounts belonging to a specific user
- `GET /api/accounts` allows admins to view all accounts with optional status filtering

**Modified files**
- `BankAccountRepository` — added `JpaSpecificationExecutor`
- `BankAccountService` — added `getAllAccounts()` and enhanced `getByUserId()` with status filtering
- `BankAccountController` — added status query parameter and new admin account listing endpoint

---

## #3 — Audit Log Filtering

Added filtering support for audit logs to help admins search activity history by action type and date range.

**Endpoint**

```http
GET /api/audit-logs/admin/{adminId}?action=CREATE_USER&from=2026-01-01&to=2026-06-01
```

**Available filters**

| Parameter | Type | Description |
|---|---|---|
| action | ActionType | Audit action type such as CREATE_USER, FREEZE_ACCOUNT |
| from | LocalDate | Start date (inclusive) |
| to | LocalDate | End date (inclusive) |

**Modified files**
- `AuditLogRepository` — added `JpaSpecificationExecutor`
- `AuditLogService` — `getByAdminId()` now supports action and date range filtering
- `AuditLogController` — added optional filter query parameters to audit log endpoint

---

### General Notes

- All filtering functionality is implemented using Spring Data JPA Specifications.
- Every filter parameter is optional.
- Multiple filters can be combined in a single request.
- Existing endpoint behavior remains unchanged when no filters are supplied.
- Filtering is available for transaction history, bank account listings, and audit log records.