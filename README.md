# Banking System REST API

A fully featured banking REST API built with Spring Boot, featuring JWT authentication, role-based access control, audit logging, and comprehensive transaction management.

---

## Live Demo

> **Frontend:** `coming soon`
>
> **API Docs (Swagger UI):** `coming soon`
> 
> **Base URL:** `coming soon` — built with Vite + React *(in progress)*

---

## Features

- **JWT Authentication** — Stateless token-based auth with role enforcement
- **Role-Based Access Control** — `ADMIN` and `USER` roles with `@PreAuthorize` on every endpoint
- **User Management** — Register, login, update, and delete app users and user profiles
- **Admin Management** — Create and manage admin profiles with staff codes
- **Bank Accounts** — Create, freeze, unfreeze, and close accounts (max 3 per user)
- **Transactions** — Deposit, withdraw, and transfer with full history per account
- **Transfer Tracking** — Cross-linked `TRANSFER_OUT` / `TRANSFER_IN` records with `relatedTransactionId`
- **Audit Logging** — Every admin action is logged with timestamp and target user
- **Global Exception Handling** — Typed exceptions mapped to proper HTTP status codes
- **Input Validation** — Jakarta validation on all request DTOs
- **Pagination** — All list endpoints support `?page=0&size=20`
- **API Documentation** — Swagger UI via SpringDoc OpenAPI

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security + JWT (JJWT) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5, Mockito, MockMvc, H2 |
| Build | Maven |
| Frontend | Vite + React *(in progress)* |

---

## Project Structure

```
src/
├── main/java/com/banking/system/
│   ├── controller/        # REST controllers
│   ├── services/          # Business logic
│   ├── repository/        # Spring Data JPA repositories
│   ├── model/
│   │   ├── entities/      # JPA entities
│   │   └── enums/         # Role, AccountStatus, TransactionType, ActionType
│   ├── dto/
│   │   ├── request/       # Request DTOs with validation
│   │   └── response/      # Response DTOs
│   ├── exception/         # Custom exceptions + GlobalExceptionHandler
│   ├── security/          # JWTService, JWTAuthFilter, SecurityConfig, SecurityUtil
│   ├── config/            # OpenAPI config
│   └── util/              # PasswordUtil
└── test/                  # Integration and unit tests
```

---

## API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/app-users/login` | Public | Login and receive JWT |

### App Users
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/app-users/register` | ADMIN | Register a new user |
| GET | `/api/app-users/{id}` | ADMIN | Get user by ID |
| GET | `/api/app-users` | ADMIN | Get all users (paginated) |
| PUT | `/api/app-users/{id}` | ADMIN, USER (own) | Update user |
| DELETE | `/api/app-users/{id}` | ADMIN | Delete user |

### User Profiles
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/users` | ADMIN | Create user profile |
| GET | `/api/users/{id}` | ADMIN | Get user profile by ID |
| GET | `/api/users` | ADMIN | Get all profiles (paginated) |
| PUT | `/api/users/{id}` | ADMIN, USER (own) | Update profile |
| DELETE | `/api/users/{id}` | ADMIN | Delete profile |

### Admins
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/admins` | ADMIN | Create admin profile |
| GET | `/api/admins/{id}` | ADMIN | Get admin by ID |
| GET | `/api/admins` | ADMIN | Get all admins (paginated) |
| PUT | `/api/admins/{id}` | ADMIN | Update admin |
| DELETE | `/api/admins/{id}` | ADMIN | Delete admin |

### Bank Accounts
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/accounts` | ADMIN | Create bank account |
| GET | `/api/accounts/{id}` | ADMIN, USER (own) | Get account by ID |
| GET | `/api/accounts/users/{id}` | ADMIN, USER | Get accounts by user |
| PUT | `/api/accounts/{id}/freeze` | ADMIN | Freeze account |
| PUT | `/api/accounts/{id}/unfreeze` | ADMIN | Unfreeze account |
| PUT | `/api/accounts/{id}/close` | ADMIN | Close account |

### Transactions
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/transactions/deposit` | USER | Deposit to account |
| POST | `/api/transactions/withdraw` | USER | Withdraw from account |
| POST | `/api/transactions/transfer` | USER | Transfer between accounts |
| GET | `/api/transactions/account/{id}` | ADMIN, USER (own) | Get transaction history |

### Audit Logs
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/audit-logs` | ADMIN | Get all logs (paginated) |
| GET | `/api/audit-logs/{id}` | ADMIN | Get log by ID |
| GET | `/api/audit-logs/admin/{adminId}` | ADMIN | Get logs by admin |
| GET | `/api/audit-logs/user/{userId}` | ADMIN | Get logs by target user |

---

## Getting Started

### Prerequisites
- Java 17+
- MySQL 8
- Maven

### Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/banking-system.git
cd banking-system
```

2. **Create the database**
```sql
CREATE DATABASE banking_system;
```

3. **Configure your local properties**

Create `src/main/resources/application-local.properties`:
```properties
spring.datasource.password=yourpassword
jwt.secret=yoursecretkeyatleast32characterslong
```

4. **Run the application**
```bash
./mvnw spring-boot:run
```

5. **Access Swagger UI**
```
http://localhost:8080/swagger-ui/index.html
```

---

## Running Tests

```bash
./mvnw test
```

Tests use an H2 in-memory database and do not require MySQL to be running.

---

## Authentication

All endpoints except `/api/app-users/login` and the Swagger UI require a valid JWT in the `Authorization` header:

```
Authorization: Bearer <your-token>
```

To get a token, call the login endpoint with valid credentials. The token expires after **10 hours**.

---

## First Time Setup

Since registering a user requires an admin, you'll need to seed one directly into the database:

```sql
INSERT INTO app_user (email, password_hash, role, created_at)
VALUES ('admin@example.com', '<bcrypt-hash>', 'ADMIN', NOW());

INSERT INTO admins (app_user_id, staff_code, first_name, last_name)
VALUES (1, 'ADMIN-001', 'Super', 'Admin');
```

Then log in with that admin to register other users through the API.

---

## License

This project is open source and available under the [MIT License](LICENSE).