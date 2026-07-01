# Added Features Summary — July 1, 2026

## #1 — Repository Tests (the ones I completely forgot about)

Added `@DataJpaTest` integration tests for the 4 repositories that were missing test coverage. These run against an H2 in-memory database and test all custom query methods directly.

**Added files**
- `LoanRepositoryTests` — `findByUser_Id`, `findByStatus` (paged and list), `existsByUser_IdAndStatusIn`, default `PENDING` status on save
- `LoanRepaymentRepositoryTests` — ordering by installment number, status filtering, `findFirst` for next unpaid installment, `countByLoan_IdAndStatus`, overdue detection via `findByStatusAndDueDateBefore`, default `PENDING` status on save
- `JointAccountMemberRepositoryTests` — member listing, lookup by account and user, `exists` check, primary owner lookup by role, member count, duplicate member constraint
- `StockHoldingRepositoryTests` — list and paged `findByUser_Id`, `existsByUser_IdAndSymbol` including cross-user case, timestamp on save