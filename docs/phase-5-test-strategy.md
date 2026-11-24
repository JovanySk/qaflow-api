# FASE 5 — ESTRATEGIA DE PRUEBAS

## 1. Nombre de la fase

**Fase 5 — Estrategia integral de QA, automatización y evidencias.**

### Inicio obligatorio de continuidad

#### Resumen del estado anterior

- Las Fases 0–4 están completadas y aprobadas.
- El alcance, requisitos, permisos, arquitectura, modelo relacional y contrato HTTP están vigentes.
- El OpenAPI preliminar tiene 28 rutas, 39 operaciones y 71 schemas; solo fue auditado estáticamente.
- Los scripts SQL no se han ejecutado contra MySQL.
- No existe código backend/frontend ni suites de prueba ejecutables.

#### Decisiones que siguen vigentes

- Backend Spring Boot modular, React SPA y MySQL como motor real.
- Flyway gobierna el esquema y JPA usa validación, no generación automática.
- Seguridad por JWT breve, `authVersion` y permisos aplicados en backend.
- Reserva al confirmar, liberación al cancelar y consumo al completar bajo transacciones/locks.
- OpenAPI `/api/v1`, errores correlacionados y precisión pública mediante strings.
- Pruebas con mocks no se etiquetan como integración; persistencia se prueba con MySQL, no H2.

#### Archivos existentes utilizados

- `docs/phase-1-requirements.md`
- `docs/phase-2-architecture.md`
- `docs/phase-3-data-model.md`
- `docs/phase-4-api-contract.md`
- `docs/api/openapi.json`
- Los tres scripts SQL de Fase 3.
- Los siete documentos de continuidad.

#### Objetivo de la fase actual

Definir qué se probará, en qué nivel, con qué datos, ambiente, herramienta, prioridad, evidencia y criterio de salida, antes de implementar las suites.

#### Criterios para considerar terminada la fase

1. Todos los requisitos y riesgos críticos tienen cobertura planificada.
2. Casos positivos, negativos, borde, concurrencia y permisos están catalogados.
3. Cada tipo de prueba tiene alcance y herramienta inequívocos.
4. Existen criterios explícitos para automatizar o conservar manual.
5. Regresión, defectos, severidad, prioridad y evidencias están definidos.
6. Rendimiento tiene carga y umbrales provisionales contextualizados.
7. Ninguna prueba se marca ejecutada sin evidencia real.
8. Matriz, decisiones, manifiesto, incidencias y estado quedan actualizados.
9. El usuario aprueba avanzar.

## 2. Objetivo

Crear una estrategia de QA defendible en entrevista y utilizable durante las Fases 7–21. Debe encontrar fallos relevantes con la capa más rápida y confiable que pueda demostrar el riesgo, evitando duplicación innecesaria entre JUnit, Newman y Cypress.

## 3. Dependencias

- Requisitos RF/RNF, reglas RN, escenarios EF/EN/EB y matriz de permisos aprobados.
- Modelo de datos, invariantes, triggers y consultas V-001 a V-027.
- Contrato de 39 operaciones, DTOs, códigos de error y correlación.
- Arquitectura transaccional, de seguridad, frontend, contenedores y pipeline.
- Fases de implementación 7–14 para materializar componentes que todavía no existen.

## 4. Entradas requeridas

No se requiere una decisión externa adicional. Se adoptan como objetivos provisionales ya aprobados:

- Lecturas comunes p95 ≤ 500 ms.
- Escrituras comunes p95 ≤ 1 s.
- 20 usuarios concurrentes.
- Todas las colecciones paginadas.
- Cero saldos negativos, consumos duplicados o cambios parciales.
- Los once flujos Cypress obligatorios del brief.

## 5. Entregables

1. Estrategia general y basada en riesgos.
2. Niveles de prueba y responsabilidades.
3. Ambientes, datos y política de aislamiento.
4. Criterios de entrada, salida y suspensión.
5. Diseño de pruebas unitarias, integración, API, E2E, SQL y rendimiento.
6. Catálogo manual reproducible en `docs/phase-5-test-catalog.md`.
7. Checklist de smoke y regresión.
8. Plantilla de defectos.
9. Modelo independiente de prioridad y severidad.
10. Criterios de automatización, mantenimiento y flaky tests.
11. Matriz viva actualizada con trazabilidad de casos.

