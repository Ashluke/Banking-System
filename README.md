# Banking System REST API

A fully featured banking REST API built with Spring Boot, featuring JWT authentication, role-based access control, audit logging, and comprehensive transaction management. Includes a Python analytics microservice for credit scoring, fraud detection, portfolio analysis, and savings predictions.

---

## Live Demo

> **Frontend:** `coming soon` — built with Vite + React *(in progress)*
>
> **API Docs (Swagger UI):** `coming soon`
> 
> **Base URL:** `coming soon`

---

## Features

- **JWT Authentication** — Stateless token-based auth with role enforcement
- **Role-Based Access Control** — `ADMIN` and `USER` roles with `@PreAuthorize` on every endpoint
- **User Management** — Register, login, update, and delete app users and user profiles
- **Admin Management** — Create and manage admin profiles with staff codes
- **Bank Accounts** — Create, freeze, unfreeze, and close accounts (max 3 per user)
- **Transactions** — Deposit, withdraw, and transfer with full history per account
- **Transaction Filtering** — Filter transaction history by type, date range, and amount
- **Account Filtering** — Filter accounts by status for both user and admin views
- **Transfer Tracking** — Cross-linked `TRANSFER_OUT` / `TRANSFER_IN` records with `relatedTransactionId`
- **Audit Logging** — Every admin action is logged with timestamp and target user
- **Audit Log Filtering** — Filter audit logs by action type and date range
- **Analytics Microservice** — Python/FastAPI service for transaction insights, credit scoring, fraud detection, portfolio analysis, and savings predictions
- **Global Exception Handling** — Typed exceptions mapped to proper HTTP status codes
- **Input Validation** — Jakarta validation on all request DTOs
- **Pagination** — All list endpoints support `?page=0&size=20`
- **API Documentation** — Swagger UI via SpringDoc OpenAPI

---

## Tech Stack

### Banking API (Java)

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

### Analytics Microservice (Python)

| Layer | Technology |
|---|---|
| Language | Python |
| Framework | FastAPI |
| Data Processing | Pandas, NumPy |
| Validation | Pydantic |

---

## Project Structure

### Banking API

```
src/
├── main/java/com/banking/system/
│   ├── controller/        # REST controllers
│   ├── services/          # Business logic
│   ├── repository/        # Spring Data JPA repositories
│   ├── specification/     # JPA Specifications for dynamic filtering
│   ├── model/
│   │   ├── entities/      # JPA entities
│   │   └── enums/         # Role, AccountStatus, TransactionType, ActionType
│   ├── dto/
│   │   ├── request/       # Request DTOs with validation
│   │   └── response/      # Response DTOs
│   ├── exception/         # Custom exceptions + GlobalExceptionHandler
│   ├── security/          # JWTService, JWTAuthFilter, SecurityConfig, SecurityUtil
│   ├── config/            # OpenAPI config
│   └── util/              # PasswordUtil, AuthorizationUtil
└── test/                  # Integration and unit tests
```

### Analytics Microservice

```
analytics/
├── models/                # Pydantic request/response models
├── routers/               # FastAPI route definitions
├── services/              # Business logic
└── tests/                 # Unit tests
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
| GET | `/api/accounts/users/{id}?status=` | ADMIN, USER | Get accounts by user, optionally filtered by status |
| GET | `/api/accounts?status=` | ADMIN | Get all accounts, optionally filtered by status |
| PUT | `/api/accounts/{id}/freeze` | ADMIN | Freeze account |
| PUT | `/api/accounts/{id}/unfreeze` | ADMIN | Unfreeze account |
| PUT | `/api/accounts/{id}/close` | ADMIN | Close account |

### Transactions
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/transactions/deposit` | USER | Deposit to account |
| POST | `/api/transactions/withdraw` | USER | Withdraw from account |
| POST | `/api/transactions/transfer` | USER | Transfer between accounts |
| GET | `/api/transactions/account/{id}?type=&from=&to=&minAmount=&maxAmount=` | ADMIN, USER (own) | Get transaction history with optional filters |

### Audit Logs
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/audit-logs` | ADMIN | Get all logs (paginated) |
| GET | `/api/audit-logs/{id}` | ADMIN | Get log by ID |
| GET | `/api/audit-logs/admin/{adminId}?action=&from=&to=` | ADMIN | Get logs by admin with optional filters |
| GET | `/api/audit-logs/user/{userId}` | ADMIN | Get logs by target user |

### Analytics Microservice

| Method | Endpoint | Description |
|---|---|---|
| POST | `/analytics/transactions/insights` | Transaction summary, monthly cash flow, and type breakdown |
| POST | `/analytics/transactions/trends` | Monthly savings trend and spending direction |
| POST | `/analytics/credit-score` | Credit score (400–850) with breakdown and loan eligibility |
| POST | `/analytics/detect` | Fraud detection with risk level and flagged transactions |
| POST | `/analytics/performance` | Portfolio gain/loss, diversification score, and risk level |
| POST | `/analytics/savings` | Savings forecast at 3, 6, and 12 months |

---

## Getting Started

### Prerequisites
- Java 17+
- MySQL 8
- Maven
- Python 3.10+ *(for analytics microservice)*

### Banking API Setup

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

### Analytics Microservice Setup

1. **Navigate to the analytics directory**
```bash
cd analytics
```

2. **Install dependencies**
```bash
pip install -r requirements.txt
```

3. **Run the service**
```bash
uvicorn main:app --reload
```

---

## Running Tests

### Prerequisites

The Banking API test suite uses an H2 in-memory database, so **MySQL is not required**.

However, the **Analytics Microservice must be running** before executing the Banking API tests because the loan application flow sends HTTP requests to it.

### 1. Start the Analytics Microservice

```bash
cd analytics
uvicorn main:app --reload
```

Leave this terminal running.

### 2. Run the Banking API Tests

Open a new terminal:

```bash
./mvnw test
```

---

## Running Analytics Microservice Tests

Open another terminal:

```bash
cd analytics
python -m pytest
```

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