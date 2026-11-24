# QAFlow Backend

Spring Boot 3.5 backend compiled for Java 17. Flyway owns the MySQL 8.0.42 baseline schema and Hibernate validates mapped entities without creating or mutating tables.

## Prerequisites

- JDK 17 for direct execution, or Docker for the reproducible JDK 17 build.
- Docker-compatible daemon for MySQL integration tests.
- Environment variables documented in the root `.env.example`.

The MySQL server must allow the migration account to create the approved triggers. In disposable local/test MySQL, start the server with `--log-bin-trust-function-creators=1`. Production must apply an equivalent DBA-reviewed policy; the application user must not receive `SUPER` merely to run migrations.

## Commands

From `backend/`:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

The local profile requires `DB_PASSWORD` and a Base64 JWT secret that decodes to at least 32 bytes in `JWT_SECRET_BASE64`. It defaults host, port, database and username to `localhost`, `3306`, `qaflow` and `qaflow` respectively.

Demo users are optional and disabled by default. Set `QAFLOW_BOOTSTRAP_ENABLED=true` plus the three password variables documented in `.env.example` only for a disposable local/demo database. Startup rejects this bootstrap when the `prod` profile is active.

Runtime endpoints:

- Health: `http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Login: `POST http://localhost:8080/api/v1/auth/login`
- Current user: `GET http://localhost:8080/api/v1/auth/me`
- User administration: `/api/v1/users` (`ADMIN` only)
- Products: `/api/v1/products` (`ADMIN` read/write, `OPERATOR` read)
- Materials: `/api/v1/materials` (`ADMIN` read/write, `OPERATOR` read)
- BOM revisions: `/api/v1/products/{productId}/boms` (`ADMIN` publish/read, `OPERATOR` read)

Spring Security uses stateless Bearer authentication, JWT HS256 with a 15-minute default lifetime, BCrypt password hashes and current-user/role/`authVersion` validation on every protected request. There are no refresh-token or backend logout endpoints in the MVP.

Material creation also creates its zero inventory balance in the same transaction. Catalog records are deactivated with `PATCH active=false`; there is no physical-delete endpoint. Material units become immutable after BOM, inventory movement or production-order material history exists.

The Fase 10 gate executed 26 unit/web tests and 22 MySQL integration tests: 48 passed with no failures, errors or skips. The runtime OpenAPI currently exposes 19 of the 39 designed operations. Inventory remains deferred until Fase 10 is approved.
