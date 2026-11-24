# Login Service: architecture

The Login Service is the first service of the Zodiac boutique platform. It answers two questions:
**who is this person?** and **are they still logged in?**

## What it owns, and what it does not

| Owns | Does not own (future services) |
|---|---|
| Accounts: email, password hash, role, status | Customer profile: name, addresses, preferences (Onboarding Service) |
| Login sessions (tokens) | Products, orders, payments |
| Lockout after repeated failed logins | Store branches and staff schedules |

Other services will refer to a person only by `user_account.id` (a UUID), never by copying credentials.

## Big picture

```mermaid
flowchart LR
    Browser["Browser"]
    Pages["Static pages<br/>frontend/ (port 5500)"]
    API["Login Service<br/>Spring Boot (port 8081)"]
    DB[("PostgreSQL<br/>login_db")]

    Browser -->|loads HTML, CSS, JS| Pages
    Browser -->|JSON over HTTP<br/>Authorization: Bearer token| API
    API -->|JDBC| DB
```

- The pages and the API are separate programs with separate addresses. The API accepts browser calls only from the origins listed in `zodiac.cors.allowed-origins` (CORS).
- The database belongs to this service alone. Other services must ask the API, never read the tables.

## Inside the service

```mermaid
flowchart TD
    C["controller/<br/>HTTP in, HTTP out"] --> S["service/<br/>business rules"]
    S --> R["repository/<br/>database access"]
    R --> E["entity/<br/>one object per table row"]
    C -.uses.-> D["dto/<br/>request and response shapes"]
    S -.uses.-> SEC["security/<br/>tokens and hashing"]
    X["exception/<br/>one JSON error shape"] -.catches errors from.-> C
    CFG["config/<br/>settings, beans, CORS, API docs"]
```

| Package | Responsibility | Rule |
|---|---|---|
| `controller` | Translate HTTP to Java and back | No business rules |
| `service` | Register, login, lockout, sessions, change password | Entities never leave this layer |
| `repository` | Read and write rows (Spring Data JPA) | Interfaces only |
| `entity` | Mirror the tables | State changes go through methods with meaning, no setters |
| `dto` | The exact JSON fields going in and out | Nothing internal is exposed |
| `security` | Token generation, hashing, header parsing | |
| `exception` | `ApiException`, `ErrorCode`, `GlobalExceptionHandler` | Every error has the same JSON shape |
| `config` | Beans, typed settings, CORS, OpenAPI | |

## Request flow: logging in

```mermaid
sequenceDiagram
    participant B as Browser
    participant C as AuthController
    participant A as AuthService
    participant R as Repositories
    participant DB as PostgreSQL

    B->>C: POST /api/v1/auth/login {email, password}
    C->>A: login(request)
    A->>R: find user by email
    R->>DB: SELECT ... WHERE email = ?
    A->>A: locked? password matches? account active?
    A->>R: save session (SHA-256 hash of new token)
    R->>DB: INSERT auth_session
    A-->>C: token + user
    C-->>B: 200 {accessToken, tokenType, expiresAt, user}
```

Later requests send the token in the `Authorization` header. The service hashes it, looks the hash up, and checks that the session is neither expired nor revoked.

## Data model

```mermaid
erDiagram
    user_account ||--o{ auth_session : "has"
    user_account {
        uuid id PK
        varchar email UK "lowercase"
        varchar phone UK "reserved"
        varchar password_hash "BCrypt"
        varchar role "CUSTOMER, STAFF, ADMIN"
        varchar status "ACTIVE, LOCKED, DISABLED"
        int failed_login_attempts
        timestamptz locked_until
        timestamptz last_login_at
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    auth_session {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK "SHA-256"
        timestamptz created_at
        timestamptz expires_at
        timestamptz revoked_at
        varchar ip_address
        varchar user_agent
    }
```

All schema changes are Liquibase changelogs in `src/main/resources/db/changelog/`. Hibernate only validates that the entities match the tables.

## Key decisions

| Decision | Why |
|---|---|
| Random opaque tokens stored as SHA-256 hashes | Simple, revocable on logout, and a copy of the database cannot be used to log in |
| BCrypt for passwords | Deliberately slow and salted; only the hash is stored |
| UUID primary keys | Safe to share with other services and not guessable |
| `VARCHAR` plus `CHECK` instead of PostgreSQL `ENUM` | Easier to evolve with a migration |
| One error JSON shape with a stable `code` | Frontends choose the wording, so translations need no backend change |
| `Clock` injected as a bean | Tests can move time forward (expiry, lockout) without waiting |
| Settings from environment variables | The same build runs in every environment |
| PostgreSQL 17.12 | Supported by Amazon Aurora PostgreSQL, so a later move is a connection change |
| API docs generated from code (springdoc) | The documentation cannot drift from the controllers |

## Environments and configuration

| Setting | Local | Other environments |
|---|---|---|
| Database | `application-local.yml` matches `docker-compose.yml` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Allowed web origins | `localhost:5500` | `ZODIAC_CORS_ALLOWED_ORIGINS` |
| API docs page | On | Off by default |
| Profile | `local` (set automatically by `bootRun`) | `SPRING_PROFILES_ACTIVE` |

The service refuses to start if the database settings are missing, instead of guessing. How it is built, packaged and checked on every push is described in [DEPLOYMENT.md](DEPLOYMENT.md).

## Testing strategy

| Layer | Tool | What it proves |
|---|---|---|
| Unit | JUnit 5, Mockito | Each business rule on its own, in milliseconds |
| Integration | Spring Boot test, MockMvc, Testcontainers PostgreSQL | Every endpoint against a real, throwaway database |
| Migrations | Testcontainers | The changelogs build the intended schema from scratch |

## Not in this service yet

Forgot-password and email verification, phone-number login, and a way for other services to validate tokens. They will be added as the neighbouring services appear.