## 6. Decisiones tomadas

Estas decisiones entraron en vigor con la aprobación explícita de la Fase 5 el 2026-07-19:

1. Priorizar por impacto/riesgo y demostrar cada regla en la capa más baja adecuada; no imponer una cuota artificial por nivel.
2. Usar Mockito solo para aislamiento unitario y MySQL Testcontainers para persistencia/transacciones.
3. Reservar Cypress para once journeys de negocio; validar combinaciones exhaustivas en unitarias/API.
4. No crear endpoints productivos de reset/seed; cada suite usa ambiente efímero o fixture controlado.
5. Tratar una repetición flaky como defecto de prueba/producto, no como éxito por retry.
6. Separar prioridad de ejecución y severidad del impacto.
7. Hacer trazable toda evidencia a build, commit, ambiente, caso y correlation ID, sin secretos.
8. Usar umbrales JaCoCo como señal/gate complementario, nunca como sustituto de assertions útiles.
9. Ejecutar rendimiento en ambiente dedicado; no comparar métricas obtenidas en runners compartidos como si fueran equivalentes.
10. Mantener exploración manual para usabilidad, accesibilidad visual y riesgos nuevos no cubiertos por casos deterministas.

## 7. Diseño o código

No se implementan pruebas en esta fase. Se define su diseño.

### 7.1 Alcance QA

Incluido:

- Reglas funcionales y permisos de los nueve módulos.
- Contrato HTTP, schemas, errores, paginación y correlación.
- Persistencia, restricciones, triggers, migraciones e integridad.
- Transacciones, rollback, locking optimista/pesimista y concurrencia.
- UI funcional, rutas protegidas, token, formularios y mensajes.
- Arranque/health, Docker, pipeline, rendimiento local y evidencias.
- Configuración segura básica: secretos, headers/token, CORS y exposición de errores.

Fuera del alcance del MVP:

- Certificación regulatoria, safety industrial o validación GxP.
- Pentest profesional, alta disponibilidad, disaster recovery y failover multi-región.
- Compatibilidad exhaustiva de navegadores/dispositivos.
- Carga de producción a gran escala o SLA contractual.
- Pruebas de hardware, PLC, maquinaria o integración con ERP/MES externos.

### 7.2 Riesgos de calidad priorizados

| Riesgo | Impacto | Probabilidad inicial | Nivel | Evidencia mínima exigida |
|---|---|---:|---|---|
| Inventario negativo o doble reserva | Pérdida de integridad operativa | Alta | Crítico | Unitarias + integración concurrente MySQL + API + SQL |
| Consumo parcial/duplicado al completar | Ledger y trazabilidad incorrectos | Alta | Crítico | Integración transaccional + repetición + SQL |
| Permiso incorrecto | Exposición o mutación no autorizada | Media | Crítico | Matriz `401/403` en Spring Test/Newman + E2E representativo |
| Transición inválida aceptada | Flujo de producción corrupto | Media | Alto | Tabla completa de estados en unitarias + integración/API |
| BOM/snapshot incorrecto | Requerimiento y consumo equivocados | Media | Alto | Unitarias de cálculo + integración de revisión concurrente |
| Inspección inválida/duplicada | Calidad sin trazabilidad | Media | Alto | Unitarias + constraint MySQL + API/E2E |
| Duplicados normalizados | Identidades ambiguas | Media | Alto | Servicio + integración de unique + API |
| Error/secretos filtrados | Riesgo de seguridad y soporte | Media | Alto | Security/controller tests + Newman + revisión de logs |
| Divergencia OpenAPI/implementación | Clientes y tests rotos | Media | Alto | Linter + contract diff + schema assertions |
| Precisión perdida en frontend | Cantidades incorrectas | Media | Alto | Casos límite API + utilidades UI + E2E dirigido |
| Dashboard incorrecto | Información engañosa, sin mutación | Media | Medio | API + E2E por rol |
| Degradación de rendimiento | Demo lenta/inestable | Media | Medio | Locust contextualizado + métricas del ambiente |

### 7.3 Niveles y responsabilidades

