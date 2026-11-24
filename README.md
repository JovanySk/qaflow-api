# QAFlow API — Manufacturing Backend (Spring Boot)

Backend REST API for a small manufacturing workflow (products, materials, versioned bills of materials and transactional inventory), built **test-first** to show backend, QA automation and security practices end to end.

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen) ![MySQL](https://img.shields.io/badge/MySQL-8.0-blue) [![CI](https://github.com/JovanySk/qaflow-api/actions/workflows/ci.yml/badge.svg)](https://github.com/JovanySk/qaflow-api/actions/workflows/ci.yml) ![Tests](https://img.shields.io/badge/tests-65%20passing-success) ![Testcontainers](https://img.shields.io/badge/Testcontainers-MySQL-informational)

## Highlights

| | |
|---|---|
| **24 REST endpoints** under `/api/v1` | auth, users, products, materials, BOM, inventory |
| **65 automated tests, all green in CI** | 31 integration tests against real MySQL 8 (Testcontainers) + 34 unit/web tests (JUnit 5, Mockito, MockMvc) |
| **98 designed test cases** | traceable to requirements in [`TEST_MATRIX.md`](TEST_MATRIX.md) |
| **12 tables** | one Flyway migration, Hibernate in `validate` mode (never mutates the schema) |
| **Security** | stateless JWT (HS256, 15 min), BCrypt, 3 roles, token invalidation via `authVersion` |

## Architecture

```text
Client ──► Spring Security (JWT filter, role checks)
             │
             ▼
        Controller  ──►  Service (business rules, @Transactional)  ──►  Repository (Spring Data JPA)
             │                     │                                         │
       DTO validation       audit log + pessimistic locks                MySQL 8 + Flyway
```

Modules are packaged by feature (`auth`, `user`, `product`, `material`, `bom`, `inventory`, `shared`), each with `api / application / domain / persistence` layers.

## What it does

- **Auth & users:** login returns a JWT. Changing a role, password or status bumps `authVersion`, which invalidates tokens already issued. Errors come back as consistent `401`/`403` responses. User administration is ADMIN-only.
- **Catalog:** products and materials with normalized uniqueness, optimistic versioning and logical deactivation (`PATCH active=false`, no physical deletes). A zero inventory balance is created in the same transaction as each material.
- **Versioned BOM:** published BOM revisions are immutable and append-only. Quantities are exact decimals (`DECIMAL(19,6)`, sent as strings). Materials are locked in a stable order so concurrent publications produce a controlled `409` conflict instead of a deadlock.
- **Inventory ledger:** stock entries, outputs and compensating adjustments update the balance, the movement and the audit row in one transaction. It rejects invalid quantities, inactive materials, insufficient stock and overflow. Permissions per role: OPERATOR writes, ADMIN reads, QUALITY_INSPECTOR is denied.

## Testing

```text
src/test/java
├── *IT.java    → integration: real HTTP + real MySQL in a container (Testcontainers)
└── *Test.java  → unit/web: services, domain rules, JWT validator, error handler
```

Every push runs the full suite on GitHub Actions: **34 unit/web tests and 31 integration tests pass, with 0 failures**. Integration suites include InventoryApiIT, CatalogApiIT, BomApiIT, SecurityApiIT and BackendInfrastructureIT.

Also included:
- **Postman collection** with 12 requests ([`tests/postman`](tests/postman)). It uses an environment file with no secrets.
- **SQL integrity checks** ([`tests/sql/inventory-validation.sql`](tests/sql/inventory-validation.sql), [`docs/database/phase-3-validation.sql`](docs/database/phase-3-validation.sql)).

## Run it

Requirements: JDK 17 and Docker (for MySQL and Testcontainers).

```powershell
cd backend
.\mvnw.cmd clean verify          # unit + integration tests (starts MySQL in a container)
.\mvnw.cmd spring-boot:run       # needs the variables from .env.example
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Copy `.env.example`, set `DB_PASSWORD` and a Base64 `JWT_SECRET_BASE64` (32 bytes or more). Real secrets are never committed.

## Documentation

The design was written before the code, phase by phase, in [`docs/`](docs):
- requirements
- architecture
- data model
- API contract
- test strategy
- test catalog
- security
- BOM

Architecture decisions are recorded in [`docs/DECISIONS.md`](docs/DECISIONS.md).

## Roadmap

Production orders and quality inspection (designed; tables exist), then a React frontend, Cypress E2E, a Newman report, Docker Compose and a CI pipeline. See [`ROADMAP.md`](ROADMAP.md).

---
Author: **Jovany Morales del Ángel** — Computer Systems Engineering student.
