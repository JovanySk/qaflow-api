# TEST_MATRIX

## Estado de la matriz

Esta es la línea base de cobertura prevista, actualizada hasta Fase 10. Se diseñaron 98 casos funcionales en `docs/phase-5-test-catalog.md`; Fases 8–10 ejecutaron automatización trazable para autenticación, usuarios, productos, materiales y BOM, pero los casos de módulos inexistentes permanecen `NOT_RUN`. El gate definitivo ejecutó 26 pruebas Surefire y 22 Failsafe (48 PASS). No se convierte el catálogo completo en PASS por asociación.

## Cobertura detallada propuesta en Fase 5

| Entrada | Casos principales | Gate/capa dominante | Estado |
|---|---|---|---|
| TM-001 | TC-AUT-001 a TC-AUT-008 | Unit, security slice, integración, API, E2E | AUT-001–007 backend ejecutados; AUT-008 frontend `NOT_RUN` |
| TM-002 | TC-SEC-001 a TC-SEC-006 | Security slice, integración, API, E2E | SEC-001/002/004 parcial Fase 8; módulos futuros/UI `NOT_RUN` |
| TM-003 | TC-USR-001 a TC-USR-007 | Unit, integración, API | USR-001–007 backend ejecutados; combinatoria >20 de USR-004 pendiente |
| TM-004 | TC-PRD-001 a TC-PRD-006 | Unit, integración, API, E2E, SQL | PRD-001–005 backend PASS; PRD-006 depende de BOM/orden y E2E/SQL siguen `NOT_RUN` |
| TM-005 | TC-MAT-001 a TC-MAT-007 | Unit, integración, API, SQL | MAT-001–007 backend PASS, incluida carrera optimista compartida; SQL externo F18 `NOT_RUN` |
| TM-006 | TC-BOM-001 a TC-BOM-008 | Unit, integración, API, SQL | BOM backend PASS: publicación, validación, historia, permisos, trigger y concurrencia; Newman/SQL externo `NOT_RUN` |
| TM-007 | TC-INV-001 a TC-INV-011 | Unit, integración concurrente, API, E2E, SQL | Diseñada; no ejecutada |
| TM-008 | TC-ORD-001 a TC-ORD-016 | Unit, integración, API, E2E, SQL | Diseñada; no ejecutada |
| TM-009 | TC-ORD-005 a TC-ORD-016, especialmente 010/011/015 | Integración transaccional/concurrente y SQL | Diseñada; no ejecutada |
| TM-010 | TC-QLT-001 a TC-QLT-008 | Unit, integración, API, E2E, SQL | Diseñada; no ejecutada |
| TM-011 | TC-DSH-001 a TC-DSH-003 | Integración, API, E2E, SQL | Diseñada; no ejecutada |
| TM-012 | TC-ERR-001 a TC-ERR-005 y TC-NFR-001/002 | Web slice, integración, API | Error web + 400/401/403/404/409 Fases 8–10 PASS parcial |
| TM-013 | TC-AUD-001 a TC-AUD-005 | Integración, API, SQL, manual | Login/permisos/usuarios/catálogos/BOM + persistencia/inmutabilidad PASS parcial |
| TM-014 | TC-NFR-005 y TC-NFR-006 | Locust | Diseñada; no ejecutada |
| TM-015 | TC-NFR-004 | Docker/health smoke | Backend+DB parcial PASS; stack completo pendiente |
| TM-016 | Gates 7.20 y TC-NFR-007 | GitLab CI/artifacts/config | Diseñada; no ejecutada |
| TM-017 | Auditoría de OpenAPI de Fase 4 | Node.js estático + Springdoc runtime | Estática conforme; runtime 19/39 parcial PASS |
| TM-018 | TC-NFR-008 y EX-01 a EX-07 | Manual, exploratoria y componente/E2E dirigido | Diseñada; no ejecutada |
| TM-019 | Integridad del catálogo/estrategia de Fase 5 | Node.js estático | Ejecutada; conforme con límites declarados |
| TM-020 | Integridad de rutas/ownership de Fase 6 | PowerShell estático | Ejecutada; conforme con límites declarados |
| TM-021 | Inicialización backend Fase 7 | Maven/Surefire/Failsafe/Testcontainers/smoke | Ejecutada; PASS con límites declarados |
| TM-022 | Autenticación, seguridad y usuarios Fase 8 | JUnit/Mockito/web/HTTP/Testcontainers MySQL | Ejecutada; 21 PASS con límites declarados |
| TM-023 | Productos y materiales Fase 9 | JUnit/Mockito/HTTP/JPA concurrente/Testcontainers MySQL/Postman estático | Java ejecutado: 34 PASS total; Postman JSON PASS estructural y Newman `NOT_RUN` |
| TM-024 | BOM Fase 10 | JUnit/Mockito/HTTP/JPA locks/Testcontainers MySQL | Java ejecutado: 48 PASS total; 7 unitarias + 7 integraciones BOM, Newman/SQL externo `NOT_RUN` |