| Nivel | Qué demuestra | Herramienta | Dobles/infraestructura | No debe demostrar |
|---|---|---|---|---|
| Unitaria backend | Regla pura, cálculo, transición, mapper/validador | JUnit 5, Mockito, AssertJ si ya está disponible | Repositorios/gateways mock; reloj fijo | SQL, JPA, Spring completo o HTTP real |
| Web/security slice | Binding DTO, validation, handlers, filtros y permisos web | Spring Test, MockMvc | Dependencias de aplicación sustituidas | Transacción/persistencia real |
| Integración backend | JPA, Flyway, constraints, locks, transacciones y módulos juntos | JUnit 5, Spring Test, Testcontainers MySQL | MySQL real efímero; pocos mocks externos | Navegador o despliegue completo |
| Componente frontend | Render, formulario, guard/interceptor y estados de UI | Herramienta React a confirmar en Fase 14 | API simulada contractualmente | Backend/MySQL real |
| API | Sistema desplegado por HTTP, contrato y roles | Postman/Newman | Backend + MySQL; datos aislados | Detalle interno de clases |
| E2E | Journey de usuario y wiring completo | Cypress | Frontend + backend + MySQL | Matriz combinatoria exhaustiva |
| SQL | Invariantes independientes de API | Cliente MySQL/scripts | Dataset conocido y schema desechable | UX o contrato HTTP |
| Rendimiento | Latencia, throughput, error rate y saturación | Locust | Ambiente dedicado y dataset controlado | Capacidad productiva universal |
| Manual/exploratoria | Usabilidad, claridad, accesibilidad y riesgos emergentes | Checklist, navegador, DevTools | Ambiente estable | Regresión repetitiva de alto volumen |

Clasificación obligatoria:

- Una prueba con todos los colaboradores mockeados es unitaria.
- `@WebMvcTest`/MockMvc con aplicación mockeada es slice web, no E2E ni integración de persistencia.
- Una prueba JPA contra H2 no se aceptará como evidencia MySQL.
- Cypress que intercepta todas las API es componente UI; solo es E2E cuando cruza el stack real.

### 7.4 Ambientes

| Ambiente | Propósito | Datos | Evidencia válida |
|---|---|---|---|
| JVM unit | Unitarias puras | Builders locales, reloj fijo | JUnit/JaCoCo |
| Testcontainers | Integración por build | MySQL efímero + Flyway + fixture por suite | JUnit, logs sanitizados, SQL |
| Local/Compose QA | API, E2E, smoke manual | Volumen/schema reiniciable, usuarios demo | Newman, Cypress, screenshots/videos de fallo |
| Performance | Locust controlado | Dataset preconfigurado y exclusivo | CSV/HTML, config de carga, recursos host |
| CI | Gates reproducibles | Servicios efímeros, secretos CI | Artifacts ligados a commit/pipeline |

No se prueban mutaciones contra una base compartida o de producción. El perfil `test` usa secretos efímeros y el fixture demo nunca se activa implícitamente en producción.

### 7.5 Datos de prueba

Convenciones:

- `runId` único por ejecución; identificadores visibles usan prefijo/sufijo del run.
- Aliases estables del catálogo: `U_ADMIN`, `U_OPERATOR`, `U_INSPECTOR`, `U_INACTIVE`, `MAT_A`, `MAT_B`, `PRD_A`, `ORD_DRAFT`, etc.
- Fechas/reloj se fijan cuando una assertion depende del tiempo.
- Unitarias construyen solo el mínimo estado necesario.
- Integración crea un schema/container limpio y aplica Flyway; no reutiliza estado entre tests.
- API/E2E reinician el ambiente o cargan fixture conocido antes de la suite; no dependen del orden entre casos.
- No existe endpoint de test en el artefacto productivo. La preparación usa API pública o bootstrap externo exclusivo del ambiente.
- Passwords/tokens no aparecen en reports, snapshots, screenshots, console logs o nombres de casos.
- Casos de precisión incluyen `0.000001`, máximo contractual `9999999999999.999999`, cero, negativos, escala 7 y overflow de multiplicación.

Limpieza:

- Tablas append-only y triggers no se fuerzan con `DELETE` durante casos normales.
- La suite destruye el schema/container completo o recrea el volumen efímero.
- Locust usa dataset independiente; nunca se mezcla con E2E o demo.

### 7.6 Criterios de entrada, suspensión y salida

#### Entrada por suite

