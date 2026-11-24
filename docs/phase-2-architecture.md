# Fase 2 — Arquitectura

## 1. Nombre de la fase

Fase 2. Arquitectura.

### Inicio obligatorio de continuidad

1. **Resumen del estado anterior:** las Fases 0 y 1 fueron aprobadas el 2026-07-19. Existen alcance, requisitos, reglas, permisos y escenarios; todavía no existe código.
2. **Decisiones que siguen vigentes:** MVP acotado, aprobación por fase, mínimo privilegio, reserva al confirmar, consumo al completar, estados controlados, una inspección por orden, desactivación lógica, snapshot de BOM, dashboard por rol y evidencia antes de afirmar éxito.
3. **Archivos existentes utilizados:** `PROJECT_STATE.md`, `ROADMAP.md`, `DECISIONS.md`, `FILE_MANIFEST.md`, `TEST_MATRIX.md`, `OPEN_ISSUES.md`, `INTERVIEW_NOTES.md`, `docs/phase-0-discovery.md` y `docs/phase-1-requirements.md`.
4. **Objetivo de la fase actual:** definir cómo se organizarán aplicación, seguridad, persistencia, pruebas y entrega sin inicializar todavía el proyecto.
5. **Criterios de terminación:** arquitecturas general, backend, frontend, pruebas y DevOps documentadas; flujos, capas, componentes y dependencias coherentes; decisiones y riesgos registrados; aprobación explícita.

## 2. Objetivo

Diseñar una arquitectura implementable, verificable y proporcional al MVP que preserve las reglas de negocio, evite dependencias circulares, permita pruebas por nivel y produzca una ejecución reproducible en desarrollo, Docker y CI.

## 3. Dependencias

### Documentales

- Fases 0 y 1 aprobadas.
- Requisitos RF, RNF, reglas RN-001 a RN-036 y matriz de permisos.
- Decisiones D-0001 a D-0013.

### Tecnológicas confirmadas

- Java 17 y Spring Boot.
- Spring Web, Data JPA, Security y JWT.
- MySQL.
- React con Vite, Bootstrap y Axios.
- JUnit 5, Mockito, Postman/Newman, Cypress y Locust.
- Docker, Docker Compose, Git y GitLab CI/CD.

Las versiones exactas se fijarán y verificarán al crear los manifiestos de construcción; esta fase no afirma compatibilidad de versiones todavía no ejecutadas.

## 4. Entradas requeridas

- Flujo funcional desde login hasta inspección.
- Roles y permisos aprobados.
- Invariantes de inventario y estados.
- Requisitos de trazabilidad, errores, seguridad, portabilidad y pruebas.
- Riesgos abiertos OI-002, OI-005, OI-006 y OI-008.

No se requiere una elección adicional del usuario para preparar esta propuesta. Los detalles físicos del modelo y contrato se reservan para Fases 3 y 4.

## 5. Entregables

- Arquitectura general.
- Arquitectura backend.
- Arquitectura frontend.
- Arquitectura de pruebas.
- Arquitectura DevOps.
- Flujo de autenticación y autorización.
- Flujo principal de manufactura.
- Diagramas textuales de contexto, contenedores y componentes.
- Responsabilidades por capa y módulo.
- Dependencias internas y externas.
- Estrategia transaccional y de concurrencia.
- Estrategia de auditoría, errores y observabilidad.
- Decisiones técnicas y consecuencias.

## 6. Decisiones tomadas

Estas decisiones fueron aprobadas el 2026-07-19 y están vigentes:

1. Monolito modular backend + SPA frontend + una base MySQL.
2. Paquetes backend por funcionalidad, con capas internas y dependencias dirigidas.
3. REST JSON versionado bajo `/api/v1`; entidades JPA nunca se exponen directamente.
4. Flyway administra el esquema y JPA lo valida.
5. Transacciones en servicios de aplicación y bloqueo pesimista ordenado para inventario/órdenes.
6. Spring Security OAuth2 Resource Server valida JWT Bearer; HMAC-SHA-256 y expiración breve configurable.
7. Una versión de autorización del usuario invalida tokens cuando cambia rol, contraseña o estado.
8. El frontend guarda el token en `sessionStorage`, lo envía con Axios y reconoce explícitamente el riesgo XSS.
9. Auditoría persistente y append-only, separada de logs técnicos.
10. Testcontainers con MySQL para integración; no se usará H2 como sustituto de MySQL.
11. Imágenes Docker multi-stage; Nginx sirve la SPA y actúa como proxy de mismo origen.
12. No se incorporan microservicios, Redis, broker de mensajes, Redux, Kubernetes, Lombok ni MapStruct en el MVP.

