# FASE 6 — ESTRUCTURA DEL REPOSITORIO

## 1. Nombre de la fase

**Fase 6 — Diseño del monorepo, propiedad de artefactos y preparación Git.**

### Inicio obligatorio de continuidad

#### Resumen del estado anterior

- Las Fases 0–5 están completadas y aprobadas.
- Existen documentos de alcance, requisitos, arquitectura, modelo, API y estrategia QA.
- OpenAPI tiene 39 operaciones auditadas estáticamente.
- Hay 98 casos diseñados y todos siguen `NOT_RUN`.
- No existe backend, frontend, suites ejecutables, Docker, pipeline o repositorio Git inicializado.

#### Decisiones que siguen vigentes

- Monolito modular Spring Boot y SPA React, desplegados con MySQL.
- Package-by-feature, DTOs separados, Flyway como autoridad y JPA `validate`.
- MySQL Testcontainers para integración; Newman, Cypress, Locust y SQL como suites de sistema.
- Datos de test aislados, sin endpoints productivos de reset.
- Documentos de control en raíz y compuerta de aprobación entre fases.

#### Archivos existentes utilizados

- Los siete documentos de continuidad en raíz.
- `docs/phase-0-discovery.md` a `docs/phase-5-test-strategy.md`.
- `docs/phase-5-test-catalog.md`.
- `docs/api/openapi.json`.
- `docs/database/phase-3-*.sql`.

#### Objetivo de la fase actual