- Build del componente compila.
- Dependencias/servicios requeridos saludables.
- Migraciones aplicadas y versión de schema conocida.
- Fixture identificado, credenciales de test disponibles fuera de reportes.
- Contrato y caso de prueba versionados.
- Defectos bloqueantes conocidos declarados.

#### Suspensión

- Ambiente no saludable o datos contaminados.
- Más de 20% de fallos atribuibles a infraestructura.
- Migración no aplicable, login global roto o ausencia de evidencia.
- Métricas de rendimiento tomadas con carga/recursos desconocidos.

La suspensión se reporta como `BLOCKED`, no como `PASS` ni `FAIL` funcional.

#### Salida de implementación/módulo

- Casos P0/P1 aplicables ejecutados y con evidencia.
- Cero defectos S1/S2 abiertos sin aceptación explícita.
- Unitarias e integración obligatorias en verde.
- API positiva/negativa del módulo en verde.
- SQL relevante sin violaciones.
- Cobertura cumple umbral o excepción justificada por línea/clase.
- Riesgos residuales y limitaciones documentados.

#### Salida del MVP para demo

- Smoke completo y once journeys Cypress aprobados.
- Newman completo sin fallos inesperados.
- Docker Compose saludable desde ambiente limpio.
- Flujo principal ejecutable con datos deterministas.
- Evidencias ligadas a commit/build.
- Rendimiento reportado con contexto; si no se ejecutó, se declara pendiente.

### 7.7 Pruebas unitarias backend

Alcance prioritario:

- Normalización y reglas de username/SKU/código.
- Validación de password, estado de usuario y `authVersion`.
- Validación/cálculo BOM, material duplicado y overflow.
- Dirección/tipo de movimiento y no negatividad calculada.
- Máquina de estados completa de órdenes.
- Cálculo de snapshot, reservas, liberación y consumo.
- Reglas de inspección y observación.
- Mappers que excluyen campos sensibles.
- Selección de dashboard por rol.
- Traducción de excepciones a códigos estables donde sea lógica pura.

Patrón Arrange–Act–Assert, nombres `method_condition_expectedResult`, un comportamiento principal por test y captors solo cuando el argumento sea la evidencia. No se mockean value objects ni la clase bajo prueba.

Umbral propuesto JaCoCo:

- Paquetes `domain` y `application`: ≥ 80% líneas y ≥ 70% branches.
- Sin gate para DTOs triviales, configuración o código generado, pero exclusiones deben ser explícitas.
- Servicios críticos de inventario/órdenes/calidad requieren tests de cada rama de negocio aunque el porcentaje global ya cumpla.

### 7.8 Pruebas de integración

Suites mínimas:

1. Flyway desde schema vacío y `ddl-auto=validate` exitoso.
2. Unicidad normalizada concurrente de username, SKU, código y número de orden.
3. Generated columns, `CHECK`, FK `RESTRICT` y triggers de inmutabilidad.
4. Crear material produce balance cero atómicamente.
5. Publicar BOM conserva historia y solo una vigente.
6. Confirmar orden fija snapshot/reserva en la misma transacción.
7. Dos confirmaciones compitiendo por stock insuficiente para ambas: como máximo una confirma.
8. Lock de materiales en orden estable y manejo controlado de timeout/deadlock.
9. Fallo inyectado antes de commit revierte balance, movimiento, orden, historial y auditoría de éxito.
10. Completar genera exactamente un consumo por material y no duplica al repetir.
11. Cancelar libera reserva sin cambiar físico ni crear consumo.
12. Optimistic locking devuelve conflicto ante dos versiones del mismo maestro/borrador.
13. Inspección única de orden completada; rechazo sin observación bloqueado en servicio/DB.
14. Auditoría exitosa comparte transacción; rechazo crítico usa transacción corta independiente.
15. Usuario desactivado o `authVersion` modificado invalida token aún no expirado.

La concurrencia usa barreras/latches y múltiples transacciones reales; `Thread.sleep` no es mecanismo de sincronización ni evidencia.

### 7.9 Pruebas API — Postman/Newman

Cobertura:

- Las 39 operaciones del OpenAPI al menos una vez en éxito cuando el rol lo permite.
- Login automático y variables `baseUrl`, tokens por rol, IDs/versiones creados y `runId`.
- Matriz negativa `401/403` por familia de recursos.
- `400` para schema/tipo/longitud/enum/filtro/cantidad/campo desconocido.
- `404` para path/referencia inexistente.
- `409` para duplicados, concurrencia, stock, estado, BOM e inspección.
- Headers `Content-Type`, `Location`, `Cache-Control`, `WWW-Authenticate` y `X-Correlation-Id` según contrato.
- Envelope, tipos, campos obligatorios, enums, strings exactos y paginación.
- Tiempo por request como observación; Newman no sustituye Locust.
- Correlación entre acción, movimiento, historial, inspección y auditoría.

Orden de colección:

1. Health/setup externo.
2. Auth y captura de tres tokens.
3. Usuarios/catálogos/BOM.
4. Inventario.
5. Órdenes/transiciones.
6. Calidad/dashboard/auditoría.
7. Negativos independientes.
8. Validaciones finales y summary.

Una assertion falla por código estable o forma contractual; no depende del texto localizado de `message`.

### 7.10 Casos manuales y exploración

`docs/phase-5-test-catalog.md` contiene casos reproducibles, todos ejecutables manualmente por API/UI cuando exista implementación. Cada caso define prioridad, riesgo, trazabilidad, precondición, procedimiento, resultado y capa candidata.

Charters exploratorios de 30–45 minutos:

| Charter | Misión | Evidencia |
|---|---|---|
| EX-01 Roles | Intentar navegar/operar fuera del rol y mediante URL directa | Notas, correlation IDs, screenshots solo de fallos |
| EX-02 Cantidades | Variar pegado, formato, escala, límites y locale decimal | Dataset y resultados |
| EX-03 Concurrencia UI | Dos pestañas editando/accionando la misma versión | Timeline y respuestas `409` |
| EX-04 Recuperación | Refresh, back, token expirado y API temporalmente caída | Comportamiento y mensajes |
| EX-05 Usabilidad | Completar flujo principal sin conocimiento del modelo | Fricción, errores y claridad |
| EX-06 Accesibilidad básica | Teclado, foco, labels, contraste, zoom 200% | Checklist y defectos |
| EX-07 Trazabilidad | Reconstruir una orden usando UI/API/SQL/auditoría | IDs y gaps detectados |

Exploración no reemplaza regresión determinista y sus hallazgos se documentan como defectos reales, no como anécdotas.

### 7.11 Pruebas E2E — Cypress

Solo once journeys obligatorios:

| ID E2E | Journey | Caso base |
|---|---|---|
| E2E-001 | Login exitoso | TC-AUT-001 |
| E2E-002 | Login incorrecto | TC-AUT-002 |
| E2E-003 | Crear producto | TC-PRD-001 |
| E2E-004 | SKU duplicado | TC-PRD-002 |
| E2E-005 | Entrada de inventario | TC-INV-001 |
| E2E-006 | Crear orden | TC-ORD-001 |
| E2E-007 | Confirmación con stock insuficiente | TC-ORD-006 |
| E2E-008 | Iniciar y completar orden | TC-ORD-009 |
| E2E-009 | Inspección aprobada | TC-QLT-001 |
| E2E-010 | Inspección rechazada | TC-QLT-002 |
| E2E-011 | Permisos por rol | TC-SEC-006 |

Estándares:

- Selectores `data-cy` semánticos y estables.
- `cy.intercept()` observa requests reales y crea aliases; no sustituye backend en E2E.
- Fixtures/builders con `runId`; comandos personalizados para login/setup repetible.
- Assertions reintentables sobre estado visible y respuestas.
- Prohibido `cy.wait(ms)` salvo una justificación documentada de sistema externo; se espera alias/condición.
- Cada test puede ejecutarse aislado y no depende del anterior.
- Screenshot/video solo en fallo por defecto; se sanitizan tokens y passwords.

### 7.12 Pruebas SQL y de datos

- Ejecutar V-001 a V-027 contra fixture conocido y registrar conteos esperados.
- Verificar duplicados normalizados, saldos, reservas, movimientos, snapshots, estados, inspecciones y auditoría.
- Comparar `reserved_quantity` con suma de reservas activas.
- Comparar consumo por orden/material con snapshot requerido.
- Confirmar una única BOM vigente y una inspección por orden.
- Verificar historial inicia en DRAFT, secuencia continua y último estado igual a cabecera.
- Confirmar cero valores negativos, referencias huérfanas o secretos en auditoría.
- Probar constraints/triggers mediante mutaciones negativas solo en schema desechable y rollback/destrucción posterior.
- Capturar versión MySQL, migration version, dataset/runId, query ID y resultado.