## Ejecuciones técnicas de Fase 7

| ID de ejecución | Alcance | Ambiente/herramienta | Estado | Evidencia | Resultado |
|---|---|---|---|---|---|
| F7-GIT-001 | Inicialización aislada Git | Git 2.54, `D:\qaflow` | PASS | toplevel `D:/qaflow`, rama `main` | Sin staging, commit o remote |
| F7-WRP-001 | Maven Wrapper | Maven 3.9.11, Java 17.0.17 | PASS | `./mvnw -v` | Versiones exactas resueltas |
| F7-UT-001 | Error/correlación web | Surefire + MockMvc, Java 17 | PASS | JUnit XML: 4/0/0/0 | Validación, 404, correlation y 500 sanitizado |
| F7-IT-001 | Infraestructura MySQL | Failsafe + Testcontainers MySQL 8.0.42 | PASS | JUnit XML: 2/0/0/0 | Flyway/JPA/health/OpenAPI/auditoría/trigger |
| F7-BLD-001 | Gate backend | `mvn -B -ntp clean verify`, Java 17 | PASS | BUILD SUCCESS, JAR generado | 6 tests totales sin failures/errors/skips |
| F7-SMK-001 | Aplicación iniciada | Java 17.0.17 + MySQL 8.0.42 temporal | PASS | Health UP; Swagger/OpenAPI 200; DB 12/1/10 | Recursos temporales eliminados |

## Ejecuciones de Fase 8

| ID de ejecución | Alcance | Ambiente/herramienta | Estado | Evidencia | Resultado |
|---|---|---|---|---|---|
| F8-UT-001 | Auth service, validador JWT, bootstrap y user service | JUnit 5 + Mockito, Java 17 | PASS | Surefire XML: 9/0/0/0 | Éxito/rechazo, active/role/version, tres roles, duplicado, último ADMIN y password |
| F8-WEB-001 | Error boundary/correlación bajo starter Security | `@WebMvcTest` + MockMvc | PASS | Surefire XML: 4/0/0/0 | 400/404/500 sanitizado y correlación |
| F8-IT-001 | Infraestructura + rutas OpenAPI Fase 8 | Spring Boot + MySQL 8.0.42 | PASS | Failsafe XML: 2/0/0/0 | Flyway/JPA/health/OpenAPI/auditoría y siete operaciones visibles |
| F8-API-001 | Login, tokens, permisos y cinco operaciones usuario | HTTP real + Spring Security + MySQL | PASS | Failsafe XML: 6/0/0/0 | 400/401/403/409, revocación, filtros, último ADMIN, concurrencia y auditoría |
| F8-BLD-001 | Gate backend definitivo | Maven `verify`, Java 17.0.17 | PASS | BUILD SUCCESS 2026-07-20T03:41:52Z | 13 Surefire + 8 Failsafe = 21; 0 failures/errors/skips |
| F8-REG-001 | Regresión dirigida de listado/detalle | `SecurityApiIT` aislada | PASS | 6/0/0/0 | Búsqueda, filtros, orden, página y detalle ADMIN incluidos |

Intentos no contabilizados como PASS: un wrapper PowerShell fragmentó argumentos; Ryuk y `172.17.0.1` bloquearon el runner Maven anidado; una corrida reveló MySQL 1205 en auditoría `REQUIRES_NEW`. Los tres fueron corregidos y el gate definitivo fue ejecutado desde cero/verify según se documenta en Fase 8.

## Ejecuciones de Fase 9