## 7. Diseño

### 7.1 Principios arquitectónicos

- **Backend como autoridad:** UI y API pueden validar forma; reglas, permisos y transacciones se aplican en backend.
- **Una sola fuente de verdad:** MySQL conserva estado de negocio; dashboard y reportes se derivan de él.
- **Modularidad sin distribución:** límites lógicos claros dentro de un único despliegue y una transacción local.
- **Dependencias dirigidas:** presentación depende de aplicación; aplicación de dominio/repositorios; nunca al revés.
- **DTO en fronteras:** ninguna entidad JPA se serializa como contrato público.
- **Invariantes en profundidad:** servicio/dominio valida y base de datos refuerza unicidad e integridad.
- **Trazabilidad explícita:** auditoría de negocio, historial de estados y logs correlacionados cumplen propósitos distintos.
- **Configuración externa:** secretos y diferencias de ambiente no se compilan en el artefacto.
- **Diseño para pruebas:** puertos claros, inyección de dependencias, reloj controlable y datos deterministas.

### 7.2 Arquitectura general

QAFlow será un sistema web de tres contenedores lógicos:

1. **Frontend SPA:** React/Vite durante desarrollo; archivos estáticos servidos por Nginx en Docker.
2. **Backend API:** aplicación Spring Boot desplegada como un único proceso modular.
3. **Base de datos:** MySQL/InnoDB con un esquema administrado por Flyway.

La decisión de monolito modular permite:

- Transacciones locales para reservar y consumir inventario.
- Menos infraestructura y fallos distribuidos.
- Ejecución sencilla durante una entrevista.
- Separación suficiente para probar y explicar módulos.
- Evolución futura a servicios solo si métricas y límites reales lo justifican.

### 7.3 Diagramas textuales

#### Diagrama de contexto

```text
[ADMIN] ------------------------+
[OPERATOR] ---------------------+--> [QAFlow Web] --> [MySQL]
[QUALITY_INSPECTOR] ------------+

[Postman/Newman] ---------------> [QAFlow API]
[Cypress] ----------------------> [QAFlow Web + API]
[Locust] -----------------------> [QAFlow API]
[GitLab Runner] ----------------> [Build/Test/Package]
```

#### Diagrama de contenedores

```text
[Browser]
   |
   | HTTP(S), HTML/CSS/JS y /api/v1
   v
[Nginx / Vite dev server]
   |-- sirve React SPA
   |-- proxy /api, /actuator permitido y documentación configurada
   v
[Spring Boot modular monolith]
   |-- Security + JWT
   |-- REST controllers + DTO validation
   |-- Application/domain modules
   |-- JPA repositories + Flyway
   |-- Audit + correlation/logging
   v
[MySQL / InnoDB]
```

#### Diagrama de petición backend

```text
HTTP Request
  -> CorrelationIdFilter
  -> Spring Security filter chain
  -> Controller + request DTO + Bean Validation
  -> Application Service (@Transactional cuando muta)
  -> Domain policies/state transitions
  -> Repository ports / Spring Data JPA
  -> MySQL
  -> Audit record dentro de la transacción aplicable
  -> Response mapper + response DTO
  -> HTTP Response

Errores:
  AuthenticationEntryPoint -> 401
  AccessDeniedHandler      -> 403
  RestControllerAdvice     -> 400/404/409/5xx consistentes
```

### 7.4 Arquitectura backend

#### Estilo

Monolito modular con organización **feature-first**. Cada módulo contiene sus propias capas, evitando carpetas globales gigantes y facilitando identificar qué requisito implementa cada elemento.

```text
com.qaflow
  common/
    config/ security/ error/ web/ time/
  auth/
  user/
  product/
  material/
  bom/
  inventory/
  production/
  quality/
  dashboard/
  audit/

Cada módulo, según necesidad:
  api/             controllers, request/response DTOs, mappers
  application/     casos de uso, transacciones, orquestación
  domain/          entidades, enums, políticas, excepciones del módulo
  infrastructure/  repositorios JPA y adaptadores técnicos
```

