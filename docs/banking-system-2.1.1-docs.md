# Test Fixes Summary — June 25, 2026

## #1 — Admin Tests

Updated `AdminServiceTests` and `AdminControllerTests` to reflect the new combined `AppUser + Admin` creation flow and updated DTOs.

**What changed**
- `createAdmin()` now creates both `AppUser` and `Admin` internally — tests mock `appUserRepository.existsByEmail` and `appUserRepository.save` instead of assuming a pre-existing `AppUser`
- Return type changed from `AdminResponseDto` to `AdminRegisterResponseDto` — assertions updated to check `email`, `role`, and `createdAt`
- Registration endpoint moved from `POST /api/admins` to `POST /api/admins/register`

**Modified test files**
| File | What was fixed |
|---|---|
| `AdminServiceTests` | Added `appUserRepository` mock; updated `createAdmin` to use new DTO and assert `AdminRegisterResponseDto` |
| `AdminControllerTests` | POST tests updated to hit `/api/admins/register`; request and response assertions updated |

---

## #2 — User Tests

Updated `UserServiceTests` and `UserControllerTests` to reflect the combined `AppUser + User` creation flow, trimmed self-service update, and new admin-only update endpoint.

**What changed**
- `createUser()` replaced by `createCustomer()` — takes `UserCreateRequestDto(email, password, firstName, lastName, phoneNumber, address)` directly with no pre-existing `AppUser`
- `updateUser()` now only accepts `phoneNumber` and `address` — name field assertions removed from self-service tests
- New `updateUserByAdmin()` added — allows all fields including names; tested separately
- Registration endpoint moved from `POST /api/users` to `POST /api/users/register`
- New endpoint `PUT /api/users/{id}/admin` added to controller tests

**Modified test files**
| File | What was fixed |
|---|---|
| `UserServiceTests` | `createCustomer()` replaces `createUser()`; `updateUser` assertions trimmed to phone and address only; new `updateUserByAdmin` test group |
| `UserControllerTests` | POST updated to `/api/users/register`; new `updateByAdmin` test group for `PUT /{id}/admin` with `AdminUserUpdateRequestDto` |

---

## #3 — Transaction Tests

Updated `TransactionServiceTests` and `TransactionControllerTests` to pass the new filter parameters on `getByAccountId()`.

**What changed**
- `getByAccountId()` signature expanded with `TransactionType type`, `LocalDateTime from`, `LocalDateTime to`, `BigDecimal minAmount`, `BigDecimal maxAmount`
- Service now delegates to `transactionRepository.findAll(spec, pageable)` — tests use `any(Specification.class)`
- Controller `GET /account/{accountId}` exposes all filters as optional query params

**Modified test files**
| File | What was fixed |
|---|---|
| `TransactionServiceTests` | All `getByAccountId()` calls updated with new params; added tests for type, date range, and amount range filters |
| `TransactionControllerTests` | GET tests updated; added tests for `type`, `from`/`to`, and `minAmount`/`maxAmount` query params |

---

## #4 — Bank Account Tests

Updated `BankAccountServiceTests` and `BankAccountControllerTests` for the `status` filter on `getByUserId()` and the new `getAllAccounts()` method.

**What changed**
- `getByUserId()` gains optional `AccountStatus status` param — tests pass `null` for no filter and a specific status for filtered cases
- New `getAllAccounts(AccountStatus status, Pageable)` method — tested with and without a status filter
- New admin `GET /api/accounts` endpoint tested in controller tests
- Both methods delegate to `bankAccountRepository.findAll(spec, pageable)` — tests use `any(Specification.class)`

**Modified test files**
| File | What was fixed |
|---|---|
| `BankAccountServiceTests` | `getByUserId()` calls updated with status param; new `getAllAccounts` test group |
| `BankAccountControllerTests` | `GET /users/{id}` updated with optional `status` param; new `GET /` test group for admin listing |

---

## #5 — Audit Log Tests

Updated `AuditLogServiceTests` and `AuditLogControllerTests` to pass the new filter parameters on `getByAdminId()`.

**What changed**
- `getByAdminId()` signature expanded with `ActionType action`, `LocalDateTime from`, `LocalDateTime to`
- Service now delegates to `auditLogRepository.findAll(spec, pageable)` — tests use `any(Specification.class)`
- Controller `GET /audit-logs/admin/{adminId}` exposes all filters as optional query params

**Modified test files**
| File | What was fixed |
|---|---|
| `AuditLogServiceTests` | All `getByAdminId()` calls updated with new params; added tests for action type and date range filters |
| `AuditLogControllerTests` | GET tests updated; added tests for `action`, `from`, and `to` query params |

---

## #6 — Integration Tests

Updated `BankingSystemIntegrationTests` to match the combined creation flows and new endpoint paths.

**What changed**
- Removed the old two-step flow (`POST /api/app-users/register` → `POST /api/users`) — replaced with single `POST /api/users/register`
- `UserCreateRequestDto` constructor updated to `(email, password, firstName, lastName, phoneNumber, address)`
- Added `seedAdmin()` helper — seeds an admin directly via the repository to avoid the chicken-and-egg problem
- Added `loginAndGetToken()` helper — reduces repetition across test methods
- Added `fullFlow_registerAdmin_shouldSucceedEndToEnd` test
- Added `registerAdmin_shouldReturn403_whenCallerIsNotAdmin` test
- Added `registerUser_shouldReturn409_whenEmailAlreadyTaken` test

**Modified test files**
| File | What was fixed |
|---|---|
| `BankingSystemIntegrationTests` | Flow updated to combined creation; helpers extracted; three new test cases added |

---

### General Notes

- All filter-related tests use `any(Specification.class)` — no brittle predicate assertions.
- Controller tests use `isNull()` for absent optional params to verify the correct service call signature.
- `@MockBean` replaced with `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`) across all controller tests.
- `ObjectMapper` instantiated directly in controller tests (`new ObjectMapper().findAndRegisterModules()`) since it is not autowireable in `@WebMvcTest`.