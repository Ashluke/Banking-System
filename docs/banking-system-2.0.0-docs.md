# Added Features Summary — June 23, 2026

## #1 — Combined Customer Registration

Single endpoint `POST /api/users/register` creates both `AppUser` and `User` profile in one call. No more two-step creation — admin fills one form and gets one response back. Role is auto-set to `USER`.

**New files**
- `UserCreateRequestDto` — replaced old one, now includes email, password, firstName, lastName, phoneNumber, address
- `UserRegisterResponseDto` — full response after registration including email, role, createdAt

**Modified files**
- `UserService` — `createCustomer()` replaces `createUser()`, wrapped in `@Transactional`
- `UserController` — `POST /api/users` changed to `POST /api/users/register`

---

## #2 — Combined Admin Registration

Single endpoint `POST /api/admins/register` creates both `AppUser` and `Admin` profile in one call. Role is auto-set to `ADMIN`. Returns full response including email, role, and createdAt.

**New files**
- `AdminCreateRequestDto` — replaced old one, now includes email, password, staffCode, firstName, lastName
- `AdminRegisterResponseDto` — full response after registration

**Modified files**
- `AdminService` — `createAdmin()` now handles `AppUser` creation internally, wrapped in `@Transactional`
- `AdminController` — `POST /api/admins` changed to `POST /api/admins/register`

---

## #3 — Finnhub Stock Market Integration

Real-time stock price fetching via the Finnhub API. Accessible by both `USER` and `ADMIN` roles. Supports multiple symbols in a single request. Invalid or unknown symbols are skipped without crashing the request.

**Endpoint**

```
GET /api/market/stocks?symbols=AAPL,GOOGL,MSFT
```

**Response fields per symbol**

| Field | Description |
|---|---|
| symbol | Stock ticker |
| currentPrice | Current price |
| change | Price change |
| percentChange | Percent change |
| highPrice | Day high |
| lowPrice | Day low |
| openPrice | Opening price |
| previousClose | Previous closing price |

**Notes**
- Uses `RestClient` built into Spring 6+ — no extra dependency needed
- Free tier — 60 calls per minute

**New files**
- `StockQuoteDto`
- `StockService`
- `StockController`

**Properties to add**

```properties
# application.properties
finnhub.api.key=

# application-local.properties
finnhub.api.key=YOUR_REAL_KEY

# application-test.properties
finnhub.api.key=test-key
```

---

## #4 — Auto Freeze/Close Scheduler

Automatically freezes and closes inactive accounts on a daily schedule. No manual trigger needed.

**Rules**
- `ACTIVE` account with no transactions in 60+ days — auto-frozen at 1:00 AM daily
- `FROZEN` account with no transactions in 60+ days — auto-closed at 2:00 AM daily
- Accounts with no transaction history at all are treated as inactive

**New files**
- `AccountSchedulerService`

**Modified files**
- `TransactionRepository` — added `findTopByBankAccount_IdOrderByTimestampDesc()`
- `BankAccountRepository` — added `findByStatus()`
- `BankingSystemApplication` — added `@EnableScheduling`

---

## #5 — Split User Update

Users can only update their own contact details. Name changes are restricted to admins only.

**Endpoints**

| Method | Endpoint | Access | Fields |
|---|---|---|---|
| PUT | `/api/users/{id}` | USER, ADMIN | phoneNumber, address |
| PUT | `/api/users/{id}/admin` | ADMIN only | firstName, lastName, phoneNumber, address |

**New files**
- `AdminUserUpdateRequestDto` — full fields including names

**Modified files**
- `UserUpdateRequestDto` — trimmed to phoneNumber and address only
- `UserService` — `updateUser()` trimmed, new `updateUserByAdmin()` added
- `UserController` — new `PUT /api/users/{id}/admin` endpoint added