| ID de ejecución | Alcance | Ambiente/herramienta | Estado | Evidencia | Resultado |
|---|---|---|---|---|---|
| F9-UT-001 | Reglas de ProductService y MaterialService | JUnit 5 + Mockito, Java 17 | PASS | Surefire XML: 6/0/0/0 | Duplicados, PATCH null/baja, versión, balance y unidad con/sin historia |
| F9-API-001 | Ocho operaciones, validación, filtros y permisos | HTTP real + Spring Security + MySQL 8.0.42 | PASS | `CatalogApiIT`: 6 escenarios API dentro de 7 casos | 201/200/400/401/403/404/409, balance cero y auditoría |
| F9-CON-001 | Dos escritores sobre versión 0 | JPA + dos transacciones + latches + MySQL | PASS | `CatalogApiIT`: 1 caso concurrente | Exactamente un commit, un optimistic conflict y versión final 1 |
| F9-OAS-001 | Paths runtime de catálogos | Springdoc OpenAPI 3.1 | PASS | Assertion de ocho método/ruta | Runtime acumulado 15/39 operaciones |
| F9-PST-001 | Colección/environment Postman | Collection v2.1 + PowerShell JSON parser | PASS estructural | 12 requests, 7 variables | JSON válido, secretos vacíos; ejecución Newman `NOT_RUN` hasta F16 |
| F9-REG-001 | Infraestructura y seguridad previas | Failsafe/Testcontainers MySQL | PASS | BackendInfrastructureIT 2 + SecurityApiIT 6 | Ocho integraciones previas sin regresión |
| F9-BLD-001 | Gate backend definitivo desde limpio | Maven `clean verify`, Java 17.0.17 | PASS | BUILD SUCCESS 2026-07-20T04:27:26Z | 19 Surefire + 15 Failsafe = 34; 0 failures/errors/skips |

Intento no contabilizado como ejecución: PowerShell fragmentó `-Dit.test=CatalogApiIT` y Maven respondió “Unknown lifecycle phase” antes de cargar pruebas. Se eliminó el selector y los gates completos posteriores sí ejecutaron las suites.

## Ejecuciones de Fase 10

| ID de ejecución | Alcance | Ambiente/herramienta | Estado | Evidencia | Resultado |
|---|---|---|---|---|---|
| F10-UT-001 | Dominio y reglas de `BomService` | JUnit 5 + Mockito, Java 17 | PASS | Surefire XML: 7/0/0/0 | Duplicado, precisión, activos/ausentes, cambio concurrente, revisión y cantidad |
| F10-API-001 | Cuatro operaciones, validación, historia y permisos | HTTP real + Spring Security + MySQL 8.0.42 | PASS | `BomApiIT`: 5 casos funcionales | 201/200/400/401/403/404/409, escala, auditoría e historial |
| F10-DB-001 | Current unique e inmutabilidad de líneas | JPA/JDBC + MySQL trigger | PASS | `BomApiIT` replacement | Una vigente, dos revisiones; `UPDATE bom_items` rechazado |
| F10-CON-001 | Dos publicaciones que observan la misma vigente | HTTP + transactions + latches + MySQL | PASS | `BomApiIT`: 1 caso concurrente | Exactamente un `201`, un `409`, una revisión y una vigente |
| F10-OAS-001 | Paths runtime BOM | Springdoc OpenAPI 3.1 | PASS | Assertion de cuatro método/ruta | Runtime acumulado 19/39 operaciones |
| F10-REG-001 | Infraestructura, seguridad y catálogos previos | Failsafe/Testcontainers MySQL | PASS | Backend 2 + Security 6 + Catalog 7 | 15 integraciones previas sin regresión |
| F10-WRP-001 | Arranque Maven Wrapper Windows tras null-target fix | Maven Wrapper 3.9.11 + host Java 21 | PASS acotado | `.\mvnw.cmd -version` | Wrapper inicia; no se usa como evidencia Java 17 |
| F10-BLD-001 | Gate backend definitivo desde limpio | Maven `clean verify`, Temurin 17.0.17 | PASS | BUILD SUCCESS 2026-07-20T05:30:48Z | 26 Surefire + 22 Failsafe = 48; 0 failures/errors/skips |

Intentos no contabilizados como PASS: `mvnw.cmd test` falló inicialmente al indexar `DirectoryInfo.Target` nulo. Se corrigió el wrapper, se verificó su arranque por separado y el gate funcional definitivo se ejecutó desde limpio dentro del runtime Java 17.

## Trazabilidad de ubicación propuesta en Fase 6