Una consulta que devuelve cero filas solo es PASS cuando el caso define explícitamente que cero violaciones es lo esperado.

### 7.13 Pruebas de rendimiento

Baseline propuesto para Fase 19, sujeto a capacidad real:

| Parámetro | Valor inicial |
|---|---|
| Usuarios concurrentes | 20 |
| Spawn rate | 2 usuarios/s |
| Warm-up | 1 minuto, excluido de evaluación |
| Duración medida | 5 minutos |
| Dataset | ≥ 200 productos, 500 materiales, 1,000 órdenes y movimientos suficientes para paginación |
| Lecturas | Productos 30%, inventario 30%, órdenes 25%, dashboard 10% |
| Escrituras controladas | Crear borrador 5%; ejecución separada si contamina comparación |
| Login | Una vez por usuario y escenario específico adicional |
| Umbrales | Lecturas p95 ≤ 500 ms; escrituras p95 ≤ 1 s; error técnico < 1% |

Los `400/401/403/409` intencionales no se mezclan con error técnico, pero se reportan por separado. Se capturan host/CPU/RAM, versiones, commit, red, dataset, percentiles p50/p90/p95/p99, throughput y fallos. Un resultado local no se extrapola a producción.

### 7.14 Seguridad funcional y configuración

- Token ausente, mal formado, firma cambiada, expirado y versión inválida.
- Usuario inactivo no inicia sesión ni reutiliza token.
- Cada rol prueba al menos una acción permitida y todas las familias prohibidas.
- Body/header no puede suplantar rol o actor.
- Password/hash/token ausentes de responses, logs, auditoría y artifacts.
- CORS solo permite orígenes configurados; no wildcard con credenciales.
- JWT secreto/DB password ausentes de Git e imágenes/config efectiva.
- Errores `500` no exponen stack, SQL o clases.
- Frontend elimina sesión al recibir `401` y no inserta HTML del servidor.

Esto es verificación funcional/configuración, no un pentest profesional.

### 7.15 Checklist de smoke y regresión

#### Smoke obligatorio

- Servicios y healthchecks saludables.
- Login por cada rol y login inválido.
- Catálogos se consultan.
- ADMIN crea material/producto/BOM.
- OPERATOR registra entrada, crea/confirma/inicia/completa orden.
- QUALITY_INSPECTOR registra inspección.
- Dashboard responde para cada rol.
- Auditoría ADMIN reconstruye el correlation ID.
- Un permiso prohibido devuelve `403`.

#### Regresión por cambio

| Cambio | Regresión mínima |
|---|---|
| Seguridad/JWT | AUT + SEC + todos los handlers `401/403` |
| Usuario/roles | USR + último admin + invalidación de tokens |
| Producto/material | PRD/MAT + BOM + selectores de orden |
| BOM | BOM + confirmación/snapshot + cálculos/overflow |
| Inventario | INV + confirm/cancel/complete + concurrencia + SQL |
| Órdenes | Máquina de estados completa + ledgers + calidad |
| Calidad | QLT + órdenes completadas + dashboard/auditoría |
| Error/contrato | ERR + OpenAPI diff + Newman negativo |
| Frontend compartido | Login, rutas, formularios, mensajes y once E2E |
| Migración/SQL | Flyway limpio/upgrade, JPA validate, V-001–V-027 |

Suites objetivo:

- **PR rápida:** unitarias, web slices y lint/contrato; objetivo informativo < 10 min.
- **Main:** unitarias + integración MySQL + build.
- **API:** Compose + Newman completo.
- **E2E:** once Cypress después de API saludable.
- **Release/demo:** smoke, regresión P0/P1, SQL, Docker y evidencias.
- **Performance:** manual/programada y no gate de cada commit.

### 7.16 Formato de defectos

