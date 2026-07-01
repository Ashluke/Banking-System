# Added Features Summary — July 28, 2026

## #1 — Updated Files

**`JointAccountService.java`**
- Injected `AuditLogService`
- `addCoOwner()` gains `adminAppUserId` param — logs `ADD_CO_OWNER` against the co-owner's `appUserId` after saving
- `removeCoOwner()` gains `adminAppUserId` param — captures co-owner's `appUserId` before deleting, then logs `REMOVE_CO_OWNER`
- `getMembers()` gains `appUserId` and `isAdmin` params — non-members throw `UnauthorizedActionException`

**`JointAccountController.java`**
- `addCoOwner()` — pulls `SecurityUtil.getCurrentUserId()` and passes it to service
- `removeCoOwner()` — pulls `SecurityUtil.getCurrentUserId()` and passes it to service
- `getMembers()` — pulls `SecurityUtil.getCurrentUserId()` and `SecurityUtil.isAdmin()` and passes both to service

**`BankAccountService.java`**
- Constructor now injects `JointAccountMemberRepository`
- `createAccount()` — registers the creator as `PRIMARY` in `JointAccountMember` after saving the account
- `closeAccount()` — non-admin path now checks that the caller is the `PRIMARY` owner via `JointAccountMember`; co-owners cannot close
- `checkOwnershipOrAdmin()` — now queries `JointAccountMember` instead of comparing directly against `account.getUser()`; both primary and co-owner pass
- `mapToResponse()` — now includes `isJoint` field

---

## #2 — New Tests

**`JointAccountServiceTests`**

| Test | What it verifies |
|---|---|
| `addCoOwner_shouldAddMember_setJoint_andLogAction` | Happy path — saves member, sets `isJoint=true`, logs `ADD_CO_OWNER` |
| `addCoOwner_shouldThrowException_whenAccountNotFound` | 404 on missing account |
| `addCoOwner_shouldThrowException_whenAccountClosed` | Closed accounts reject co-owner |
| `addCoOwner_shouldThrowException_whenAlreadyHasCoOwner` | Enforces 2-member limit |
| `addCoOwner_shouldThrowException_whenUserNotFound` | 404 on missing user |
| `addCoOwner_shouldThrowException_whenUserAlreadyMember` | Duplicate member check |
| `addCoOwner_shouldThrowException_whenAddingPrimaryOwnerAsCoOwner` | Primary cannot be added as co-owner |
| `removeCoOwner_shouldDeleteMember_clearJoint_andLogAction` | Happy path — deletes member, sets `isJoint=false`, logs `REMOVE_CO_OWNER` |
| `removeCoOwner_shouldThrowException_whenAccountNotFound` | 404 on missing account |
| `removeCoOwner_shouldThrowException_whenCoOwnerNotFound` | 404 when no co-owner exists |
| `getMembers_shouldReturnMembers_whenAdmin` | Admin bypasses membership check |
| `getMembers_shouldReturnMembers_whenMember` | Member can view account members |
| `getMembers_shouldThrowException_whenNotMember` | Non-member gets 403 |
| `getMembers_shouldThrowException_whenAccountNotFound` | 404 on missing account |
| `isMember_shouldReturnTrue_whenUserIsMember` | Member check returns true |
| `isMember_shouldReturnFalse_whenUserIsNotMember` | Member check returns false |

**`JointAccountControllerTests`**

| Test | What it verifies |
|---|---|
| `addCoOwner_shouldReturn201_whenAdmin` | Admin gets 201 with CO_OWNER response |
| `addCoOwner_shouldReturn403_whenUserRole` | USER role is blocked |
| `addCoOwner_shouldReturn401_whenUnauthenticated` | No token gets 401 |
| `addCoOwner_shouldReturn400_whenCoOwnerUserIdMissing` | Validation fires on null `coOwnerUserId` |
| `addCoOwner_shouldReturn404_whenAccountNotFound` | 404 from service propagates |
| `addCoOwner_shouldReturn404_whenUserNotFound` | 404 from service propagates |
| `addCoOwner_shouldReturn409_whenAlreadyHasCoOwner` | 409 on duplicate co-owner |
| `addCoOwner_shouldReturn400_whenAccountClosed` | 400 on closed account |
| `removeCoOwner_shouldReturn204_whenAdmin` | Admin gets 204 |
| `removeCoOwner_shouldReturn403_whenUserRole` | USER role is blocked |
| `removeCoOwner_shouldReturn401_whenUnauthenticated` | No token gets 401 |
| `removeCoOwner_shouldReturn404_whenAccountNotFound` | 404 from service propagates |
| `removeCoOwner_shouldReturn404_whenCoOwnerNotFound` | 404 from service propagates |
| `getMembers_shouldReturn200_whenAdmin` | Admin sees all members |
| `getMembers_shouldReturn200_whenMember` | Member sees account members |
| `getMembers_shouldReturn403_whenNotMember` | Non-member gets 403 |
| `getMembers_shouldReturn404_whenAccountNotFound` | 404 from service propagates |
| `getMembers_shouldReturn401_whenUnauthenticated` | No token gets 401 |

**`BankAccountServiceTests`** — updated

| Test | What it verifies |
|---|---|
| `createBankAccount_shouldCreate_andRegisterPrimaryMember` | `jointAccountMemberRepository.save()` called once for PRIMARY after account creation |
| `createBankAccount_shouldThrowException_whenUserNotFound` | `jointAccountMemberRepository.save()` never called |
| `createBankAccount_shouldThrowException_whenAccountLimitExceeded` | `jointAccountMemberRepository.save()` never called |
| `getBankAccountById_shouldReturnAccount_whenPrimaryOwner` | Primary owner passes membership check |
| `getBankAccountById_shouldReturnAccount_whenCoOwner` | Co-owner also passes membership check |
| `getBankAccountById_shouldReturnAccount_whenAdmin` | Admin skips `findByBankAccount_Id` entirely |
| `getBankAccountById_shouldThrowException_whenNotMember` | Non-member throws `UnauthorizedActionException` |
| `closeAccount_shouldCloseAccount_andLogAction_whenAdmin` | Admin path skips PRIMARY check |
| `closeAccount_shouldCloseAccount_whenPrimaryOwner` | Primary owner can close; no audit log |
| `closeAccount_shouldThrowException_whenCoOwnerTriesToClose` | Co-owner cannot close the account |

**`BankAccountControllerTests`** — updated

| Test | What it verifies |
|---|---|
| `create_shouldReturn201_whenAdmin` | Response now includes `isJoint=false` |
| `getById_shouldReturn200_whenMember` | Renamed from `whenOwner` to reflect membership-based check |
| `getById_shouldReturn200_andShowIsJoint_whenJointAccount` | `isJoint=true` comes through in response |
| `getById_shouldReturn403_whenNotMember` | Error message updated to match new service message |

---

### General Notes

- All admin-only endpoints pass `adminAppUserId` from `SecurityUtil.getCurrentUserId()` for audit logging.
- `getMembers` access check reuses `isMember()` to avoid duplicating the stream logic.
- `BankAccountServiceTests` adds `@Mock JointAccountMemberRepository` and stubs it across all affected tests.
- `BankAccountControllerTests` uses a 5-arg `BankAccountResponseDto` constructor throughout; private helpers `accountResponse()` and `jointAccountResponse()` reduce repetition.