| Entrada | Rutas propietarias futuras | Regla de ubicación |
|---|---|---|
| TM-001 a TM-013 | `backend/src/test/java`, `backend/src/test/resources` | Unitarias, slices e integración Spring/MySQL permanecen dentro del módulo Maven. |
| TM-001 a TM-013 | `tests/postman/` | Contrato/API y combinatoria HTTP se ejecutan desde el harness QA externo. |
| TM-001, TM-002, TM-004, TM-007, TM-008, TM-010, TM-011, TM-018 | `tests/cypress/` | Solo los once journeys aprobados; componente simulado no se etiqueta E2E. |
| TM-004 a TM-013 | `tests/sql/setup/`, `tests/sql/validation/` | Setup/validación externa; nunca reemplaza migraciones Flyway. |
| TM-014 | `tests/locust/locustfile.py` | Workload y resultados contextualizados de Fase 19. |
| TM-015 | `backend/Dockerfile`, `frontend/Dockerfile`, `docker/`, `docker-compose.yml` | Smoke del sistema completo y healthchecks. |
| TM-016 | `.gitlab-ci.yml` | Gates, reports y artifacts con working directories explícitos. |
| TM-017 | `docs/api/openapi.json` | Contrato preliminar que se comparará con Springdoc. |
| TM-018 | `tests/manual/executions/`, `tests/cypress/` | Manual/exploratoria y automatización dirigida según el oracle. |
| TM-019 | `docs/phase-5-test-strategy.md`, `docs/phase-5-test-catalog.md` | Fuentes del diseño QA; los reports runtime no se guardan aquí. |
| TM-020 | `docs/phase-6-repository-structure.md`, `FILE_MANIFEST.md` | Evidencia documental del árbol; no demuestra que las rutas estén materializadas. |

## Trazabilidad contractual de Fase 4

| Entrada | Contrato observable | Evidencia actual/futura |
|---|---|---|
| TM-001/TM-002 | `/auth/login`, `/auth/me`, Bearer, `401/403` uniforme | OpenAPI estático; Spring Security/Newman futuro |
| TM-003 | Cinco operaciones ADMIN de usuarios y concurrencia por versión | OpenAPI estático; integración/API futura |
| TM-004/TM-005 | CRUD lógico de productos/materiales, duplicados `409` | OpenAPI estático; integración/API/E2E futura |
| TM-006 | Publicación y consulta de revisiones BOM | OpenAPI estático; integración/API futura |
| TM-007 | Saldos y movimientos manuales; consumo no expuesto | OpenAPI estático; integración/concurrencia futura |
| TM-008/TM-009 | Orden DRAFT y acciones confirm/start/complete/cancel | OpenAPI estático; integración/API/E2E futura |
| TM-010 | Inspección discriminada APPROVED/REJECTED | OpenAPI estático; integración/API/E2E futura |
| TM-011 | Dashboard discriminado por rol | OpenAPI estático; API/E2E futura |
| TM-012 | `ErrorResponse`, códigos estables y correlación | OpenAPI estático; MockMvc/Newman futuro |
| TM-013 | Consulta ADMIN de eventos de auditoría | OpenAPI estático; integración/API/SQL futura |
| TM-017 | Paridad Markdown/OpenAPI y referencias internas | Node.js, ejecutada en Fase 4 |

## Trazabilidad del modelo de datos de Fase 3

| Entrada | Tablas/constraints principales | Evidencia futura |
|---|---|---|
| TM-001/TM-003 | `users`, username unique, role, active, `auth_version` | Integración de seguridad/MySQL |
| TM-004 | `products`, SKU normalizado unique, active/version | Integración + V-002 |
| TM-005 | `materials`, code normalizado unique, UOM | Integración + V-003 |
| TM-006 | `boms`, `bom_items`, current unique, material unique | Integración + V-005 a V-007 |
| TM-007 | `inventory_balances`, `inventory_movements` y checks | Concurrencia + V-008, V-011, V-012 |
| TM-008 | `production_orders`, status history y snapshot | Integración + V-015 a V-018 |
| TM-009 | `production_order_materials`, reserva agregada y consumo unique | Concurrencia + V-009, V-010, V-013, V-014 |
| TM-010 | `quality_inspections`, order unique y observación check | Integración + V-019 a V-021 |
| TM-013 | `audit_events` e inmutabilidad | Integración + V-023, V-024 |

Los scripts SQL existen, pero su estado sigue siendo **No ejecutada**; una revisión estática no equivale a evidencia MySQL.

## Trazabilidad arquitectónica de Fase 2