```text
ID: QAF-DEF-###
Título: [Módulo] Comportamiento observado
Fecha/build/commit:
Ambiente y versiones:
Reportado por:
Caso/requisito relacionado:
Severidad: S1/S2/S3/S4
Prioridad: P0/P1/P2/P3
Estado: NEW/TRIAGED/IN_PROGRESS/FIXED/READY_FOR_RETEST/CLOSED/REOPENED/DEFERRED
Precondiciones y datos (sanitizados):
Pasos exactos:
Resultado esperado:
Resultado actual:
Frecuencia:
Correlation ID / request / query relevante:
Evidencia:
Impacto:
Posible causa: hipótesis, no hecho hasta confirmar
Solución aplicada:
Prueba de regresión:
Riesgo residual:
```

No se crean defectos ficticios ni se introduce código defectuoso para llenar el formato. Un hallazgo se cierra solo con retest y regresión ejecutados.

### 7.17 Criterios de automatización y mantenimiento manual

Automatizar cuando:

- El caso es P0/P1 o gate frecuente.
- El resultado es determinista y observable.
- Se ejecutará repetidamente en PR, main, API, E2E o release.
- Cubre combinaciones, concurrencia o regresión costosa manualmente.
- Los datos pueden aislarse y el mantenimiento esperado es razonable.
- El riesgo no puede demostrarse mejor en una capa más baja.

Mantener manual/exploratorio cuando:

- Requiere juicio visual, claridad de mensaje, accesibilidad o usabilidad.
- El flujo cambia rápidamente y todavía no es estable.
- Es escenario raro de bajo riesgo y alto costo de automatización.
- Depende de inspección humana de logs/planes sin oracle estable.
- Es exploración de riesgos nuevos, no una regresión conocida.

Reglas de mantenimiento:

- El propietario del cambio actualiza código, caso y trazabilidad juntos.
- Tests obsoletos se reemplazan/eliminan con motivo; no se ignoran silenciosamente.
- Retry puede obtener diagnóstico, pero el primer fallo conserva estado FAIL hasta determinar causa.
- Un flaky test genera defecto con frecuencia, ambiente y evidencia; se corrige o aísla de forma visible con fecha límite.

### 7.18 Prioridad y severidad

#### Severidad — impacto del defecto

| Nivel | Definición | Ejemplos QAFlow |
|---|---|---|
| S1 Crítica | Pérdida/corrupción de datos, brecha grave, sistema no utilizable o sin workaround | Inventario negativo, consumo duplicado, acceso administrativo no autorizado, secretos expuestos |
| S2 Alta | Flujo principal roto o regla esencial incumplida con workaround inviable/difícil | No confirmar con stock válido, completar cancelada, inspección inválida aceptada |
| S3 Media | Función secundaria incorrecta con workaround razonable | Filtro/paginación incorrectos, dashboard desactualizado sin mutación |
| S4 Baja | Presentación, texto o inconveniente menor sin pérdida funcional | Alineación, copy no crítico, orden visual |

#### Prioridad — urgencia de atención

| Nivel | Definición |
|---|---|
| P0 Inmediata | Bloquea build/demo/release o expone riesgo S1; atender antes de continuar. |
| P1 Alta | Resolver en el ciclo actual antes del gate afectado. |
| P2 Media | Planificar en siguiente ciclo; existe workaround/impacto acotado. |
| P3 Baja | Backlog/mejora; no compromete objetivo actual. |

Severidad y prioridad se asignan separadamente. Ejemplo: un typo en la pantalla principal puede ser S4/P1 por demo; una limitación de escala fuera del MVP puede ser S2/P3 si no afecta el ambiente comprometido y está documentada.

### 7.19 Evidencias, resultados y métricas

Estados de ejecución: `NOT_RUN`, `PASS`, `FAIL`, `BLOCKED`, `SKIPPED` con justificación. No existe “PASS parcial”.

Toda evidencia automatizada incluye:

- `runId`, timestamp UTC, commit SHA/build y ambiente.
- Suite/caso, duración y resultado.
- Reporte JUnit/JaCoCo/Newman/Cypress/Locust/SQL según capa.
- Correlation ID cuando cruza HTTP.
- Logs mínimos sanitizados; nada de passwords, JWT completos o secretos.

Métricas útiles:

