# Fase 8 — Autenticación y seguridad

## Continuidad obligatoria

### Resumen del estado anterior

Las Fases 0–7 están completadas y aprobadas. La Fase 7 dejó Spring Boot 3.5.13 ejecutable con Java 17, Flyway/MySQL, error JSON, correlación, auditoría append-only, Springdoc y seis pruebas técnicas verificadas.

### Decisiones que siguen vigentes

- Monolito modular, paquetes por funcionalidad y DTOs separados de entidades.
- Flyway es autoridad del esquema; Hibernate solo valida.
- JWT HS256 de 15 minutos, sin refresh ni blacklist, con `authVersion` y lectura de usuario en cada petición.
- BCrypt mediante `DelegatingPasswordEncoder`; secretos solo por variables.
- MySQL real con Testcontainers; mocks se clasifican como unitarios.
- `ADMIN` administra usuarios; `OPERATOR` y `QUALITY_INSPECTOR` reciben `403` en `/users`.

### Archivos existentes utilizados

- `backend/pom.xml`, Maven Wrapper y perfiles `application*.yml`.
- Flyway `V1__initial_schema.sql`, tabla `users` y `audit_events`.
- Boundary de errores, filtro de correlación y puerto de auditoría.
- Contrato `docs/api/openapi.json` y casos de Fase 5.

### Objetivo de la fase actual

Implementar usuarios, roles, BCrypt, login, emisión/validación JWT, autorización, invalidación inmediata por cambios de seguridad, datos demo aislados y pruebas reales de `401`/`403` sin iniciar productos o materiales.

### Criterios para considerar la fase terminada

1. Login correcto/incorrecto, token válido/inválido/expirado y `/auth/me` verificados.
2. Roles y cinco operaciones ADMIN de usuarios implementados conforme al contrato.
3. Cambio de rol, estado o contraseña incrementa `authVersion`; token previo queda inválido.
4. Último ADMIN y versión obsoleta devuelven `409`.
5. Errores `400/401/403/404/409/500` conservan el envelope y correlación.
6. Bootstrap demo desactivado por defecto y prohibido en `prod`.
7. Pruebas unitarias/web e integración HTTP/MySQL pasan en Java 17.
8. Documentos de continuidad actualizados y aprobación del usuario pendiente.

## 1. Nombre de la fase

**Fase 8 — Autenticación y seguridad.**

## 2. Objetivo

Cerrar la frontera de identidad/autorización del backend y ofrecer gestión administrativa de usuarios ejecutable, auditable y probada contra MySQL.

## 3. Dependencias

- Fase 7 aprobada.
- Tabla `users`, generated column `username_normalized`, optimistic `version` y `auth_version` de V1.
- Spring Security, OAuth2 Resource Server/Jose, JPA, Validation y auditoría.
- Docker Desktop para el gate Testcontainers del host actual.

## 4. Entradas requeridas

- `JWT_SECRET_BASE64`: Base64 que decodifica al menos 32 bytes.
- `JWT_ISSUER`, `JWT_AUDIENCE` y expiración opcional, default 900 segundos.
- Datasource MySQL y política de triggers ya documentada.
- Para bootstrap demo opcional: tres usernames/display names/passwords por variables; nunca se versionan valores reales.

## 5. Entregables

- Modelo/repositorio de usuario con roles, estado, versiones y auditoría temporal.
- Login y perfil actual.
- JWT HS256 firmado, con `iss`, `aud`, `sub`, `iat`, `exp`, `jti`, `role` y `authVersion`.
- Validación de firma/tiempo/emisor/audiencia y usuario activo/rol/versión actuales.
- Security filter chain stateless, `@PreAuthorize`, `401`/`403` JSON y `WWW-Authenticate`.
- CRUD contractual de usuario sin delete ni cambio de username.
- BCrypt, bootstrap demo aislado y auditoría de login/permisos/mutaciones.
- Suite JUnit/Mockito/web e integración HTTP/MySQL.

## 6. Decisiones tomadas

- D-0076: Resource Server/Nimbus y HS256 con secreto externo mínimo de 256 bits.
- D-0077: validar usuario activo, rol y `authVersion` contra MySQL en toda petición protegida.
- D-0078: mantener `DelegatingPasswordEncoder` con BCrypt y no devolver credenciales/hashes.
- D-0079: bloquear los ADMIN activos para proteger el último administrador y usar versión optimista contractual.
- D-0080: bootstrap de tres roles opt-in, idempotente por username y prohibido en `prod`.
- D-0081: diferir auditoría de rechazos hasta terminar la transacción que sostiene locks y luego persistirla en `REQUIRES_NEW`.
- D-0082: para Maven dentro de Docker Desktop, usar `host.docker.internal`; Ryuk se deshabilita solo en ese runner y se verifica limpieza explícita.

Todas permanecen propuestas hasta aprobar esta fase.

## 7. Diseño o código

Flujo de login:

```text
POST /api/v1/auth/login
  -> Bean Validation
  -> DaoAuthenticationProvider + UserDetailsService
  -> DelegatingPasswordEncoder/BCrypt + active
  -> auditoría LOGIN_SUCCEEDED o LOGIN_REJECTED
  -> JwtEncoder HS256
  -> LoginResponse + Cache-Control: no-store
```

Flujo protegido:

```text
Bearer token
  -> firma HS256 + iss/aud/exp
  -> users[id]: active + role + authVersion
  -> QaFlowPrincipal + ROLE_*
  -> reglas URL + @PreAuthorize
  -> caso de uso o 401/403 JSON correlacionado
```