| Entrada | Componente/decisión responsable | Dependencia de prueba prevista |
|---|---|---|
| TM-001 | `auth`, Resource Server, BCrypt, JWT, `authVersion` | Unitarias + Spring Security Test + MySQL |
| TM-002 | Security filter chain, `@PreAuthorize`, handlers `401/403` | MockMvc/Newman/Cypress por rol |
| TM-003 | Módulo `user`, optimistic version y auditoría | Unitarias + integración MySQL |
| TM-004/TM-005 | `product`/`material`, DTOs y constraints únicos | Integración MySQL + API/E2E |
| TM-006 | `bom`, validación y snapshot usado por `production` | Unitarias + integración MySQL |
| TM-007/TM-009 | `inventory`, transacciones y locks pesimistas ordenados | MySQL Testcontainers + concurrencia + SQL |
| TM-008 | `production`, lock de orden y máquina de estados | Unitarias + integración + API/E2E |
| TM-010 | `quality` y constraint de inspección única | Integración MySQL + API/E2E |
| TM-011 | `dashboard` mediante fachadas de lectura por rol | API + E2E |
| TM-012 | Correlation filter, Security handlers y Controller Advice | MockMvc + Newman |
| TM-013 | Auditoría append-only y correlación | Integración + API + SQL |
| TM-014 | API desplegada en ambiente documentado | Locust |
| TM-015 | Contenedores `db`, `backend`, `frontend` y healthchecks | Docker Compose smoke |
| TM-016 | Gates GitLab y artifacts | Ejecución de pipeline real |

## Trazabilidad inicial de requisitos

| Entrada de matriz | Requisitos principales | Escenarios representativos |
|---|---|---|
| TM-001 | RF-AUT-001 a RF-AUT-009 | EF-001, EN-001, EN-002, EB-012, EB-013 |
| TM-002 | RF-AUT-004 a RF-AUT-006 y matriz de permisos | EN-003, EN-004, EN-018 |
| TM-003 | RF-USR-001 a RF-USR-009 | EN-019 |
| TM-004 | RF-PRD-001 a RF-PRD-007 | EF-002, EN-005, EB-001, EB-015 |
| TM-005 | RF-MAT-001 a RF-MAT-008 | EF-002, EN-005, EN-020 |
| TM-006 | RF-BOM-001 a RF-BOM-009 | EF-003, EN-007 a EN-009, EB-004, EB-005 |
| TM-007 | RF-INV-001 a RF-INV-012 | EF-004, EN-006, EN-010, EB-002, EB-009 |
| TM-008 | RF-ORD-001 a RF-ORD-015 | EF-005, EF-006, EF-009, EN-011 a EN-014 |
| TM-009 | RF-ORD-005 a RF-ORD-012 y RNF-DAT-001 a RNF-DAT-003 | EB-003, EB-006 a EB-009 |
| TM-010 | RF-QLT-001 a RF-QLT-010 | EF-007, EF-008, EN-015 a EN-017, EB-010 |
| TM-011 | RF-DSH-001 a RF-DSH-005 | EF-010 |
| TM-012 | RF-COM-004 a RF-COM-008 | EN-021 y todos los negativos HTTP |
| TM-013 | RF-COM-001, RF-COM-002 y requisitos auditables por módulo | EF-011 |
| TM-014 | RNF-PER-001 y RNF-PER-002 | Carga a definir en Fase 19 |
| TM-015 | RNF-REL-002 y RNF-POR-001 | Smoke Docker a definir en Fase 20 |
| TM-016 | RNF-CIC-001 | Pipeline a definir en Fase 21 |
| TM-018 | RNF-USA-001/002 y riesgos exploratorios | EX-01 a EX-07, TC-NFR-008 |