- Casos planificados/ejecutados/pass/fail/blocked por prioridad.
- Defectos por severidad/estado y tasa de reapertura.
- Cobertura line/branch solo en paquetes objetivo.
- Duración y flaky rate por suite.
- API status/schema/error-code coverage.
- Rendimiento p50/p90/p95/p99, throughput y error rate.

No se usa “cantidad de tests” como prueba aislada de calidad.

### 7.20 Gates propuestos

| Gate | Bloquea | Criterio |
|---|---|---|
| Validate/build | Siguiente job | Compilación, lint/config y OpenAPI válido |
| Unit | Merge/package | Cero fallos; cobertura objetivo o excepción documentada |
| Integration | Merge/package | MySQL/Flyway/transacciones críticas en verde |
| API | Release/demo | Newman sin fallos inesperados; contrato y permisos P0/P1 |
| E2E | Release/demo | Once journeys en verde, sin retry ocultando causa |
| SQL | Release/demo | Cero violaciones esperadas y conteos del fixture correctos |
| Defects | Release/demo | Cero S1/S2 abiertos salvo aceptación explícita documentada |
| Performance | Informe/gate manual | Umbrales medidos en ambiente conocido; no bloquea cada commit |

## 8. Archivos creados

- `docs/phase-5-test-strategy.md`
- `docs/phase-5-test-catalog.md`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-4-api-contract.md` (solo registro de aprobación)

## 10. Comandos

No se ejecutaron JUnit, Mockito, MySQL, Newman, Cypress, Locust, Docker ni pruebas funcionales. Se ejecutó una auditoría Node.js/búsquedas locales: 98 filas/IDs únicos y continuos en 13 módulos, cero filas mal formadas, prioridades válidas, 151 referencias completas resueltas, RN-001 a RN-036 cubiertas, once E2E consecutivos, 16 secciones obligatorias y estados `98 NOT_RUN / 0 ejecutados / 0 PASS` coherentes.

## 11. Cómo verificar

1. Confirmar que todo RF/RNF y regla crítica aparece en `TEST_MATRIX.md` y catálogo.
2. Confirmar IDs únicos y rangos de casos continuos por módulo.
3. Confirmar los once E2E obligatorios y su caso base.
4. Confirmar cobertura positiva, negativa, borde, permisos, transacción y concurrencia.
5. Confirmar que herramientas/niveles no están mal clasificados.
6. Confirmar que severidad y prioridad son independientes.
7. Confirmar que no se marca prueba runtime como ejecutada.
8. Confirmar que cada archivo existe y está en `FILE_MANIFEST.md`.
9. Durante implementación, ejecutar cada suite y adjuntar evidencia antes de cambiar `NOT_RUN`.

## 12. Pruebas relacionadas

Esta fase diseña todas las suites futuras. Solo permanecen ejecutadas las auditorías estáticas `TM-017` (OpenAPI) y `TM-019` (estrategia/catálogo); ninguna evidencia funcional existe todavía.

## 13. Riesgos

- Los casos pueden requerir ajuste cuando existan firmas/clases/UI reales, sin perder su intención.
- Los umbrales de cobertura pueden incentivar tests superficiales si se usan sin revisión de branches/riesgo.
- Concurrencia puede ser flaky si se sincroniza por tiempo en lugar de barreras reales.
- E2E puede volverse lento si se usa para combinatoria que corresponde a unitarias/API.
- Reset de datos es complejo por ledgers/triggers; se requiere entorno efímero.
- Rendimiento varía por host y no debe compararse sin contexto.
- No se conoce aún disponibilidad/versiones exactas del toolchain ni runner.
- No hay implementación sobre la cual ejecutar lo diseñado.

## 14. Estado actualizado

- Fases 0–4: completadas y aprobadas.
- Fase 5: completada y aprobada el 2026-07-19; auditoría estática conforme.
- Pruebas implementadas: ninguna.
- Pruebas funcionales ejecutadas: ninguna.
- Código de aplicación: inexistente.

## 15. Próximo paso

Se inició únicamente la Fase 6 para proponer el árbol del repositorio, propósito de carpetas, ubicación de artefactos y preparación de Git, sin inicializar todavía backend, frontend o suites.

## 16. Registro de aprobación

Fase 5 aprobada explícitamente por el usuario el 2026-07-19. La aprobación valida la estrategia y el catálogo; los 98 casos continúan `NOT_RUN` hasta su ejecución real.