No todos los módulos necesitan artificialmente todas las carpetas. La estructura debe reflejar responsabilidades reales y no crear clases vacías.

#### Responsabilidades por capa

| Capa | Responsabilidad | No debe hacer |
|---|---|---|
| Filtros/configuración web | Correlación, seguridad, CORS, serialización y aspectos HTTP transversales. | Implementar reglas de inventario u órdenes. |
| API/controller | Traducir HTTP a DTO, activar validación, invocar un caso de uso y mapear respuesta. | Acceder directamente a repositorios o abrir transacciones de negocio. |
| DTO/mapper | Definir frontera estable y conversión explícita. | Exponer contraseñas, entidades completas o lógica transaccional. |
| Application service | Orquestar casos de uso, permisos finos, transacciones y coordinación entre módulos. | Construir respuestas HTTP o depender de React. |
| Domain | Representar estados, invariantes y transiciones puras del negocio. | Conocer controladores, JSON o detalles de Nginx. |
| Repository/infrastructure | Persistir, consultar, bloquear filas y adaptar tecnología. | Decidir permisos o transiciones por sí solo. |
| Audit | Registrar eventos de negocio inmutables y sanitizados. | Sustituir logs técnicos o guardar secretos. |
| Common | Proveer infraestructura transversal verdaderamente compartida. | Convertirse en una carpeta de utilidades sin dueño. |

#### Responsabilidades por módulo

| Módulo | Responsabilidad principal | Dependencias permitidas |
|---|---|---|
| `auth` | Credenciales, emisión/validación JWT y eventos de autenticación. | `user`, `audit`, `common` |
| `user` | Ciclo de usuarios, rol, estado y versión de autorización. | `audit`, `common` |
| `product` | Catálogo de productos y unicidad SKU. | `audit`, `common` |
| `material` | Catálogo, código y unidad de medida. | `audit`, `common` |
| `bom` | BOM vigente y validación de líneas. | `product`, `material`, `audit`, `common` |
| `inventory` | Saldos, reservas, movimientos, locks e invariantes. | `material`, `audit`, `common` |
| `production` | Orden, snapshot de BOM y máquina de estados. | `product`, `bom`, `inventory`, `audit`, `common` |
| `quality` | Inspección final de órdenes completadas. | interfaz de lectura de `production`, `audit`, `common` |
| `dashboard` | Consultas agregadas de solo lectura por rol. | fachadas de lectura de módulos, `common` |
| `audit` | Persistencia/consulta de eventos y sanitización. | `common` |
| `common` | Errores, reloj, correlación y configuración común. | Ningún módulo de negocio |

#### Grafo de dependencias internas

```text
common
  ^
  +-- audit <-----------------------------+
  +-- user <--- auth                      |
  +-- product <--- bom <--- production ---+
  +-- material <--+  ^         |
  +-- inventory <-+  +---------+
  +-- production <--- quality
  +-- read facades <--- dashboard
```

La comunicación entre módulos ocurre mediante servicios/fachadas de aplicación, no mediante controladores. Las asociaciones persistentes se definirán en Fase 3 sin introducir ciclos de llamadas.

### 7.5 Persistencia y migraciones

- MySQL con motor InnoDB, codificación `utf8mb4` y timestamps en UTC.
- Flyway será el único responsable de crear y versionar el esquema.
- JPA/Hibernate usará `ddl-auto=validate`; no creará ni actualizará el esquema en ambientes normales.
- Spring Data JPA implementará repositorios y consultas derivadas/expresas.
- Cantidades usarán `BigDecimal`; precisión y escala exactas se decidirán en Fase 3.
- Identificadores únicos y relaciones se reforzarán con constraints e índices.
- Entidades mutables comunes usarán versión optimista para detectar actualizaciones perdidas cuando aplique.
- Inventario y transiciones críticas usarán locks explícitos, no confiarán solo en versión optimista.
- H2 queda excluido de integración porque su semántica no representa fielmente MySQL.

### 7.6 Transacciones y concurrencia de inventario

#### Estrategia

