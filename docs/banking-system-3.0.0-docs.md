# Added Features Summary — June 27, 2026

## #1 — Joint Account Support

Added joint account functionality allowing a bank account to have a primary owner and one co-owner. Co-owner management is admin-only to simulate users to visit the bank. Co-owners get the same access as the primary owner for viewing and transacting on the account.

**Endpoints**

```http
POST   /api/accounts/{accountId}/joint/invite
DELETE /api/accounts/{accountId}/joint/remove
GET    /api/accounts/{accountId}/joint/members
```

| Endpoint | Role | Description |
|---|---|---|
| `POST /invite` | ADMIN | Add a co-owner to an account |
| `DELETE /remove` | ADMIN | Remove the co-owner from an account |
| `GET /members` | USER or ADMIN | View all members of a joint account |

**Business rules**
- An account can have at most one co-owner (max 2 members total)
- The primary owner cannot be added as co-owner
- Co-owners cannot be added to a closed account
- When a co-owner is added, `isJoint` on the account is set to `true`
- When the co-owner is removed, `isJoint` is reset to `false`
- On account creation, the creator is automatically registered as the `PRIMARY` member in `JointAccountMember`
- Ownership checks now use `JointAccountMember` — both primary and co-owner pass the check

---

## #2 — New Files

**Enum**

`JointAccountRole.java`
- `PRIMARY` — the original account owner
- `CO_OWNER` — the added co-owner

**Entity**

`JointAccountMember.java`
| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key |
| `bankAccount` | `BankAccount` | `@ManyToOne` |
| `user` | `User` | `@ManyToOne` |
| `role` | `JointAccountRole` | PRIMARY or CO_OWNER |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp` |

**Repository**

`JointAccountMemberRepository.java`
| Method | Description |
|---|---|
| `findByBankAccount_Id` | All members of an account |
| `findByBankAccount_IdAndUser_Id` | Specific member lookup |
| `existsByBankAccount_IdAndUser_Id` | Membership check |
| `findByBankAccount_IdAndRole` | Find primary or co-owner by role |
| `countByBankAccount_Id` | Count members (used to enforce the 2-member limit) |

**DTOs**

`JointAccountInviteRequestDto.java`
- `coOwnerUserId` — `@NotNull`

`JointAccountMemberResponseDto.java`
- `id`, `bankAccountId`, `userId`, `firstName`, `lastName`, `role`, `joinedAt`

**Service**

`JointAccountService.java`
| Method | Description |
|---|---|
| `addCoOwner(accountId, request)` | Validates account, enforces limit, saves member, sets `isJoint = true` |
| `removeCoOwner(accountId)` | Deletes co-owner member, sets `isJoint = false` |
| `getMembers(accountId)` | Returns all members of the account |
| `isMember(accountId, appUserId)` | Used by `BankAccountService` for ownership checks |

**Controller**

`JointAccountController.java` — base path `/api/accounts/{accountId}/joint`

---

## #3 — Modified Files

**`ActionType.java`**
- Added `ADD_CO_OWNER`
- Added `REMOVE_CO_OWNER`

**`BankAccount.java`**
- Added `private boolean isJoint = false`

**`BankAccountResponseDto.java`**
- Added `private boolean isJoint`

**`BankAccountService.java`**
- Constructor now injects `JointAccountMemberRepository`
- `createAccount()` — after saving the account, registers the creator as `PRIMARY` in `JointAccountMember`
- `closeAccount()` — non-admin path now checks that the caller is the `PRIMARY` owner via `JointAccountMember`; co-owners cannot close the account
- `checkOwnershipOrAdmin()` — now queries `JointAccountMember` instead of comparing directly against `account.getUser()`, so both primary and co-owner pass the check
- `mapToResponse()` — now includes `isJoint` field

---

### General Notes

- Joint account management is intentionally admin-only to simulate a branch visit workflow.
- Co-owner access is equal to primary owner access for deposits, withdrawals, transfers, and account viewing.
- Only the primary owner (or an admin) can close a joint account.
- The `JointAccountMember` table also serves as the source of truth for ownership checks on all accounts, not just joint ones — every account has at least a `PRIMARY` entry created at account creation time.