| ID | Funcionalidad | Tipo de prueba previsto | Herramienta | Estado | Evidencia | Resultado |
|---|---|---|---|---|---|---|
| TM-001 | Login y validación de JWT | Unitaria, integración, API, E2E | JUnit 5, Mockito, Postman/Newman, Cypress | Planificada | No disponible | No ejecutada |
| TM-002 | Roles, permisos, `401` y `403` | Integración, API, E2E | Spring Test, Postman/Newman, Cypress | Planificada | No disponible | No ejecutada |
| TM-003 | Usuarios | Unitaria, integración, API | JUnit 5, Mockito, Spring Test, Postman | Planificada | No disponible | No ejecutada |
| TM-004 | Productos y SKU único | Unitaria, integración, API, E2E, SQL | JUnit 5, Postman/Newman, Cypress, MySQL | Planificada | No disponible | No ejecutada |
| TM-005 | Materiales y código único | Unitaria, integración, API, E2E, SQL | JUnit 5, Postman/Newman, Cypress, MySQL | Planificada | No disponible | No ejecutada |
| TM-006 | BOM no vacía y cantidades válidas | Unitaria, integración, API, SQL | JUnit 5, Spring Test, Postman/Newman, MySQL | Parcial ejecutada | `BomServiceTest` 5 + `BomTest` 2 + `BomApiIT` 7 PASS | Backend/MySQL/API ejecutados; Newman y validación SQL externa F18 pendientes |
| TM-007 | Inventario, movimientos y no negatividad | Unitaria, integración, API, E2E, concurrencia, SQL | JUnit 5, Spring Test, Postman/Newman, Cypress, MySQL | Parcial ejecutada; rerun requerido | 8 unitarias PASS; `InventoryApiIT` último run útil 8 PASS + 1 expectativa corregida; SQL creado no ejecutado como script completo | Código/tests existen, pero integración actual y gate Java 17 todavía no pueden declararse PASS |
| TM-008 | Órdenes y transiciones de estado | Unitaria, integración, API, E2E, SQL | JUnit 5, Spring Test, Postman/Newman, Cypress, MySQL | Planificada | No disponible | No ejecutada |
| TM-009 | Consumo transaccional de materiales | Integración, API, concurrencia, SQL | Spring Test, Postman/Newman, MySQL | Planificada | No disponible | No ejecutada |
| TM-010 | Inspecciones, aprobación y rechazo | Unitaria, integración, API, E2E, SQL | JUnit 5, Spring Test, Postman/Newman, Cypress, MySQL | Planificada | No disponible | No ejecutada |
| TM-011 | Dashboard | API, componente/E2E | Postman/Newman, Cypress | Planificada | No disponible | No ejecutada |
| TM-012 | Contrato de errores `400/404/409/5xx` | Unitaria, integración, API | JUnit 5, Spring Test, Postman/Newman | Parcial | `GlobalExceptionHandlerTest`: 4 PASS | Infraestructura `400/404/500` y correlación conforme; `409`, seguridad y endpoints reales pendientes |
| TM-013 | Auditoría y trazabilidad | Integración, API, SQL | Spring Test, Postman/Newman, MySQL | Parcial | `BackendInfrastructureIT`: evento JSON + trigger PASS | Persistencia base conforme; actores/acciones de negocio/API pendientes |
| TM-014 | Rendimiento de flujos seleccionados | Carga | Locust | Planificada | No disponible | No ejecutada |
| TM-015 | Arranque y salud de contenedores | Smoke/integración de sistema | Docker Compose, scripts de salud | Parcial | F7-SMK-001 backend+DB Java 17 | Health/DB/Swagger PASS; frontend, Dockerfiles y Compose pendientes de Fase 20 |
| TM-016 | Pipeline y publicación de evidencias | CI/CD | GitLab CI | Planificada | No disponible | No ejecutada |
| TM-017 | Integridad del contrato OpenAPI | Contrato/estática/runtime | Node.js, springdoc | Parcial ejecutada | Diseño: 28 rutas/39 operaciones/71 schemas/538 refs; runtime OpenAPI 3.1/título/bearer scheme PASS | Auditoría estática conforme y base Springdoc viva; paridad de rutas pendiente tras Fases 8–13 |
| TM-018 | Usabilidad, accesibilidad básica y exploración | Manual, componente, E2E dirigido | Navegador, DevTools, Cypress cuando aplique | Planificada | Catálogo Fase 5 | No ejecutada |
| TM-019 | Integridad estática de estrategia y catálogo | Contrato/estática | Node.js | Ejecutada | 98 casos únicos/continuos, 151 referencias resueltas, RN-001–RN-036 cubiertas, 11 E2E, 16 secciones | Conforme documentalmente; no demuestra ninguna funcionalidad runtime |
| TM-020 | Integridad estática de estructura propuesta | Estructura/estática | PowerShell | Ejecutada | 19 archivos existentes inventariados; rutas obligatorias, ownership y autoridades revisadas; `.git` ausente | Conforme documentalmente; no creó carpetas, código, repositorio ni evidencia runtime |
| TM-021 | Inicialización técnica del backend | Build, web slice, integración, smoke | Maven, JUnit 5, MockMvc, Testcontainers, MySQL, Docker | Ejecutada | 4 Surefire + 2 Failsafe PASS; build/smoke PASS bajo Java 17 | Fase 7 técnicamente conforme; no demuestra seguridad, endpoints de dominio, frontend ni 98 casos funcionales |