- Los límites `@Transactional` se ubican en servicios de aplicación.
- Se propone aislamiento `READ_COMMITTED` con bloqueos explícitos `PESSIMISTIC_WRITE`/`SELECT ... FOR UPDATE` para filas críticas.
- La orden se bloquea antes de transicionar.
- Los balances se bloquean por `materialId` ascendente para reducir deadlocks.
- Entrada, salida manual, confirmación, cancelación y finalización usan el mismo protocolo de lock.
- Cada material tendrá una fila de balance aun cuando su saldo sea cero, evitando la ausencia de fila que bloquear.
- Constraints de base de datos reforzarán `onHand ≥ reserved ≥ 0`.

#### Confirmación

```text
1. Bloquear orden DRAFT.
2. Leer BOM vigente y calcular requerimientos.
3. Bloquear balances afectados en orden determinista.
4. Validar material activo y disponible para todos.
5. Guardar snapshot de requerimientos.
6. Incrementar reservado para todos.
7. Cambiar orden a CONFIRMED y auditar.
8. Commit único; cualquier fallo revierte todo.
```

#### Finalización

```text
1. Bloquear orden IN_PROGRESS.
2. Bloquear balances de su snapshot en el mismo orden.
3. Verificar reservas íntegras.
4. Disminuir stock físico y reservado.
5. Insertar un movimiento de consumo por material.
6. Cambiar orden a COMPLETED y auditar.
7. Commit único; cualquier fallo revierte todo.
```

#### Cancelación

Bloquea orden y balances; libera reservas si las hay; cambia a `CANCELLED`; no modifica stock físico ni crea consumo.

Un lock timeout o deadlock residual revierte la transacción y se traducirá a un conflicto estable; el código exacto se fijará en Fase 4. La implementación deberá demostrar estas invariantes mediante pruebas concurrentes contra MySQL real.

### 7.7 Autenticación y seguridad

#### Componentes

- Spring Security con sesiones de servidor deshabilitadas (`STATELESS`).
- OAuth2 Resource Server para procesar `Authorization: Bearer <jwt>` mediante su filter chain mantenida por Spring.
- `JwtEncoder`/`JwtDecoder` con soporte JOSE administrado por Spring Security.
- Firma HMAC-SHA-256 con secreto de al menos 256 bits suministrado por variable de entorno.
- Expiración por defecto propuesta: 15 minutos, configurable por ambiente.
- BCrypt con costo configurable para contraseñas.
- `@PreAuthorize` para permisos de caso de uso y reglas de URL para fronteras públicas/privadas.

#### Claims mínimos

- `sub`: identificador estable del usuario.
- `iss` y `aud`: emisor y audiencia esperados por QAFlow.
- `role`: único rol autorizado.
- `authVersion`: versión de seguridad del usuario.
- `iat`, `exp` y `jti`.
- No se incluyen contraseñas, hashes, correo innecesario ni datos sensibles.

#### Invalidación por cambios de seguridad

Cada usuario tendrá una versión de autorización. Cambiar contraseña, rol o estado la incrementa. En cada petición protegida, la validación JWT comprobará:

1. Firma, emisor, tiempo y formato.
2. Usuario existente y activo.
3. `authVersion` del token igual a la actual.
4. Rol coherente con la versión actual.

Esto agrega una lectura breve de base de datos por petición, aceptable para el MVP, y hace efectiva inmediatamente la desactivación o cambio de rol. El sistema conserva JWT, pero no presume ser completamente stateless.

#### Logout, renovación y revocación

- El MVP no implementará refresh tokens.
- Logout elimina el token del cliente.
- No habrá blacklist por token individual.
- Un token robado puede utilizarse hasta expirar, salvo que se incremente `authVersion`; esta limitación debe documentarse.
- La corta expiración, TLS en despliegues reales, CSP, sanitización y control de dependencias reducen riesgo, pero no convierten el MVP en una solución IAM empresarial.

#### CORS y CSRF

- Docker usa proxy de mismo origen, por lo que normalmente no requiere CORS entre SPA y API.
- Desarrollo permite solo orígenes explícitos configurados, nunca `*` con credenciales.
- Al usar Bearer header y no cookies de autenticación, CSRF puede deshabilitarse para la API.
- El frontend no debe insertar HTML no confiable ni usar `dangerouslySetInnerHTML` para mensajes del servidor.