Rutas implementadas:

| Método | Ruta | Acceso | Resultado principal |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Público, usuario activo | JWT 900 s |
| GET | `/api/v1/auth/me` | Cualquier rol activo | Perfil actual |
| GET/POST | `/api/v1/users` | ADMIN | Listado/creación |
| GET/PATCH | `/api/v1/users/{userId}` | ADMIN | Detalle/actualización |
| PATCH | `/api/v1/users/{userId}/password` | ADMIN | `204`, revoca tokens previos |

No existen refresh token, blacklist, logout backend, recuperación de contraseña, productos o materiales en esta fase.

## 8. Archivos creados

- 36 clases de producción bajo `auth/`, `security/`, `user/`, `shared/` y `audit/`.
- 5 clases de prueba nuevas, incluida `SecurityApiIT`.
- Este documento.

La lista ruta por ruta está en `FILE_MANIFEST.md`.

## 9. Archivos modificados

- Dependencias/configuración: `backend/pom.xml`, `.env.example`, `application.yml`, `application-test.yml`.
- Infraestructura común: OpenAPI y `GlobalExceptionHandler`.
- Pruebas existentes: `GlobalExceptionHandlerTest`, `BackendInfrastructureIT`.
- READMEs y los siete documentos de continuidad.
- `ROADMAP.md` y el documento Fase 7 para registrar su aprobación.

## 10. Comandos

Gate final ejecutado en Java 17:

```powershell
docker run --rm `
  --env TESTCONTAINERS_RYUK_DISABLED=true `
  --env TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  --volume /var/run/docker.sock:/var/run/docker.sock `
  --mount type=bind,source=D:\qaflow\backend,target=/workspace `
  --workdir /workspace `
  maven:3.9.11-eclipse-temurin-17 mvn -B -ntp verify
```

En un host con JDK 17 y Docker accesible directamente:

```powershell
cd backend
.\mvnw.cmd clean verify
```

El wrapper del PowerShell actual volvió a fragmentar argumentos; el contenedor Java 17 es la evidencia reproducible utilizada.

## 11. Cómo verificar

1. Configurar DB y `JWT_SECRET_BASE64` según `.env.example`.
2. Ejecutar `clean verify` con JDK 17 y Docker.
3. Confirmar 13 Surefire + 8 Failsafe, sin failures/errors/skips.
4. Iniciar aplicación y consultar `/v3/api-docs`; deben existir las siete operaciones Fase 8 y `bearerAuth`.
5. Login devuelve Bearer/900 s; usar token en `/auth/me`.
6. Probar `/users` con OPERATOR (`403`) y ADMIN (`200`).
7. Cambiar rol/password y confirmar que el token previo devuelve `401 INVALID_TOKEN`.
8. Confirmar `audit_events` para éxito/rechazo sin contraseñas ni tokens.

## 12. Pruebas relacionadas

Ejecución definitiva `2026-07-20T03:39:53Z–03:41:52Z` UTC:

| Suite | Tipo real | Casos | Resultado |
|---|---|---:|---|
| `AuthenticationServiceTest` | Unitaria Mockito | 2 | PASS |
| `ActiveUserJwtValidatorTest` | Unitaria Mockito | 2 | PASS |
| `DemoUserBootstrapTest` | Unitaria Mockito | 2 | PASS |
| `UserServiceTest` | Unitaria Mockito | 3 | PASS |
| `GlobalExceptionHandlerTest` | Web slice | 4 | PASS |
| `BackendInfrastructureIT` | Integración Spring/MySQL | 2 | PASS |
| `SecurityApiIT` | Integración HTTP/Spring Security/MySQL | 6 | PASS |
| Total | 13 Surefire + 8 Failsafe | 21 | PASS |

Reports locales ignorados por Git:

- `backend/target/surefire-reports/TEST-*.xml`
- `backend/target/failsafe-reports/TEST-*.xml`

## 13. Riesgos

- HMAC comparte secreto entre emisor/validador; rotación de claves no está implementada.
- No hay refresh token ni revocación por JTI; un token robado dura hasta expirar salvo cambio de `authVersion`.
- Cada petición protegida lee `users`; es una decisión de revocación inmediata aceptable para el MVP, no “stateless puro”.
- El frontend futuro guardará el token en `sessionStorage`; riesgo XSS sigue abierto hasta Fase 14.
- Bootstrap demo solo debe activarse en bases desechables.
- Runner Maven anidado requiere host override y limpieza si se interrumpe con Ryuk deshabilitado.
- Los cuatro warnings históricos `TINYINT(1)` permanecen.

## 14. Estado actualizado

- Fases 0–7: completadas y aprobadas.
- Fase 8: completada, verificada y aprobada el 2026-07-19.
- Build Java 17: PASS.
- 13 pruebas unitarias/web: PASS.
- 8 integraciones MySQL: PASS.
- Rutas Fase 8 en OpenAPI: PASS.
- Fase 9: autorizada para iniciar.

## 15. Próximo paso

Después de aprobación, iniciar únicamente Fase 9: productos y materiales con entidades/DTOs/repositorios/servicios/controladores, unicidad normalizada, validaciones, permisos, Swagger, pruebas y artefactos Postman previstos.

## 16. Registro de aprobación

Fase 8 aprobada por el usuario el 2026-07-19. Se autoriza iniciar únicamente productos y materiales de la Fase 9.