Definir antes del scaffold dónde vive cada responsabilidad, cuál archivo será autoridad, qué se versiona, qué se genera y cómo se materializará Git sin incluir contenidos ajenos de `D:\`.

#### Criterios para considerar terminada la fase

1. Árbol objetivo completo y coherente con las Fases 2, 4 y 5.
2. Cada carpeta tiene propósito, propietario y fase de creación.
3. Backend, frontend y pruebas no duplican autoridades.
4. Migraciones, fixtures, SQL de validación y documentación tienen ubicación inequívoca.
5. Archivos raíz, secretos, reportes y artefactos generados tienen política explícita.
6. Git se inicializa únicamente dentro de `D:\qaflow` después de aprobación.
7. Rutas planificadas quedan registradas en `FILE_MANIFEST.md`.
8. No se genera todavía Spring Boot, React, Docker o suites.
9. El usuario aprueba avanzar.

## 2. Objetivo

Diseñar un monorepo fácil de ejecutar, probar, revisar en entrevista y mantener en CI. La estructura debe favorecer navegación por capacidad, reproducibilidad y separación entre código, pruebas, documentación y artefactos generados.

## 3. Dependencias

- Arquitectura modular aprobada en Fase 2.
- Modelo y scripts SQL aprobados en Fase 3.
- Contrato OpenAPI aprobado en Fase 4.
- Estrategia, catálogo, gates y evidencias aprobados en Fase 5.
- Orden de implementación del `ROADMAP.md`.

## 4. Entradas requeridas

No se requiere otra decisión para presentar la propuesta. Se respetan las rutas obligatorias del brief:

- `backend/`
- `frontend/`
- `tests/postman/`
- `tests/cypress/`
- `tests/locust/`
- `tests/sql/`
- `tests/manual/`
- `tests/defects/`
- `docs/`
- `docker/`
- `docker-compose.yml`
- `.gitlab-ci.yml`
- Los siete documentos de continuidad y `README.md` en raíz.

## 5. Entregables

1. Árbol objetivo del monorepo.
2. Explicación de cada carpeta.
3. Estructura interna prevista de backend y frontend.
4. Ubicación y clasificación de pruebas.
5. Autoridad de migraciones/SQL/fixtures.
6. Propiedad de Docker, Compose y GitLab CI.
7. Convenciones de nombres, wrappers, lockfiles y archivos raíz.
8. Política `.gitignore`, secretos, reports y evidencias.
9. Flujo Git propuesto y comandos seguros de materialización.
10. Registro de archivos planificados y dependencias.

## 6. Decisiones tomadas

Estas decisiones quedan **propuestas en Fase 6** y entrarán en vigor al aprobarla:

1. Un monorepo `qaflow` contiene aplicación, pruebas de sistema, infraestructura y documentación.
2. Backend usa package-by-feature; no se crea una jerarquía global controller/service/repository.
3. Tests Java viven en `backend/src/test`; pruebas de sistema viven en `tests/`.
4. Flyway en backend será la autoridad ejecutable del schema; SQL documental y validación permanecen separados.
5. Dockerfiles viven junto a sus contextos; `docker/` solo guarda configuración compartida.
6. Documentos de continuidad permanecen en raíz y documentos de fase permanecen en `docs/`.
7. Outputs generados nunca se versionan; evidencia seleccionada debe copiarse sanitizada a documentación.
8. Maven Wrapper, lockfiles npm y dependencias Python fijadas se versionan para reproducibilidad.
9. Git se inicializa y verifica en `D:\qaflow`, después de aprobación y después de crear `.gitignore`; no se ejecuta `git add`/commit automáticamente.

## 7. Diseño o código

No se crea código ni carpetas vacías. El siguiente árbol representa el estado objetivo y su fase propietaria.

### 7.1 Estado actual frente a estado objetivo

Estado actual real:

```text
qaflow/
├── docs/
├── PROJECT_STATE.md
├── ROADMAP.md
├── DECISIONS.md
├── FILE_MANIFEST.md
├── TEST_MATRIX.md
├── OPEN_ISSUES.md
└── INTERVIEW_NOTES.md
```

Solo existen 18 archivos documentales/diseño antes de crear este entregable. Cualquier ruta adicional mostrada abajo está **planificada**.

### 7.2 Árbol objetivo propuesto para materialización futura

```text
qaflow/
├── backend/                                  # Fases 7–13, 15 y 20
│   ├── .mvn/
│   │   └── wrapper/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/qaflow/
│   │   │   │   ├── QaflowApplication.java
│   │   │   │   ├── shared/
│   │   │   │   ├── auth/
│   │   │   │   ├── user/
│   │   │   │   ├── product/
│   │   │   │   ├── material/
│   │   │   │   ├── bom/
│   │   │   │   ├── inventory/
│   │   │   │   ├── production/
│   │   │   │   ├── quality/
│   │   │   │   ├── dashboard/
│   │   │   │   └── audit/
│   │   │   └── resources/
│   │   │       ├── db/migration/
│   │   │       ├── application.yml
│   │   │       ├── application-local.yml
│   │   │       └── application-docker.yml
│   │   └── test/
│   │       ├── java/com/qaflow/
│   │       └── resources/
│   │           ├── db/testdata/
│   │           └── application-test.yml
│   ├── .dockerignore
│   ├── Dockerfile                            # Se agrega en Fase 20
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── frontend/                                 # Fases 14 y 20
│   ├── public/
│   ├── src/
│   │   ├── app/
│   │   ├── auth/
│   │   ├── features/
│   │   │   ├── users/
│   │   │   ├── products/
│   │   │   ├── materials/
│   │   │   ├── bom/
│   │   │   ├── inventory/
│   │   │   ├── production/
│   │   │   ├── quality/
│   │   │   ├── dashboard/
│   │   │   └── audit/
│   │   ├── api/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── utils/
│   │   ├── styles/
│   │   └── main.jsx
│   ├── .dockerignore
│   ├── Dockerfile                            # Se agrega en Fase 20
│   ├── index.html
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.js
│
├── tests/                                    # Harnesses externos al código
│   ├── postman/                              # Fase 16
│   │   ├── QAFlow.postman_collection.json
│   │   ├── QAFlow.local.postman_environment.json
│   │   └── README.md
│   ├── cypress/                              # Fase 17
│   │   ├── e2e/
│   │   ├── fixtures/
│   │   ├── support/
│   │   ├── cypress.config.js
│   │   └── README.md
│   ├── locust/                               # Fase 19
│   │   ├── locustfile.py
│   │   ├── requirements.txt
│   │   └── README.md
│   ├── sql/                                  # Fase 18
│   │   ├── setup/
│   │   ├── validation/
│   │   └── README.md
│   ├── manual/                               # Fases 5, 14 y 22
│   │   ├── executions/
│   │   └── README.md
│   ├── defects/                              # Fase 22
│   │   ├── evidence/
│   │   └── README.md
│   ├── package.json                          # Newman + Cypress scripts
│   └── package-lock.json
│
├── docs/
│   ├── api/
│   │   └── openapi.json
│   ├── database/                             # Diseño histórico, no runtime
│   ├── evidence/                             # Evidencia seleccionada/sanitizada
│   └── phase-*.md
│
├── docker/                                   # Fase 20; config compartida
│   ├── nginx/
│   │   └── default.conf
│   └── README.md
│
├── .editorconfig
├── .env.example
├── .gitattributes
├── .gitignore
├── .gitlab-ci.yml
├── docker-compose.yml
├── README.md
├── PROJECT_STATE.md
├── ROADMAP.md
├── DECISIONS.md
├── FILE_MANIFEST.md
├── TEST_MATRIX.md
├── OPEN_ISSUES.md
└── INTERVIEW_NOTES.md
```

Las carpetas vacías no se versionan con `.gitkeep` indiscriminado. Se crean cuando su primer archivo real aparece; un `README.md` se usa solo cuando documenta una interfaz/ejecución concreta.

### 7.3 Responsabilidad por carpeta

| Ruta | Responsabilidad | No debe contener | Fase que la materializa |
|---|---|---|---|
| `backend/` | Aplicación Spring Boot, migraciones y pruebas Java | Colecciones Postman, Cypress, outputs de build | 7–13, 15, 20 |
| `frontend/` | React/Vite, assets y pruebas de componente si se aprueban | Cypress de sistema, secretos, build `dist` versionado | 14, 20 |
| `tests/postman/` | Colección/environment sin secretos y guía Newman | JWT/IDs persistentes, HTML reports versionados | 16 |
| `tests/cypress/` | Once journeys, fixtures y support | Unitarias backend, node_modules, videos/screenshots versionados | 17 |
| `tests/locust/` | Workloads y dependencias Python fijadas | Resultados que se presenten sin ambiente | 19 |
| `tests/sql/` | Setup QA y consultas ejecutables de validación | Migraciones productivas | 18 |
| `tests/manual/` | Registros de ejecución manual y charters | Casos sin build/ambiente/estado | 14, 22 |
| `tests/defects/` | Defectos reales y evidencia sanitizada | Defectos inventados, tokens o dumps sensibles | 22 |
| `docs/` | Decisiones narrativas, contrato, diseño y evidencia curada | Reports masivos/generados o fuente ejecutable duplicada | Todas |
| `docker/` | Configuración compartida Nginx/helper documentado | Código backend/frontend o secretos | 20 |
| Raíz | Orquestación, CI, entrada documental y continuidad | Código de feature suelto | 6, 20, 21, 23 |

### 7.4 Estructura backend por funcionalidad

Cada módulo usa solo las capas necesarias:

```text
com.qaflow.inventory/
├── api/              controllers, request/response DTOs, mapper HTTP
├── application/      casos de uso y transacciones
├── domain/           reglas, enums y modelos de dominio
└── infrastructure/   JPA entities, repositories y adapters
```

Reglas:

- No crear paquetes globales `controller/`, `service/`, `repository/` que mezclen módulos.
- `shared/` solo admite configuración/errores/seguridad/auditoría realmente transversales; no es un cajón genérico.
- Un módulo puede omitir una capa/carpeta si no tiene contenido real.
- Dependencias entre módulos cruzan interfaces de aplicación/dominio acordadas, no repositorios ajenos.
- `QaflowApplication.java` permanece en `com.qaflow` para component scanning controlado.

Pruebas Java:

- `backend/src/test/java` refleja el package de producción.
- `*Test` identifica unitarias/slices ejecutadas por Surefire.
- `*IT` identifica integración MySQL ejecutada por Failsafe o perfil Maven explícito.
- Fixtures/builders Java viven cerca de tests o en soporte de test, nunca en `src/main`.
- `application-test.yml` no contiene credenciales reales.

### 7.5 Estructura frontend por funcionalidad

| Ruta | Propósito |
|---|---|
| `src/app/` | Router, providers, layout y bootstrap. |
| `src/auth/` | Context/reducer, guards, login y sesión. |
| `src/features/<module>/` | Pages, components, hooks y API adapters específicos. |
| `src/api/` | Axios client, interceptors y traducción común de errores. |
| `src/components/` | Controles compartidos con uso real en varios módulos. |
| `src/hooks/` | Hooks genéricos; no lógica de dominio dispersa. |
| `src/utils/` | Funciones puras, incluida precisión/formato controlado. |
| `src/styles/` | Bootstrap overrides y estilos globales. |

Si se aprueba una herramienta de componente en Fase 14, los tests se colocan junto al componente (`*.test.jsx`) o en una carpeta local `__tests__`, con una sola convención documentada. Los E2E permanecen fuera del frontend en `tests/cypress`.

### 7.6 Organización de pruebas de sistema

#### Node QA harness

`tests/package.json` centraliza scripts de Newman/Cypress para evitar otro package raíz y permite:

```text
npm run test:api
npm run test:e2e
```

Los nombres son diseño; los scripts reales se agregan en Fases 16–17. `tests/package-lock.json` se versiona y CI usa `npm ci`.

#### Postman

- Collection y environment no contienen passwords/tokens persistidos.
- Variables sensibles se inyectan en runtime/CI.
- IDs/versiones/runId se capturan durante la ejecución.
- Reports se generan fuera de Git.

#### Cypress

- `e2e/` contiene exactamente los journeys aprobados, organizados por flujo y no por página arbitraria.
- `support/` contiene comandos y setup; `fixtures/` solo datos no sensibles.
- `cypress.config.js` vive con el harness.
- `screenshots/`, `videos/`, downloads y reports son generados/ignorados.

#### Locust

- `requirements.txt` fija versión de Locust.
- El workload puede dividirse en módulos solo cuando crezca; el entrypoint sigue siendo `locustfile.py`.
- CSV/HTML de resultados se publican como artifacts, no se versionan completos.

#### Manual y defectos

- El catálogo fuente sigue en `docs/phase-5-test-catalog.md`.
- `tests/manual/executions/` guarda registros por build/run cuando la fase los cree.
- `tests/defects/` contiene un Markdown por defecto real y evidencia sanitizada pequeña.

### 7.7 Autoridad de base de datos y SQL

| Artefacto | Ruta | Autoridad |
|---|---|---|
| Migraciones runtime | `backend/src/main/resources/db/migration/V*.sql` | Única fuente ejecutable de schema/productivo desde Fase 7 |
| Fixture integración Java | `backend/src/test/resources/db/testdata/` | Solo tests Spring/Testcontainers |
| Setup/validación externa | `tests/sql/setup/`, `tests/sql/validation/` | Pruebas SQL independientes de API en Fase 18 |
| Diseño aprobado histórico | `docs/database/phase-3-*.sql` | Evidencia de diseño; no se carga automáticamente |

Reglas:

- Fase 7 transforma el esquema aprobado en `V1__initial_schema.sql`; no mueve ni borra silenciosamente el diseño de Fase 3.
- Docker no usa `docker-entrypoint-initdb.d` para duplicar el schema; el backend/Flyway migra.
- Una corrección al schema se añade como migración nueva después de versionarse; no se reescribe una migración aplicada.
- Fixtures demo/test nunca forman parte de una migración productiva.
- SQL de `tests/sql` puede leer/validar y mutar solo schemas desechables cuando el caso lo exige.

### 7.8 Docker y contextos de build

- `backend/Dockerfile` usa `backend/` como contexto y multi-stage Maven/JRE.
- `frontend/Dockerfile` usa `frontend/` como contexto y multi-stage Vite/Nginx.
- `.dockerignore` por contexto excluye target, node_modules, reports, IDE y secretos.
- `docker/nginx/default.conf` contiene proxy/routing compartido que no pertenece al source React.
- `docker-compose.yml` en raíz orquesta `db`, `backend`, `frontend`, red, volumen, variables y healthchecks.
- `.env.example` documenta nombres; `.env` real se ignora.

### 7.9 GitLab CI y reports

`.gitlab-ci.yml` permanece en raíz porque orquesta todo el monorepo. Los jobs usan working directories explícitos:

| Job futuro | Working directory/entrada | Artifact principal |
|---|---|---|
| backend validate/build/unit/integration | `backend/` | JUnit XML, JaCoCo, JAR |
| frontend build | `frontend/` | Build/lint reports, `dist` como artifact temporal |
| api-test | `tests/` + Compose raíz | Newman JUnit/HTML |
| e2e-test | `tests/` + Compose raíz | Cypress JUnit, screenshot/video solo en fallo |
| performance-test | `tests/locust/` | CSV/HTML contextualizado |
| package | Dockerfiles + Compose | Imágenes/metadata según runner |

Artifacts CI tienen expiración configurada y no se agregan a Git. Evidencia pequeña seleccionada para entrevista se sanitiza y copia expresamente a `docs/evidence/` con contexto.

### 7.10 Archivos raíz

| Archivo | Propósito | Estado actual |
|---|---|---|
| `.gitignore` | Excluir secretos, dependencias, builds, reports e IDE | Planificado para materialización tras aprobación |
| `.gitattributes` | Normalizar texto LF y excepciones de scripts Windows/binarios | Planificado |
| `.editorconfig` | Charset UTF-8, newline final y espacios consistentes | Planificado |
| `.env.example` | Catálogo de variables sin valores sensibles | Planificado para Fases 7/20 |
| `docker-compose.yml` | Stack reproducible | Planificado para Fase 20 |
| `.gitlab-ci.yml` | Pipeline | Planificado para Fase 21 |
| `README.md` | Entrada final de instalación/demo | Esqueleto mínimo al materializar; cierre en Fase 23 |
| Documentos de continuidad | Estado/roadmap/decisiones/manifiesto/tests/issues/entrevista | Existentes; permanecen en raíz |

No se propone un `package.json` o `pom.xml` en raíz: el backend no es multi-módulo Maven y los dos ecosistemas Node tienen ownership separado (`frontend/` y `tests/`).

### 7.11 Política de archivos generados e ignorados

Patrones mínimos de `.gitignore` futuro:

```text
.env
.env.*
!.env.example
**/target/
**/node_modules/
frontend/dist/
**/coverage/
**/reports/
tests/cypress/screenshots/
tests/cypress/videos/
tests/cypress/downloads/
tests/locust/*.csv
tests/locust/*.html
*.log
.idea/
.vscode/
.classpath
.project
.settings/
.DS_Store
Thumbs.db
```

La implementación ajustará excepciones verificadas. Nunca se ignora una migración, lockfile, colección, configuración de test o documento fuente requerido para reproducir el proyecto.

### 7.12 Convenciones Git

- Repositorio único, rama principal `main`.
- Ramas cortas: `feature/<tema>`, `fix/<tema>`, `test/<tema>`, `docs/<tema>`.
- Commits: `type(scope): descripción`, por ejemplo `feat(auth): add jwt login`.
- Tipos mínimos: `feat`, `fix`, `test`, `docs`, `build`, `ci`, `chore`, `refactor`.
- Un commit no mezcla cambios no relacionados ni reportes generados.
- No se reescribe historia compartida ni se ejecuta force-push como parte del flujo asistido.
- No se crea commit/tag/release sin solicitud o aprobación explícita.

Secuencia segura futura, **no ejecutada en esta fase**:

```powershell
Set-Location -LiteralPath 'D:\qaflow'
git init
git rev-parse --show-toplevel
git status --short
```

Antes de `git init`, se materializan `.gitignore`, `.gitattributes` y `.editorconfig`. Después se verifica que `--show-toplevel` resuelva exactamente `D:/qaflow`. No se usa `git init` en `D:\` ni se hace staging automático.

### 7.13 Line endings y reproducibilidad

- Texto fuente: UTF-8, newline final, LF en repositorio.
- `*.cmd`/`*.bat`: CRLF si el wrapper lo requiere.
- Imágenes/binarios: `-text` en `.gitattributes`.
- Maven Wrapper y sus propiedades/JAR aprobados se versionan.
- `package-lock.json` de frontend/tests se versiona; CI usa `npm ci`.
- `tests/locust/requirements.txt` fija versiones exactas o rango controlado documentado.
- Imágenes Docker usan tags/digests definidos posteriormente; no `latest` silencioso para evidencia final.

### 7.14 Dependencias entre áreas

```text
docs/api/openapi.json
          |
          +--> backend API/Swagger
          +--> frontend Axios/DTO expectations
          +--> tests/postman contract assertions

backend Flyway migrations --> MySQL <-- tests/sql validation
          |
          +--> backend integration tests

docker-compose.yml
          +--> backend + frontend + db
          +--> Newman + Cypress + Locust

.gitlab-ci.yml --> builds/tests above --> generated artifacts
```

No se permite que frontend dependa de clases Java, que backend lea fixtures Cypress, o que una colección Postman se convierta en autoridad del contrato por encima de OpenAPI.

### 7.15 Mapeo de artefactos actuales

| Artefacto actual | Destino/decisión |
|---|---|
| Siete documentos de continuidad | Permanecen en raíz. |
| `docs/phase-*.md` | Permanecen; no se reubican ni renombran. |
| `docs/api/openapi.json` | Permanece como contrato de diseño; Springdoc se compara con él. |
| `docs/database/phase-3-initial-schema.sql` | Permanece histórico; Fase 7 crea migración Flyway derivada y trazada. |
| `docs/database/phase-3-test-data.sql` | Permanece diseño; fixtures ejecutables se separan por ambiente. |
| `docs/database/phase-3-validation.sql` | Permanece diseño; Fase 18 crea suite ejecutable en `tests/sql/validation`. |
| Catálogo/estrategia Fase 5 | Permanecen en docs; futuras suites referencian sus IDs. |

### 7.16 Archivos planificados por fase

| Ruta principal | Fase propietaria | Dependencia |
|---|---:|---|
| `.gitignore`, `.gitattributes`, `.editorconfig`, `README.md` mínimo | 7, antes del scaffold | Fase 6 aprobada |
| `backend/pom.xml`, wrappers, source/resources/test roots | 7 | Estructura aprobada |
| `backend/src/main/resources/db/migration/V1__initial_schema.sql` | 7 | SQL/modelo Fase 3 |
| Backend features/seguridad | 8–13 | Backend base + API/modelo |
| `frontend/package*.json`, Vite y `src/` | 14 | API funcional |
| Tests Java finales/report config | 15 | Funcionalidades implementadas |
| `tests/package*.json`, `tests/postman/*` | 16 | API desplegable |
| `tests/cypress/*` | 17 | Frontend funcional + data-cy |
| `tests/sql/*` | 18 | Schema/dataset implementados |
| `tests/locust/*` | 19 | API estable |
| Dockerfiles, `docker/*`, `.env.example`, Compose | 20 | Aplicación completa |
| `.gitlab-ci.yml` | 21 | Comandos de build/test estables |
| `tests/defects/*` | 22 | Defectos reales observados |
| README final y evidencia curada | 23 | Stack y suites ejecutados |

### 7.17 Checklist de aceptación de estructura

- [x] Rutas obligatorias del brief representadas.
- [x] Java unit/integration dentro del módulo Maven.
- [x] Postman/Cypress/Locust/SQL separados como harnesses de sistema.
- [x] Flyway identificado como única autoridad runtime de schema.
- [x] Documentos existentes conservados sin renombrar.
- [x] Dockerfiles junto al contexto y Compose/CI en raíz.
- [x] Secretos, reports, dependencias y builds excluidos del control de versiones.
- [x] Wrappers/lockfiles incluidos para reproducibilidad.
- [x] Git limitado a `D:\qaflow` y staging/commit fuera de automatismo.
- [x] Aprobación del usuario.
- [ ] Materialización real del árbol.

## 8. Archivos creados

- `docs/phase-6-repository-structure.md`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-5-test-strategy.md` (solo registro de aprobación)

## 10. Comandos

No se ejecutó `git init`, no se crearon carpetas vacías y no se generó Spring Boot/React. Se inspeccionó el árbol real y se ejecutó una auditoría estática de rutas/manifiesto al cerrar la fase.

Resultado estático: 19 archivos reales y 19 entradas existentes en manifiesto; 16 secciones obligatorias; todas las rutas requeridas representadas; 66 decisiones sin IDs duplicados; cero raíces `backend/frontend/tests/docker/.git` materializadas; cero textos obsoletos de aprobación de Fase 5 y cero hallazgos de espacios finales.

Los comandos de 7.12 son planificados y requieren aprobación de esta fase antes de ejecutarse.

## 11. Cómo verificar

1. Comparar el árbol con todas las rutas exigidas en el brief.
2. Confirmar que cada ruta planificada tiene fase/propósito/autoridad.
3. Confirmar que archivos actuales siguen en su ubicación y manifiesto.
4. Confirmar que no existe backend/frontend/tests/docker real ni `.git` dentro de `qaflow`.
5. Confirmar que migraciones, SQL de test y SQL documental no se confunden.
6. Confirmar que outputs/secrets se excluyen y wrappers/lockfiles se versionan.
7. Tras aprobación, materializar primero archivos raíz seguros y verificar Git toplevel antes de staging.

## 12. Pruebas relacionadas

No se ejecutaron pruebas funcionales/runtime. La estructura asigna destinos futuros a TM-001–TM-019 y a los 98 casos, pero sus estados no cambian. TM-020 registra únicamente la auditoría documental de rutas/ownership.

## 13. Riesgos

- Crear todo el árbol de una vez produciría directorios/README sin uso y manifiesto ruidoso.
- Dos `package-lock.json` son intencionales, pero deben actualizarse desde su working directory correcto.
- El harness Node de tests añade otra dependencia que CI deberá cachear/asegurar.
- Evidencia curada en Git puede filtrar datos si no se sanitiza/revisa.
- La migración V1 podría divergir del SQL histórico si no se compara estáticamente y contra MySQL.
- Una herramienta React adicional sigue pendiente; no se añade a la estructura como hecho.
- Git aún no está inicializado; materializar fuera de `D:\qaflow` podría incluir archivos ajenos.
- La estructura propuesta aún no prueba compilación, Docker o pipeline.

## 14. Estado actualizado

- Fases 0–5: completadas y aprobadas.
- Fase 6: completada y aprobada el 2026-07-19; auditoría estática conforme.
- Árbol materializado: no.
- Git inicializado: no.
- Backend/frontend/tests ejecutables: no.

## 15. Próximo paso

Tras aprobación, iniciar la Fase 7: materializar la estructura mínima aprobada, crear primero archivos raíz seguros, inicializar/verificar Git en `D:\qaflow` y generar el proyecto Spring Boot con configuración, perfiles, MySQL, errores, Swagger, auditoría y capas; después compilar/iniciar/verificar con evidencia real.

## 16. Registro de aprobación

Fase 6 aprobada explícitamente por el usuario el 2026-07-19. La aprobación autoriza iniciar la Fase 7 y materializar la estructura mínima; no constituye evidencia de compilación o funcionamiento.