### 7.8 Flujo de autenticación y autorización

```text
1. React envía identificador + contraseña por HTTPS a /api/v1/auth/login.
2. DTO valida formato; AuthenticationManager carga usuario.
3. PasswordEncoder compara el hash y se comprueba estado activo.
4. Éxito: se registra evento y se emite JWT con rol/authVersion/exp/jti.
5. React conserva token en memoria + sessionStorage durante la pestaña.
6. Axios agrega Authorization: Bearer en cada petición protegida.
7. Resource Server valida firma y claims.
8. Validador consulta usuario activo y authVersion actual.
9. Spring construye Authentication con ROLE_ADMIN, ROLE_OPERATOR o ROLE_QUALITY_INSPECTOR.
10. Reglas URL y @PreAuthorize autorizan el caso de uso.
11. Controller invoca aplicación solo si la autorización pasó.

Fallos:
  credenciales/token inválido -> 401 JSON
  identidad válida sin permiso -> 403 JSON
  ambos incluyen correlationId y no filtran información sensible
```

### 7.9 Arquitectura frontend

#### Estilo

SPA React organizada por funcionalidad. No se usará Redux: `AuthContext` con reducer cubre el estado transversal de sesión; el resto del estado permanece cercano a cada pantalla o en hooks reutilizables.

```text
src/
  app/          router, providers, layout
  auth/         context, guards, login, session
  features/
    users/ products/ materials/ bom/ inventory/
    production/ quality/ dashboard/ audit/
  api/          axios client, interceptors, error mapping
  components/   controles compartidos con responsabilidad concreta
  hooks/        comportamiento reutilizable
  utils/        funciones puras limitadas
  styles/       Bootstrap overrides y estilos globales
```

La estructura definitiva se creará en Fases 6 y 14; este árbol es arquitectónico, no un archivo generado.

#### Responsabilidades frontend

| Elemento | Responsabilidad |
|---|---|
| Router | Definir rutas y composición de layout. |
| `ProtectedRoute` | Exigir sesión local válida; el backend sigue siendo autoridad. |
| `RoleRoute` | Ocultar rutas/acciones no aplicables y ofrecer UX de acceso denegado. |
| Auth context | Token, perfil, login/logout y restauración desde `sessionStorage`. |
| Axios client | Base URL, Bearer token, correlación recibida y normalización de errores. |
| Feature service/hook | Carga, mutaciones, estados loading/error/success y cancelación de peticiones. |
| Página/formulario | Presentación, validación inmediata y confirmación de acciones terminales. |
| Componentes comunes | Tabla paginada, feedback, modal, campo de formulario y estados vacíos. |

#### Manejo de errores

- `401`: limpiar sesión y dirigir a login conservando una ruta segura de retorno.
- `403`: mostrar acceso denegado sin fingir que el recurso no existe.
- `400`: asociar errores de campo cuando existan.
- `404`: mostrar recurso inexistente.
- `409`: explicar el conflicto y refrescar datos susceptibles a concurrencia.
- `5xx/red`: mensaje recuperable con correlationId cuando esté disponible.

#### Seguridad y testabilidad de UI

- Token en `sessionStorage`, nunca en URL ni logs.
- La exposición a XSS se reconoce; CSP, dependencias controladas y ausencia de HTML no confiable son obligatorias.
- Formularios no conservarán contraseñas después de fallar.
- Elementos de flujos críticos tendrán `data-cy` semánticos y estables.
- Bootstrap aporta layout; no se añadirán librerías visuales redundantes.
- Accesibilidad: labels, foco, teclado, roles semánticos, contraste y anuncios de error.

### 7.10 Arquitectura de pruebas

#### Pirámide y responsabilidades

```text
                 [E2E Cypress]
              [API Postman/Newman]
          [Integración Spring + MySQL]
       [Unitarias JUnit 5 + Mockito]

Transversales: SQL, seguridad, concurrencia, Locust, smoke Docker y CI
```

