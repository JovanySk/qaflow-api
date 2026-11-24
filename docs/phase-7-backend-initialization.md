# FASE 7 — INICIALIZACIÓN DEL BACKEND

## 1. Nombre de la fase

**Fase 7 — Inicialización verificable del backend Spring Boot.**

### Inicio obligatorio de continuidad

#### Resumen del estado anterior

- Las Fases 0–6 están completadas y aprobadas.
- Fase 6 definió un monorepo, backend feature-first, Flyway como autoridad y Git limitado a `D:\qaflow`.
- Existían 19 archivos de diseño/control; no existían código, migraciones runtime, builds o pruebas funcionales.
- Los 98 casos de Fase 5 permanecían `NOT_RUN`.

#### Decisiones que siguen vigentes

- Java 17, Spring Boot, JPA, MySQL, Flyway, Springdoc y Maven Wrapper.
- Monolito modular con paquetes por funcionalidad y transacciones locales.
- DTO/error público separado de persistencia y correlación HTTP uniforme.
- MySQL real para integración; H2 y mocks no sustituyen integración.
- Auditoría persistente append-only separada de logs.
- Secretos, builds y reports generados quedan fuera de Git.

#### Archivos existentes utilizados

- `docs/phase-2-architecture.md` para módulos y capas.
- `docs/phase-3-data-model.md` y `docs/database/phase-3-initial-schema.sql` para Flyway/JPA.
- `docs/phase-4-api-contract.md` y `docs/api/openapi.json` para Swagger/errores/correlación.
- `docs/phase-5-test-strategy.md` y catálogo para clasificación/evidencia.
- `docs/phase-6-repository-structure.md` para ubicaciones y Git.
- Los siete documentos de continuidad.

#### Objetivo de la fase actual

Materializar una base Spring Boot ejecutable con configuración externa, perfiles, MySQL/Flyway, JPA validate, error boundary, OpenAPI, auditoría y pruebas técnicas, y demostrar compilación, inicio, conexión y Swagger con evidencia real bajo Java 17.

#### Criterios para considerar terminada la fase

1. Git inicializado y verificado exactamente en `D:\qaflow`, sin staging/commit automático.
2. Spring Boot compila para y se ejecuta con Java 17.
3. Maven Wrapper resuelve la versión fijada.
4. Perfiles local/test/prod y variables están documentados sin secretos.
5. Flyway aplica V1 contra MySQL y Hibernate valida el mapeo existente.
6. Errores y correlación tienen pruebas web.
7. Auditoría JSON e inmutabilidad tienen integración MySQL.
8. Health, OpenAPI 3.1 y Swagger responden desde una aplicación iniciada.
9. Reports/target no quedan versionados y los recursos temporales se limpian.
10. Documentos de continuidad y manifiesto quedan actualizados.
11. El usuario aprueba avanzar.

## 2. Objetivo

Crear el cimiento backend mínimo, real y defendible sobre el que Fases 8–13 implementarán seguridad y módulos de negocio, sin anticipar endpoints o pruebas de funcionalidades inexistentes.

## 3. Dependencias

- Fase 6 aprobada.
- Docker Desktop/daemon para el runtime Java 17 reproducible y Testcontainers.
- Acceso a Maven Central y registro de imágenes durante la primera ejecución.
- SQL/modelo y OpenAPI aprobados.
- Puerto local libre para el smoke manual; se usó `18087` de forma temporal.

## 4. Entradas requeridas

Inventario real de este host:

| Herramienta | Resultado |
|---|---|
| Host Java/Javac | `21.0.2`; no se usa como evidencia Java 17 |
| Maven global | No instalado |
| Git | `2.54.0.windows.1` |
| Docker client/server | `29.4.0`, Linux `x86_64` mediante Docker Desktop |
| Docker Compose | `v5.1.2`; no usado como orquestador en esta fase |
| Node/npm | `22.16.0` / `10.9.2`; no usados en backend |

Versiones seleccionadas:

- Spring Boot `3.5.13` y Java 17. Spring Boot requiere Java 17 o superior y Maven 3.6.3 o superior según la [documentación oficial](https://docs.spring.io/spring-boot/installing.html).
- springdoc `2.8.17`, cuya matriz oficial asigna `2.8.x` a Boot `3.5.x`: [springdoc](https://springdoc.org/).
- Maven Wrapper `3.9.11`.
- MySQL `8.0.42`, compatible con el mínimo de modelo `8.0.16+`.

## 5. Entregables

1. Archivos raíz seguros y repositorio Git `main`.
2. POM Spring Boot y Maven Wrapper.
3. Aplicación Java 17 y configuración `local/test/prod`.
4. Migración Flyway V1 exacta al SQL aprobado.
5. Actuator health/info y OpenAPI 3.1/Swagger UI.
6. Filtro `X-Correlation-Id` y errores JSON sanitizados.
7. Puerto de auditoría e implementación JPA append-only.
8. Cuatro pruebas web y dos integraciones MySQL.
9. JAR ejecutable y smoke manual Java 17/MySQL.
10. Evidencia, decisiones, riesgos, matriz y manifiesto actualizados.

## 6. Decisiones tomadas

Se proponen D-0067 a D-0075, pendientes de aprobación de esta fase:

- Boot 3.5.13, Java 17, springdoc 2.8.17, Maven 3.9.11 y MySQL 8.0.42.
- Perfiles externos sin secretos y Swagger deshabilitado por defecto en prod.
- V1 idéntica por SHA-256 al diseño histórico; Flyway migra y JPA valida.
- Correlación en filtro y error boundary uniforme.
- Auditoría mediante puerto y transacciones normal/aislada.
- Surefire para `*Test` y Failsafe para `*IT`.
- Build reproducible en Docker Java 17 debido a host Java 21 sin Maven.
- `log-bin-trust-function-creators=1` solo como política local/test/DBA para triggers.
- Seguridad/JWT y endpoints de negocio diferidos a sus fases propietarias.

## 7. Diseño o código

### 7.1 Estructura materializada

```text
backend/
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/qaflow/
    │   │   ├── QaFlowApplication.java
    │   │   ├── audit/{application,domain,infrastructure}/
    │   │   └── shared/{config,error,web}/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-prod.yml
    │       └── db/migration/V1__initial_schema.sql
    └── test/
        ├── java/com/qaflow/{integration,shared/error}/
        └── resources/application-test.yml
```

No se crearon paquetes vacíos para auth/product/material/etc.; aparecen con su primera implementación real.

### 7.2 Dependencias y build

| Área | Dependencia/configuración |
|---|---|
| REST/validación | Spring Web + Validation |
| Persistencia | Data JPA + MySQL Connector/J |
| Schema | Flyway Core + Flyway MySQL |
| Operación | Actuator health/info |
| Contrato vivo | springdoc WebMVC UI, OpenAPI 3.1 |
| Unit/web | Spring Boot Test/JUnit 5/Mockito/MockMvc |
| Integración | Testcontainers JUnit + MySQL |
| Ciclo | Surefire `*Test`; Failsafe `*IT` en `verify` |

Spring Security no se añadió anticipadamente; Fase 8 debe configurarlo completo y probar `401/403`.

### 7.3 Perfiles y variables

| Perfil | Fuente DB | Swagger | Propósito |
|---|---|---|---|
| `local` | `DB_HOST/PORT/NAME/USERNAME/PASSWORD` | Activo por default configurable | Desarrollo/smoke |
| `test` | Dynamic properties de Testcontainers | Activo | Integración efímera |
| `prod` | `DB_URL/USERNAME/PASSWORD`, pool externo | Inactivo por default | Configuración endurecida futura |

- `.env.example` no contiene una contraseña válida.
- Spring no carga `.env` automáticamente; el usuario/CI inyecta variables.
- El perfil local exige `DB_PASSWORD`; no hay secreto default.
- JSON desconocido falla mediante Jackson `fail-on-unknown-properties`.

### 7.4 Flujo de petición y error

```text
Request
  -> CorrelationIdFilter valida/genera ID y lo coloca en header + MDC
  -> Controller/validation (cuando exista endpoint)
  -> GlobalExceptionHandler clasifica 400/404/405/409/415/500
  -> ApiErrorResponseFactory crea JSON consistente
  -> Response conserva X-Correlation-Id
```

Un `500` devuelve mensaje genérico. El log conserva correlación y tipo de excepción, no el mensaje potencialmente sensible.

### 7.5 Auditoría

- `AuditEventRecorder` es el puerto que consumirán módulos.
- `recordWithinBusinessTransaction` se une a la transacción activa o crea una si se usa aisladamente.
- `recordInIsolatedTransaction` usa `REQUIRES_NEW` para rechazos/fallos que deben sobrevivir rollback.
- La entidad mapea `summary` como MySQL JSON.
- La integración inserta, consulta JSON y confirma que el trigger rechaza `UPDATE`.

### 7.6 Migración y schema

- `V1__initial_schema.sql` tiene SHA-256 `0981995FB2BB949581B1C3F00003553556920140B42EAC5D5BF16388A1B5E49C`.
- El hash es idéntico al SQL histórico de Fase 3.
- Flyway creó 12 tablas de dominio, una fila de migración exitosa y 10 triggers.
- Hibernate `ddl-auto=validate` validó el mapeo de auditoría existente.
- MySQL emitió cuatro warnings deprecatorios por `TINYINT(1)`; no alteran ejecución y se registran como riesgo de migración futura.

### 7.7 Pruebas implementadas

| Suite | Casos | Resultado final |
|---|---:|---|
| `GlobalExceptionHandlerTest` | 4 | PASS: validación, 404 de aplicación, correlación inválida, 500 sanitizado |
| `BackendInfrastructureIT` | 2 | PASS: startup/DB/Flyway/health/OpenAPI y auditoría JSON/inmutabilidad |

Estas seis pruebas son técnicas de Fase 7. No convierten los 98 casos funcionales diseñados en PASS.

### 7.8 Fallos reales detectados y corregidos

1. PowerShell fragmentó `-Dmaven=3.9.11`; el primer wrapper apuntó a Maven 3 y el goal falló. Se citó el argumento y se regeneró correctamente.
2. Un controller anidado de test no entró en `@WebMvcTest`; 3/4 casos devolvieron 404. Se movió a `src/test` top-level y quedó 4/4.
3. El handler agregaba dos valores iguales de correlación. Se dejó al filtro como única autoridad del header.
4. MySQL devolvió error 1419 al crear triggers con binlog. Se configuró confianza de creadores para el servidor efímero sin conceder `SUPER`.
5. MySQL 8.4 produjo warning de soporte Flyway no probado. El baseline se alineó a MySQL 8.0.42 y se repitió el gate.
6. Windows PowerShell presentó Actuator como bytes por su media type. El smoke final decodificó UTF-8 antes de afirmar `UP`.

### 7.9 Evidencia de ejecución final

`mvn clean verify` dentro de `maven:3.9.11-eclipse-temurin-17`:

- Compilación: 18 clases main, release 17.
- Tests: 4 unit/web PASS; 2 integración PASS; 0 failures/errors/skips.
- JAR: `target/qaflow-backend-0.1.0-SNAPSHOT.jar`.
- Runtime integración: Java `17.0.17`, MySQL `8.0.42`.
- Flyway: una migración aplicada a v1.
- Tomcat: iniciado y detenido por la suite.

Smoke manual temporal:

| Verificación | Resultado |
|---|---|
| Java del proceso | OpenJDK `17.0.17` |
| MySQL | `8.0.42` |
| `/actuator/health` | HTTP 200, `UP`, liveness/readiness |
| Correlación health | `phase7-manual-health-final` reflejada |
| `/v3/api-docs` | HTTP 200, OpenAPI `3.1.0`, título correcto |
| `/swagger-ui.html` | HTTP 200 tras redirect a `/swagger-ui/index.html` |
| DB | 12 tablas, 1 migración exitosa, 10 triggers |

Los contenedores/red etiquetados de smoke fueron detenidos y eliminados; no queda servicio de Fase 7 ejecutándose.

### 7.10 Exclusiones conscientes

- Sin usuarios, roles, JWT o Spring Security: Fase 8.
- Sin endpoints de productos/materiales u otros módulos: Fases 9–13.
- Sin frontend, Postman/Newman ni Cypress.
- Sin Dockerfile/Compose: Fase 20; el uso temporal de contenedores no adelanta ese entregable.
- Sin GitLab CI.
- Sin commit, staging o remote Git.

## 8. Archivos creados

### Raíz

- `.gitignore`
- `.gitattributes`
- `.editorconfig`
- `.env.example`
- `README.md`

### Backend y wrapper

- `backend/pom.xml`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/.mvn/wrapper/maven-wrapper.properties`
- `backend/README.md`

### Producción

- `backend/src/main/java/com/qaflow/QaFlowApplication.java`
- `backend/src/main/java/com/qaflow/shared/config/ClockConfiguration.java`
- `backend/src/main/java/com/qaflow/shared/config/OpenApiConfiguration.java`
- `backend/src/main/java/com/qaflow/shared/web/CorrelationIdFilter.java`
- `backend/src/main/java/com/qaflow/shared/error/ApiFieldError.java`
- `backend/src/main/java/com/qaflow/shared/error/ApiErrorResponse.java`
- `backend/src/main/java/com/qaflow/shared/error/ApiErrorResponseFactory.java`
- `backend/src/main/java/com/qaflow/shared/error/ApplicationException.java`
- `backend/src/main/java/com/qaflow/shared/error/BadRequestException.java`
- `backend/src/main/java/com/qaflow/shared/error/ResourceNotFoundException.java`
- `backend/src/main/java/com/qaflow/shared/error/BusinessConflictException.java`
- `backend/src/main/java/com/qaflow/shared/error/GlobalExceptionHandler.java`
- `backend/src/main/java/com/qaflow/audit/domain/AuditOutcome.java`
- `backend/src/main/java/com/qaflow/audit/application/AuditEventCommand.java`
- `backend/src/main/java/com/qaflow/audit/application/AuditEventRecorder.java`
- `backend/src/main/java/com/qaflow/audit/infrastructure/AuditEventEntity.java`
- `backend/src/main/java/com/qaflow/audit/infrastructure/SpringDataAuditEventRepository.java`
- `backend/src/main/java/com/qaflow/audit/infrastructure/JpaAuditEventRecorder.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/main/resources/db/migration/V1__initial_schema.sql`

### Pruebas

- `backend/src/test/java/com/qaflow/shared/error/ErrorProbeController.java`
- `backend/src/test/java/com/qaflow/shared/error/GlobalExceptionHandlerTest.java`
- `backend/src/test/java/com/qaflow/integration/BackendInfrastructureIT.java`
- `backend/src/test/resources/application-test.yml`

### Documentación

- `docs/phase-7-backend-initialization.md`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-6-repository-structure.md` (registro de aprobación)

## 10. Comandos

Principales comandos ejecutados:

```powershell
git init -b main
git rev-parse --show-toplevel

# Ejecutados dentro del contenedor Maven Java 17
./mvnw -v
mvn -B -ntp clean test
mvn -B -ntp clean verify

# Verificación HTTP del smoke temporal
Invoke-WebRequest http://localhost:18087/actuator/health
Invoke-WebRequest http://localhost:18087/v3/api-docs
Invoke-WebRequest http://localhost:18087/swagger-ui.html
```

También se iniciaron y eliminaron contenedores temporales etiquetados de MySQL/backend. No se ejecutó `git add`, `git commit`, Docker Compose o pipeline.

## 11. Cómo verificar

1. Usar JDK 17 directo o el contenedor Maven Java 17.
2. Iniciar Docker.
3. Desde `backend/`, ejecutar `./mvnw clean verify` (`mvnw.cmd` en Windows con JDK 17).
4. Confirmar resumen 4 Surefire + 2 Failsafe, sin failures/errors.
5. Para local, iniciar MySQL 8.0.42 con política de triggers y exportar variables de `.env.example`.
6. Ejecutar el JAR o `spring-boot:run`.
7. Consultar health, OpenAPI y Swagger.
8. Confirmar `flyway_schema_history`, 12 tablas y 10 triggers.
9. Confirmar que `backend/target`, `.env` y reports están ignorados.

## 12. Pruebas relacionadas

- TM-012: error boundary/correlación, ejecución técnica parcial.
- TM-013: auditoría JSON/inmutabilidad, ejecución técnica parcial.
- TM-015: smoke backend+DB parcial; frontend/Compose siguen pendientes.
- TM-017: OpenAPI runtime 3.1 disponible; paridad de operaciones sigue pendiente.
- TM-021: gate técnico completo de Fase 7.

Reports locales generados e ignorados:

- `backend/target/surefire-reports/TEST-com.qaflow.shared.error.GlobalExceptionHandlerTest.xml`
- `backend/target/failsafe-reports/TEST-com.qaflow.integration.BackendInfrastructureIT.xml`

## 13. Riesgos

- Host sin JDK 17/Maven global: la reproducción actual depende de Docker o de instalar JDK 17 por el usuario.
- Crear triggers bajo binlog requiere política/cuenta de migración DBA; no debe darse `SUPER` a runtime.
- `TINYINT(1)` genera warning deprecatorio; una migración futura puede normalizarlo tras revisar mapeos.
- Solo `audit_events` tiene mapeo JPA; entidades de negocio se agregan en su fase.
- Swagger está público hasta que Fase 8 configure Security.
- OpenAPI runtime todavía no puede compararse con las 39 operaciones porque no existen controllers.
- No hay baseline commit; todos los archivos permanecen untracked por decisión de no hacer staging/commit automático.

## 14. Estado actualizado

- Fases 0–6: completadas y aprobadas.
- Fase 7: completada y aprobada el 2026-07-19.
- Build Java 17: PASS.
- Unit/web: 4 PASS.
- Integración MySQL: 2 PASS.
- Smoke manual: PASS.
- Contenedores temporales activos: ninguno.
- Seguridad/endpoints de negocio: no implementados por alcance.

## 15. Próximo paso

Tras aprobación, iniciar únicamente Fase 8: usuarios/roles, BCrypt, JWT HS256, login, `authVersion`, SecurityFilterChain, handlers uniformes `401/403`, seed de perfiles demo/test y pruebas unitarias/API de autenticación y permisos.

## 16. Registro de aprobación

Fase 7 aprobada por el usuario el 2026-07-19. Se autoriza iniciar únicamente la Fase 8.
