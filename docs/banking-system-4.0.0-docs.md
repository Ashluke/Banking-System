# Added Features Summary — July 1, 2026

## #1 — Transaction Analytics

Added transaction insight and spending trend analysis for a user's transaction history. Both endpoints accept the same request body containing the user's transactions and current balance.

**Endpoints**

```http
POST /analytics/transactions/insights
POST /analytics/transactions/trends
```

**Request body**

| Field | Type | Description |
|---|---|---|
| userId | int | ID of the user |
| currentBalance | float | Current account balance |
| transactions | TransactionData[] | List of transactions to analyze |

**`/insights` returns**
- Summary totals: deposited, withdrawn, transferred in/out, net flow, average transaction amount
- Monthly cash flow breakdown
- Transaction type breakdown (count and total per type)
- Top 3 deposits and top 3 withdrawals

**`/trends` returns**
- Monthly savings breakdown with income, expenses, savings, and savings rate per month
- Overall savings rate
- Spending trend direction: `INCREASING`, `DECREASING`, or `STABLE`

**Added files**
- `routers/transaction_router.py` — `/insights` and `/trends` endpoints
- `services/analytics_service.py` — `get_transaction_insights()` and `get_spending_trends()`

---

## #2 — Credit Score Calculation

Added a credit score engine that evaluates a user's financial profile based on their transaction history and account data. Score is scaled to the standard 400–850 range.

**Endpoint**

```http
POST /analytics/credit-score
```

**Request body**

| Field | Type | Description |
|---|---|---|
| transactionData | TransactionListRequest | User's transaction history and current balance |
| accountData | AccountData | Account metadata including status and creation date |

**Scoring factors**

| Factor | Weight | Description |
|---|---|---|
| Payment consistency | 35% | Deposit regularity; penalizes sudden large drains |
| Balance history | 30% | Current balance relative to total deposited; penalizes near-zero balance |
| Transaction frequency | 15% | Ideal range is 5–20 transactions per month |
| Debt-to-income ratio | 10% | Total withdrawals relative to total deposits |
| Account age | 10% | Older accounts score higher |

**Returns**
- Credit score (400–850), rating (`EXCELLENT` / `GOOD` / `FAIR` / `POOR`), loan eligibility with suggested interest rate, per-factor breakdown, and human-readable insights

**Added files**
- `routers/credit_score_router.py` — `/credit-score` endpoint
- `services/credit_score_service.py` — `calculate_credit_score()`

---

## #3 — Fraud Detection

Added fraud detection that analyzes a user's transactions for suspicious patterns and assigns an overall risk level.

**Endpoint**

```http
POST /analytics/detect
```

**Request body**

| Field | Type | Description |
|---|---|---|
| userId | int | ID of the user |
| currentBalance | float | Current account balance |
| transactions | TransactionData[] | List of transactions to analyze |

**Detection rules**

| Rule | Description |
|---|---|
| `LARGE_AMOUNT` | Transaction amount exceeds 3× the account average |
| `UNUSUAL_HOURS` | Transaction occurred between 10PM and 5AM |
| `RAPID_TRANSACTIONS` | More than 5 transactions within a 10-minute window |
| `RAPID_ACCOUNT_DRAIN` | More than 80% of account balance withdrawn in a single day |

**Returns**
- Risk level (`LOW` / `MEDIUM` / `HIGH` / `CRITICAL`), total flag count, suspicious transaction count, per-flag details, and a summary message

**Added files**
- `routers/fraud_router.py` — `/detect` endpoint
- `services/fraud_service.py` — `detect_fraud()`

---

## #4 — Portfolio Analytics

Added portfolio performance analysis for a user's stock holdings. Calculates gain/loss, diversification, volatility, and risk per holding and across the full portfolio.

**Endpoint**

```http
POST /analytics/performance
```

**Request body**

| Field | Type | Description |
|---|---|---|
| userId | int | ID of the user |
| holdings | StockHolding[] | List of stock holdings with purchase and current price |

**Returns**
- Per-holding cost basis, current value, gain/loss (absolute and percent), and portfolio weight
- Portfolio-level totals and gain/loss
- Diversification score (0–100) using the Herfindahl–Hirschman Index
- Average daily volatility and risk level (`LOW` / `MEDIUM` / `HIGH`)
- Best and worst performers by gain/loss percent
- Plain-language portfolio insight

**Added files**
- `routers/portfolio_router.py` — `/performance` endpoint
- `services/portfolio_service.py` — `analyze_portfolio()`

---

## #5 — Savings Prediction

Added savings forecasting based on historical monthly net flow. Projects future balance at 3, 6, and 12 months with confidence percentages. Requires at least 3 transactions to generate a prediction.

**Endpoint**

```http
POST /analytics/savings
```

**Request body**

| Field | Type | Description |
|---|---|---|
| userId | int | ID of the user |
| currentBalance | float | Current account balance |
| transactions | TransactionData[] | List of transactions (minimum 3 required) |

**Returns**
- Average monthly net flow
- Savings trend: `IMPROVING`, `DECLINING`, or `STABLE`
- Projections at 3, 6, and 12 months with confidence percentage
- Month-by-month 12-month forecast
- Plain-language insight

**Added files**
- `routers/prediction_router.py` — `/savings` endpoint
- `services/prediction_service.py` — `predict_savings()`

---

## #6 — Ownership Check Refactor (Java)

Extracted a repeated authorization pattern across 7 Java services into a shared `AuthorizationUtil` utility class under `com.banking.system.util`.

**Added methods**

| Method | Description |
|---|---|
| `assertOwnerOrAdmin(ownerId, requesterId, isAdmin)` | Throws `UnauthorizedActionException` if requester is neither the owner nor an admin |
| `assertOwnerOrAdmin(ownerId, requesterId, isAdmin, message)` | Same with a custom exception message |
| `isOwnerOrAdmin(ownerId, requesterId, isAdmin)` | Boolean variant for use in stream/filter checks |

**Notes**
- `LoanService.makeRepayment` retains its own stricter inline check with no admin bypass, as that method intentionally disallows admin override

**Modified files**
- `AuthorizationUtil` — new utility class
- `AnalyticsService` — portfolio access check
- `AppUserService` — account update ownership check
- `BankAccountService` — primary owner check on account closure
- `LoanService` — loan access checks (`getById`, `getByUserId`, `getRepaymentSchedule`)
- `StockHoldingService` — stock holding ownership check
- `TransactionService` — account ownership check on transaction history
- `UserService` — user profile update ownership check

---

### General Notes

- All Python analytics endpoints are `POST` and accept a JSON request body.
- Analytics endpoints return empty/default responses when no transaction data is provided rather than throwing errors.
- The Java refactor introduces no behavioral changes — only code organization.