| Nivel | Alcance | Dependencias reales | Propósito |
|---|---|---|---|
| Unitario backend | Políticas, servicios aislados, mappers y transiciones. | Ninguna infraestructura; Mockito solo donde existe colaboración. | Feedback rápido y casos borde. |
| Integración repositorio | Constraints, queries, locks, Flyway y JPA. | MySQL Testcontainer. | Probar comportamiento real de persistencia. |
| Integración web/seguridad | Controller, validación, advice, filter chain y autorización. | Spring context + MySQL Testcontainer cuando aplique. | Verificar frontera HTTP interna. |
| API | Sistema levantado, contratos y flujo por módulos. | Backend + MySQL. | Caja negra con Postman/Newman. |
| E2E | Flujos críticos por rol en navegador. | Frontend + backend + MySQL. | Validar integración visible al usuario. |
| SQL | Invariantes, duplicados, movimientos, estados y auditoría. | MySQL con dataset conocido. | Evidencia independiente de API. |
| Rendimiento | Login/lecturas/órdenes definidas. | Stack desplegado y ambiente descrito. | Medir objetivos, no simular producción. |
| Sistema/DevOps | Healthchecks, arranque y artefactos. | Docker Compose/GitLab. | Reproducibilidad. |

#### Datos y aislamiento

- Integración usa MySQL mediante Testcontainers y migraciones Flyway; no H2.
- Cada suite crea datos únicos o limpia dentro de límites conocidos.
- Postman obtiene token automáticamente y usa variables de ejecución.
- Cypress prepara datos por API pública autorizada o por un seed de ambiente de prueba; no se crea un endpoint secreto de testing en producción.
- Tests concurrentes usan barreras/hilos controlados y verifican saldos, reservas y movimientos después del commit.
- Reloj y generación de identificadores se inyectan para evitar pruebas dependientes de hora o aleatoriedad incontrolable.
- Ningún resultado se marca aprobado hasta ejecutarse y adjuntar evidencia.

### 7.11 Arquitectura DevOps

#### Contenedores

| Servicio | Construcción/ejecución | Salud |
|---|---|---|
| `db` | Imagen oficial MySQL, volumen nombrado y usuario no root de aplicación. | `mysqladmin ping` o equivalente sin exponer secretos. |
| `backend` | Build multi-stage con Maven/JDK 17; runtime JRE 17 y usuario no root. | Spring Actuator `/actuator/health`. |
| `frontend` | Build multi-stage Node/Vite; runtime Nginx no root o configuración endurecida. | Endpoint estático `/health`. |

- Red interna dedicada.
- Solo puertos necesarios, parametrizables.
- Logs a stdout/stderr.
- Variables de entorno para DB, JWT, CORS y perfiles.
- `depends_on` con healthchecks ayuda al orden, pero backend también tolera/reporta fallos de conexión.
- El volumen MySQL conserva datos de demo; CI usa almacenamiento efímero.

#### Perfiles/configuración

| Perfil | Uso | Base de datos/configuración |
|---|---|---|
| `local` | Backend/SPA ejecutados desde IDE/CLI. | MySQL local o contenedor; CORS explícito para Vite. |
| `test` | Integración automatizada. | MySQL Testcontainer, datos aislados, secretos efímeros. |
| `docker` | Stack completo Compose. | Host `db`, proxy mismo origen y variables Compose. |

No habrá credenciales reales en YAML. Se versionará posteriormente `.env.example`; `.env` permanecerá ignorado.

#### Pipeline objetivo

```text
validate
  -> backend compile/config checks + frontend lint/config checks
build
  -> backend package + frontend build
unit-test
  -> unitarias + integración backend con reportes
api-test
  -> Compose/API + Newman
e2e-test
  -> Compose/browser + Cypress
performance-test (manual/opcional o programado)
package
  -> imágenes/artefactos solo si gates obligatorios pasan
```

Reportes JUnit, cobertura, Newman y Cypress se conservarán como artifacts. La forma exacta del runner y servicios se decidirá en Fase 21 según capacidad real; no se afirma que el pipeline haya sido ejecutado.

### 7.12 Flujo principal del sistema

```text
ADMIN
  1. Login -> JWT ADMIN.
  2. Crear material -> material + auditoría.
  3. Crear producto -> producto + auditoría.
  4. Definir BOM -> validar líneas activas -> BOM vigente + auditoría.

OPERATOR
  5. Login -> JWT OPERATOR.
  6. Entrada inventario -> lock balance -> saldo + movimiento + auditoría.
  7. Crear orden DRAFT.
  8. Confirmar:
       lock orden + balances ordenados
       leer BOM vigente
       calcular y guardar snapshot
       validar disponible
       reservar todo
       estado CONFIRMED
       commit único
  9. Iniciar -> IN_PROGRESS + historial/auditoría.
 10. Completar:
       lock orden + balances
       consumir snapshot reservado
       crear salidas
       estado COMPLETED
       commit único

QUALITY_INSPECTOR
 11. Login -> JWT QUALITY_INSPECTOR.
 12. Consultar órdenes COMPLETED sin inspección.
 13. Crear APPROVED o REJECTED con observación requerida.
 14. Guardar inspección inmutable + auditoría.

TODOS
 15. Dashboard consulta agregados autorizados; no modifica la fuente de verdad.
```

### 7.13 Errores, correlación y auditoría

#### Errores

- Un filtro acepta un `X-Correlation-Id` válido o genera uno nuevo y lo devuelve.
- Spring Security produce el mismo esquema base para `401` y `403` que el resto de la API.
- `RestControllerAdvice` traduce validación, no encontrado, conflicto y errores inesperados.
- Códigos de error estables permiten assertions de Newman/Cypress sin depender solo del texto.
- Stack traces quedan en logs internos correlacionados, nunca en la respuesta.

#### Auditoría persistente

- Append-only en MySQL.
- Registra actor cuando existe, acción, tipo/id de recurso, resultado, timestamp UTC, correlationId y resumen sanitizado.
- Cambios exitosos se registran dentro de la misma transacción de negocio.
- Login exitoso/fallido, denegaciones y rechazos críticos se registran mediante una transacción independiente corta para sobrevivir al rollback de la operación rechazada.
- Validaciones triviales de formulario se registran en log técnico, no saturan auditoría persistente.
- No almacena token, contraseña, hash, secreto ni payload sensible.
- El MVP no purga automáticamente auditoría; se conserva durante la vida de sus datos. Una política regulatoria real queda fuera del alcance.

### 7.14 Observabilidad

- Actuator expone salud y, solo con autorización/configuración, información adicional necesaria.
- Logs estructurados incluyen timestamp, nivel, logger, correlationId y evento.
- Métricas empresariales avanzadas, tracing distribuido y plataforma externa no se incorporan al MVP.
- Healthcheck no expone credenciales ni detalles internos al público.
- Auditoría explica acciones de negocio; logs explican ejecución y fallos técnicos.

### 7.15 Dependencias externas previstas

#### Backend de producción

- Spring Boot starters: Web, Validation, Data JPA, Security, OAuth2 Resource Server y Actuator.
- MySQL Connector/J.
- Flyway Core y soporte MySQL.
- Springdoc OpenAPI para Swagger/OpenAPI.

#### Backend de prueba

- Spring Boot Starter Test: JUnit 5, Mockito y soporte Spring Test/MockMvc.
- Spring Security Test.
- Testcontainers JUnit Jupiter y MySQL.
- JaCoCo como generador de cobertura, con política exacta en Fase 5/15.

#### Frontend

- React y React DOM.
- Vite.
- React Router.
- Axios.
- Bootstrap.
- Cypress como dependencia de desarrollo.

#### Construcción/operación

- Maven Wrapper y npm con lockfile.
- Docker/Docker Compose.
- Nginx para runtime frontend.
- Postman/Newman y Python/Locust en pruebas dedicadas.

No se proponen Lombok ni MapStruct: mappers y modelos críticos serán explícitos, favoreciendo lectura en entrevista y reduciendo generación oculta. Las versiones se fijarán después de verificar Java 17, Node y compatibilidad entre dependencias.

### 7.16 Trazabilidad de atributos de calidad

| Requisito de calidad | Respuesta arquitectónica | Evidencia futura |
|---|---|---|
| RNF-SEC | Resource Server, hash adaptativo, permisos de método, secretos externos y JWT breve/versionado. | Pruebas `401/403`, análisis config y E2E por rol. |
| RNF-DAT | Transacciones locales, locks ordenados, constraints, Flyway y MySQL real. | Integración concurrente y SQL. |
| RNF-PER | Paginación, índices futuros, consultas de lectura y stack simple. | Locust + plan de ejecución cuando aplique. |
| RNF-REL | Estado terminal, locks, rollback y movimientos append-only. | Pruebas de repetición/fallo parcial. |
| RNF-USA/ACC | SPA por rol, feedback consistente, semántica y teclado. | Cypress y revisión manual. |
| RNF-POR | Perfiles, configuración externa y contenedores Linux. | Docker Compose en Windows/CI. |
| RNF-MAN | Módulos feature-first, capas, DTOs y dependencias dirigidas. | Revisión estática y estructura. |
| RNF-TST | DI, reloj controlable, MySQL Testcontainers y `data-cy`. | Suites por nivel y reportes. |
| RNF-OBS | Correlación, logs, health y auditoría separada. | Respuestas/logs/SQL correlacionados. |
| RNF-CIC | Pipeline con gates y artifacts. | Ejecución GitLab real en Fase 21. |

### 7.17 Alternativas descartadas

| Alternativa | Motivo de descarte para el MVP |
|---|---|
| Microservicios | Añaden red, consistencia distribuida, despliegues y observabilidad sin necesidad de escala demostrada. |
| H2 para integración | Puede ocultar diferencias de SQL, constraints, locks y tipos respecto a MySQL. |
| Descontar inventario al confirmar | Contradice el flujo aprobado de consumo al completar. |
| Revalidar solo al completar | Permite sobreasignación y falla demasiado tarde. |
| Refresh token y blacklist | Aumentan superficie y persistencia de seguridad; authVersion + token breve cubren el MVP. |
| JWT en `localStorage` permanente | Persiste más allá de la pestaña y aumenta exposición; se prefiere `sessionStorage` con limitación declarada. |
| Redux/TanStack Query | No se justifica todavía por complejidad de estado; se reconsideraría con evidencia. |
| Kafka/RabbitMQ | No existe integración asíncrona ni volumen que lo justifique. |
| Eliminación física en cascada | Rompe trazabilidad e historial. |

## 8. Archivos creados

- `docs/phase-2-architecture.md`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-1-requirements.md` (solo estado de aprobación)

## 10. Comandos

No se ejecutan compilación, aplicación, base de datos ni pruebas. Esta fase crea arquitectura documental, no el proyecto completo.

## 11. Cómo verificar

1. Recorrer RF/RNF y localizar el componente responsable.
2. Comprobar que el grafo de módulos no tenga dependencias circulares.
3. Simular confirmación, cancelación y finalización verificando locks y rollback.
4. Comprobar que el flujo distingue autenticación `401` y autorización `403`.
5. Confirmar que ninguna entidad JPA se propone como contrato REST.
6. Confirmar que cada nivel de pruebas tiene alcance y dependencia real definidos.
7. Verificar que Docker/CI distinguen diseño de ejecución comprobada.
8. Confirmar que no existe código creado por esta fase.

## 12. Pruebas relacionadas

No se crean ni ejecutan pruebas. `TEST_MATRIX.md` se actualiza con componentes y niveles responsables. La estrategia de casos y gates exactos se define en Fase 5.

## 13. Riesgos

- El lock pesimista reduce carreras, pero requiere índices, orden de bloqueo y pruebas reales para evitar deadlocks.
- `authVersion` introduce una consulta por petición y limita el carácter stateless del JWT.
- `sessionStorage` es accesible desde JavaScript y depende de controles XSS adecuados.
- Testcontainers requiere Docker y un runner compatible.
- Los objetivos de rendimiento aún no están medidos.
- La política de auditoría del MVP no sustituye obligaciones regulatorias reales.
- La versión exacta de cada dependencia permanece sin validar hasta la inicialización.

## 14. Estado actualizado

- Fases 0 y 1: completadas y aprobadas.
- Fase 2: completada y aprobada el 2026-07-19.
- Código creado: ninguno.
- Pruebas ejecutadas: ninguna.

## 15. Próximo paso

Tras aprobación, iniciar la Fase 3 para definir entidades, campos, tipos, relaciones, restricciones, índices, auditoría, estados, ER textual, SQL inicial, datos de prueba y consultas de validación.

## 16. Pregunta de aprobación

Estado de la compuerta: aprobada el 2026-07-19; la Fase 3 fue autorizada.
