# Fase 5 — Catálogo de casos de prueba

## Estado y propósito

Catálogo de diseño, no ejecución. Todos los casos quedan `NOT_RUN` hasta que exista la funcionalidad, se ejecute el procedimiento y se adjunte evidencia. Cada caso puede ejecutarse manualmente; la columna **Capas** indica dónde se automatizará o complementará.

## Convenciones

- ID: `TC-<MÓDULO>-<NNN>`.
- Prioridad: `P0` inmediata, `P1` alta, `P2` media, `P3` baja.
- Riesgo/severidad potencial: `S1` crítica, `S2` alta, `S3` media, `S4` baja.
- Capas: `U` unitaria, `W` web/security slice, `I` integración MySQL, `API` Newman, `E2E` Cypress, `SQL`, `M` manual/exploratoria, `PERF` Locust.
- Una capa listada es planificada, no evidencia actual.
- Los códigos HTTP y error se comparan por `code`; el texto de `message` no es oracle estable.

## Datos simbólicos comunes

| Alias | Estado mínimo |
|---|---|
| `U_ADMIN` | Usuario ADMIN activo con token vigente. |
| `U_OPERATOR` | Usuario OPERATOR activo con token vigente. |
| `U_INSPECTOR` | Usuario QUALITY_INSPECTOR activo con token vigente. |
| `U_INACTIVE` | Usuario desactivado con credenciales conocidas de test. |
| `MAT_A` | Material activo KILOGRAM, balance conocido. |
| `MAT_B` | Material activo PIECE, balance conocido. |
| `MAT_INACTIVE` | Material desactivado con historia conservada. |
| `PRD_A` | Producto activo con BOM vigente: 2.500000 MAT_A + 4.000000 MAT_B por unidad. |
| `PRD_NO_BOM` | Producto activo sin BOM vigente. |
| `PRD_INACTIVE` | Producto desactivado. |
| `ORD_DRAFT` | Orden DRAFT con versión conocida. |
| `ORD_CONFIRMED` | Orden con snapshot y reservas activas. |
| `ORD_IN_PROGRESS` | Orden iniciada con reservas activas. |
| `ORD_COMPLETED` | Orden completada, consumos creados, sin inspección salvo indicación. |
| `ORD_CANCELLED` | Orden terminal sin consumo. |
| `runId` | Sufijo único por ejecución para evitar colisiones. |

Cada suite parte de un schema/container limpio o fixture conocido. IDs, versiones, tokens y correlation IDs se capturan de respuestas reales; no se escriben valores dependientes de otra ejecución.

## Autenticación

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-AUT-001 | P0/S2 | RF-AUT-001/002, EF-001 | `U_OPERATOR` activo | Enviar username/password válidos a login; consultar `/auth/me` con el token | `200`; Bearer, 900 s, perfil OPERATOR; token permite `/auth/me`; `Cache-Control: no-store` | W,I,API,E2E |
| TC-AUT-002 | P1/S2 | RF-AUT-003, EN-001 | Usuario existente | Enviar contraseña incorrecta | `401 INVALID_CREDENTIALS`; sin token, password o causa detallada | W,I,API,E2E |
| TC-AUT-003 | P1/S2 | RF-AUT-003 | Username inexistente | Intentar login | Misma forma, código y mensaje genérico que TC-AUT-002; evento de rechazo sin enumerar usuario | W,I,API |
| TC-AUT-004 | P1/S2 | RF-USR-007, EN-002 | `U_INACTIVE` | Intentar login con password correcto | `401 INVALID_CREDENTIALS`; no se emite token | W,I,API |
| TC-AUT-005 | P1/S2 | RF-AUT-004, RN-023, EB-012 | JWT expirado construido en test | Consultar recurso protegido | `401 TOKEN_EXPIRED`, `WWW-Authenticate: Bearer`, error correlacionado | W,I,API |
| TC-AUT-006 | P0/S1 | RF-AUT-004 | Token malformado y token con payload/firma alterados | Consultar recurso protegido con cada variante | `401 INVALID_TOKEN`; ninguna operación de aplicación ni detalle criptográfico expuesto | W,I,API |
| TC-AUT-007 | P0/S1 | RF-AUT-005/008, EB-013 | Token válido; ADMIN cambia rol/password/active del usuario | Reutilizar token anterior | `401 INVALID_TOKEN`; `authVersion`/estado invalida token antes de `exp` | U,I,API |
| TC-AUT-008 | P1/S2 | RF-AUT-006, D-0042 | Sesión UI válida | Ejecutar logout, refresh y volver a ruta protegida | Token eliminado de memoria/sessionStorage; redirección a login; no se llama logout backend | E2E,M |

## Seguridad y permisos

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-SEC-001 | P0/S1 | RN-022, RF-AUT-004 | Sin Authorization | Consultar un endpoint de cada familia protegida | `401` uniforme; no mutación; correlation ID presente | W,API |
| TC-SEC-002 | P0/S1 | Matriz permisos | `U_ADMIN` | Ejecutar gestión usuarios/productos/materiales/BOM y lecturas globales | Acciones autorizadas; actor real auditado | W,I,API |
| TC-SEC-003 | P0/S1 | D-0007, RN-036, EN-004 | `U_ADMIN` | Intentar movimiento, crear/transicionar orden y crear inspección | `403 ACCESS_DENIED`; cero cambios/auditoría de éxito | W,I,API |
| TC-SEC-004 | P0/S1 | D-0007, RN-036 | `U_OPERATOR` | Operar inventario/órdenes; intentar usuarios, catálogo write, inspección y auditoría | Operaciones propias permitidas; prohibidas devuelven `403` | W,I,API |
| TC-SEC-005 | P0/S1 | RF-QLT-001, D-0045 | `U_INSPECTOR`, órdenes en varios estados | Listar/detallar órdenes; pedir filtro no COMPLETED; intentar mutarlas | Solo COMPLETED visible; otro alcance `403 ORDER_SCOPE_FORBIDDEN`; mutaciones `403` | W,I,API |
| TC-SEC-006 | P0/S1 | RN-021/022, EN-018 | Tres roles y UI disponible | Navegar menús/URLs directas y ejecutar una acción prohibida por rol | UI no ofrece acción; URL/API siguen protegidas; `403` visible y sin cambio | W,API,E2E,M |

## Usuarios

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-USR-001 | P1/S2 | RF-USR-003/009 | `U_ADMIN`, `runId` | Crear usuario válido por cada rol | `201`, Location, versión `"0"`; password/hash ausentes; auditoría SUCCESS | U,I,API |
| TC-USR-002 | P1/S2 | RF-USR-005 | Username base existente | Crear variantes con espacios/case normalizados | `409 DUPLICATE_USERNAME`; solo una fila | U,I,API,SQL |
| TC-USR-003 | P1/S2 | RF-USR-006 | `U_ADMIN` | Enviar username/display/password/role ausentes, inválidos y campo desconocido | `400` estable con fieldErrors sanitizados; password nunca eco | U,W,API |
| TC-USR-004 | P2/S3 | RF-USR-001/002 | >20 usuarios | Buscar, filtrar role/active, ordenar y cambiar página | Página base cero, totales y orden correctos; detalle existente `200` | I,API,M |
| TC-USR-005 | P0/S1 | RF-USR-004/006 | Usuario activo con token | Cambiar role, active o password con versión actual | `200/204`; versión/authVersion incrementan; token previo inválido | U,I,API |
| TC-USR-006 | P0/S1 | RF-USR-008, RN-034, EN-019 | Solo un ADMIN activo | Desactivar o degradar al último ADMIN | `409 LAST_ACTIVE_ADMIN`; usuario y token sin cambio | U,I,API,SQL |
| TC-USR-007 | P1/S2 | D-0040 | Dos copias de la misma versión | Actualizar primera y luego segunda | Primera `200`; segunda `409 CONCURRENT_MODIFICATION`; no last-write-wins | U,I,API |

## Productos

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-PRD-001 | P1/S2 | RF-PRD-001 | `U_ADMIN`, SKU con `runId` | Crear producto válido desde UI/API | `201`; SKU/nombre/active/version correctos; aparece en lista | U,I,API,E2E |
| TC-PRD-002 | P1/S2 | RF-PRD-002, RN-001, EN-005 | SKU existente | Crear SKU igual y variante normalizada | `409 DUPLICATE_SKU`; una fila; formulario conserva datos | U,I,API,E2E,SQL |
| TC-PRD-003 | P1/S2 | RF-PRD-005 | `U_ADMIN` | Probar SKU/nombre vacíos, longitudes, active inválido y campo extra | `400`; fieldErrors correctos; sin persistencia | U,W,API |
| TC-PRD-004 | P2/S3 | RF-PRD-003 | Catálogo >20 | Buscar SKU/nombre, filtrar active y ordenar/paginar | Resultados/totales coherentes, strings exactos | I,API,M |
| TC-PRD-005 | P1/S2 | RF-PRD-003/004, RN-029 | Producto activo y versión actual | Editar datos y después desactivar | `200`; versión incrementa; historial relacionado permanece | U,I,API,SQL |
| TC-PRD-006 | P1/S2 | RF-PRD-006, RF-ORD-001, RN-030 | `PRD_INACTIVE` | Intentar nueva BOM/orden con producto inactivo | `409 RESOURCE_INACTIVE`; no BOM, orden ni auditoría SUCCESS | I,API |

## Materiales

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-MAT-001 | P0/S2 | RF-MAT-001, RF-INV-001 | `U_ADMIN`, código `runId` | Crear material válido | `201`; balance asociado `0/0/0` en misma transacción | U,I,API,SQL |
| TC-MAT-002 | P1/S2 | RF-MAT-002, RN-002 | Código existente | Crear variante normalizada | `409 DUPLICATE_MATERIAL_CODE`; una fila/balance | U,I,API,SQL |
| TC-MAT-003 | P1/S2 | RF-MAT-006/007 | `U_ADMIN` | Campos vacíos, UOM fuera de enum, longitudes y campo extra | `400`; sin material/balance parcial | U,W,I,API |
| TC-MAT-004 | P2/S3 | RF-MAT-003/008 | Material sin BOM/movimiento/reserva | Cambiar unidad con versión actual | `200`; unidad cambia y versión incrementa | U,I,API |
| TC-MAT-005 | P1/S2 | RN-031, EN-020, D-0031 | Material ya referenciado | Intentar cambiar UOM | `409 MATERIAL_UNIT_IMMUTABLE`; historia/cantidades sin reinterpretar | U,I,API,SQL |
| TC-MAT-006 | P1/S2 | RF-MAT-004/005, RN-029, RN-030 | Material con historial | Desactivar y consultar historial/balance | Desactivado para nuevas operaciones; datos históricos intactos | I,API,SQL |
| TC-MAT-007 | P1/S2 | D-0040 | Dos versiones del material | Enviar dos PATCH desde misma versión | Una actualización; otra `409 CONCURRENT_MODIFICATION` | U,I,API |

## BOM

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-BOM-001 | P0/S2 | RF-BOM-001/002 | Producto/materiales activos | Publicar lista de dos materiales positivos | `201`; revisión 1 current; líneas exactas y auditadas | U,I,API,SQL |
| TC-BOM-002 | P1/S2 | RF-BOM-003, RN-006, EN-007 | Producto activo | Publicar `items: []` | `400 VALIDATION_ERROR`; no revisión | U,W,API |
| TC-BOM-003 | P1/S2 | RF-BOM-004, RN-007, EN-008 | `MAT_A` activo | Enviar dos líneas con mismo material | `400`; no sumar ni guardar duplicado | U,I,API |
| TC-BOM-004 | P1/S2 | RF-BOM-005, EN-009 | Producto/material activo | Cantidad cero, negativa, escala 7 y formato inválido | `400`; sin revisión parcial | U,W,API |
| TC-BOM-005 | P1/S2 | RF-BOM-006 | Producto activo | Publicar con material inexistente | `404 RESOURCE_NOT_FOUND`; BOM anterior permanece current | U,I,API |
| TC-BOM-006 | P1/S2 | RF-BOM-006 | `MAT_INACTIVE` | Publicar BOM nueva con material inactivo | `409 RESOURCE_INACTIVE`; sin reemplazo | U,I,API |
| TC-BOM-007 | P1/S2 | RF-BOM-007/008 | `PRD_A` con revisión 1 | Publicar revisión 2 y consultar current/historia | Rev2 única current; rev1 superseded/inmutable; ambas consultables | I,API,SQL |
| TC-BOM-008 | P0/S1 | D-0028, D-0040 | Dos admins/transacciones sobre misma current | Sincronizar dos publicaciones concurrentes | Una `201`, otra `409 BOM_CONCURRENTLY_CHANGED`; nunca dos revisiones current | I,API,SQL |

## Inventario

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-INV-001 | P0/S1 | RF-INV-002/008, EF-004 | `MAT_A` saldo cero, OPERATOR | Registrar ENTRY 100 con referencia | `201`; onHand 100, reserved 0, available 100; movimiento/auditoría atómicos | U,I,API,E2E,SQL |
| TC-INV-002 | P0/S1 | RF-INV-003, RN-032, EB-003 | Disponible exactamente 25 | Registrar MANUAL_OUT 25 | Éxito; disponible/físico quedan 0, nunca negativos | U,I,API,SQL |
| TC-INV-003 | P0/S1 | RF-INV-003/007, EN-010 | Disponible 24.999999 | Registrar salida 25 | `409 INSUFFICIENT_STOCK`; cero cambio y cero movimiento/auditoría SUCCESS | U,I,API,SQL |
| TC-INV-004 | P0/S1 | RN-003, EB-002 | Material activo | Enviar cero, negativo, escala 7, máximo excedido y tipo/dirección inválidos | `400`; balance intacto | U,W,API |
| TC-INV-005 | P1/S2 | RF-INV-010 | Material inexistente e inactivo | Intentar ENTRY/OUT sobre cada variante | Inexistente `404`; inactivo `409`; sin cambios | U,I,API |
| TC-INV-006 | P1/S2 | RF-INV-006 | Movimiento original conocido | Registrar ADJUSTMENT compensatorio con razón | Nuevo movimiento; original inmutable; balance reconciliado | U,I,API,SQL |
| TC-INV-007 | P0/S1 | RF-INV-004/012 | `U_OPERATOR` | Enviar `PRODUCTION_CONSUMPTION` a movimientos y revisar contrato de balances | `400 VALIDATION_ERROR`; OpenAPI no ofrece escritura directa de balance; cero cambio | W,API |
| TC-INV-008 | P0/S1 | RF-INV-005/006, RN-025 | Movimiento existente, schema desechable | Intentar update/delete vía repositorio/SQL | Trigger rechaza; ledger original intacto | I,SQL |
| TC-INV-009 | P0/S1 | RF-INV-007/011, RN-005, RN-010 | Datos con físico/reservado conocidos | Consultar y ejecutar V-008/V-009/V-010 | `onHand ≥ reserved ≥ 0`, available calculado, suma de reservas coincide | I,API,SQL |
| TC-INV-010 | P0/S1 | RNF-DAT-003 | Stock solo para una de dos salidas | Sincronizar dos MANUAL_OUT concurrentes | Como máximo una éxito; otra conflicto; saldo final no negativo | I,API,SQL |
| TC-INV-011 | P2/S3 | RF-INV-009 | >20 movimientos variados | Filtrar material/tipo/orden/actor/from/to y paginar | Orden temporal, límites y totales correctos; ledger sin duplicados | I,API,M |

## Órdenes de producción

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-ORD-001 | P0/S2 | RF-ORD-001/002 | `PRD_A`, OPERATOR | Crear cantidad 10 | `201`; DRAFT, número servidor único, BOM/snapshot nulos, versión 0 | U,I,API,E2E,SQL |
| TC-ORD-002 | P1/S2 | RF-ORD-001 | Producto inexistente, inactivo y `PRD_NO_BOM` | Intentar crear con cada variante | `404` o `409 RESOURCE_INACTIVE/BOM_NOT_DEFINED`; sin orden | U,I,API |
| TC-ORD-003 | P1/S2 | RF-ORD-003 | `ORD_DRAFT` | Cambiar producto/cantidad con versión actual | `200`; DRAFT, sin snapshot; versión incrementa | U,I,API |
| TC-ORD-004 | P1/S2 | RF-ORD-004/013 | Orden CONFIRMED/COMPLETED/CANCELLED | Intentar PATCH parametrizado | CONFIRMED: `409 INVALID_ORDER_TRANSITION`; terminales: `409 ORDER_IMMUTABLE`; sin cambio | U,I,API |
| TC-ORD-005 | P0/S1 | RF-ORD-005/006/007, RN-009, EB-006 | Disponible exactamente requerido | Confirmar DRAFT | CONFIRMED; BOM current fijada, snapshot exacto, reserva total atómica | U,I,API,SQL |
| TC-ORD-006 | P0/S1 | RN-008, RN-009, EN-011 | Un material por debajo del requerido | Confirmar DRAFT | `409 INSUFFICIENT_STOCK`; ninguna reserva/snapshot/transición parcial | U,I,API,E2E,SQL |
| TC-ORD-007 | P1/S2 | RF-BOM-008, RN-024, EB-005 | DRAFT creado con rev1; publicar rev2 | Confirmar después del cambio y luego publicar rev3 | Snapshot usa rev2 al confirmar y no cambia con rev3; no mezcla revisiones | U,I,API,SQL |
| TC-ORD-008 | P0/S1 | RNF-DAT-003, OI-002 | Stock alcanza solo una de dos órdenes | Sincronizar confirmaciones en transacciones reales | Una CONFIRMED, otra `409`; agregado/detalle consistente; sin deadlock no controlado | I,API,SQL |
| TC-ORD-009 | P0/S2 | RF-ORD-008/009, EF-006 | `ORD_CONFIRMED` | Iniciar; con versión nueva completar | IN_PROGRESS y luego COMPLETED; UI/HTTP reflejan estados/versiones | U,I,API,E2E |
| TC-ORD-010 | P0/S1 | RF-ORD-009/010/011, RN-027 | `ORD_IN_PROGRESS` con reserva íntegra | Completar | Consumo por material, físico disminuye, reserva cero, historial/auditoría en un commit | U,I,API,SQL |
| TC-ORD-011 | P0/S1 | RN-028, RNF-DAT-001 | Fallo inyectado entre cambios de completar | Ejecutar completar y forzar excepción antes de commit | Rollback total: orden, balances, movimientos, historial y auditoría SUCCESS sin parcial | I,SQL |
| TC-ORD-012 | P1/S2 | RF-ORD-012 | `ORD_DRAFT`, reason válida | Cancelar | CANCELLED; historial/auditoría; sin reserva ni consumo | U,I,API |
| TC-ORD-013 | P0/S1 | RF-ORD-012, RN-026, EF-009 | CONFIRMED e IN_PROGRESS | Cancelar cada estado con reason | Reserva liberada, físico intacto, ningún consumo | U,I,API,SQL |
| TC-ORD-014 | P1/S2 | RN-011, RN-012, RN-013, RN-020; EN-012/013/014 | Órdenes en cada estado | Ejecutar todas las acciones origen no permitidas | No terminal inválida: `409 INVALID_ORDER_TRANSITION`; terminal: `409 ORDER_IMMUTABLE`; cero efecto | U,I,API |
| TC-ORD-015 | P0/S1 | RN-033, RNF-REL-001 | Orden ya COMPLETED/CANCELLED | Repetir complete/cancel/confirm tras timeout simulado | `409`; no duplica movimiento, reserva, historial o auditoría SUCCESS | U,I,API,SQL |
| TC-ORD-016 | P0/S1 | RN-004, D-0026 | Cantidad/BOM cuyo producto excede DECIMAL | Confirmar | `409 QUANTITY_OVERFLOW`; sin snapshot/reserva parcial | U,I,API,SQL |

## Inspecciones de calidad

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-QLT-001 | P0/S2 | RF-QLT-002/004/006, RN-035, EF-007 | `ORD_COMPLETED` sin inspección, INSPECTOR | Crear APPROVED sin observación | `201`; inspección única/inmutable, producto maestro sin cambio, dashboard/auditoría actualizados | U,I,API,E2E,SQL |
| TC-QLT-002 | P0/S2 | RF-QLT-004/005, EF-008 | Otra orden COMPLETED | Crear REJECTED con observación válida | `201`; texto trim conservado semánticamente y trazable | U,I,API,E2E,SQL |
| TC-QLT-003 | P1/S2 | RN-016, RN-018, EN-016 | Orden COMPLETED | Rechazar con ausente, vacío y espacios | `400 VALIDATION_ERROR`, field `observations`; sin inspección | U,W,API |
| TC-QLT-004 | P1/S2 | RN-014, RN-017, EN-015 | Orden DRAFT/CONFIRMED/IN_PROGRESS/CANCELLED | Intentar inspección | `409 ORDER_NOT_COMPLETED`; sin registro | U,I,API |
| TC-QLT-005 | P1/S2 | RF-QLT-003, RN-015, EN-017 | Orden ya inspeccionada | Crear segunda inspección, incluso otro resultado | `409 INSPECTION_ALREADY_EXISTS`; original intacta | U,I,API,SQL |
| TC-QLT-006 | P0/S1 | RF-QLT-007 | ADMIN y OPERATOR | Intentar crear inspección | `403 ACCESS_DENIED`; sin registro | W,I,API |
| TC-QLT-007 | P1/S2 | RF-QLT-002 | Orden inexistente | Crear inspección | `404 RESOURCE_NOT_FOUND`; error correlacionado | U,I,API |
| TC-QLT-008 | P1/S2 | RF-QLT-008/009, RN-025 | Inspecciones existentes, schema desechable | Listar/filtrar; intentar update/delete por API/SQL | Filtros correctos; API no expone mutación y trigger protege fila | I,API,SQL,M |

## Dashboard

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-DSH-001 | P1/S3 | RF-DSH-002 | Dataset conocido, ADMIN | Consultar dashboard y comparar SQL | Conteos activos, órdenes, sin stock e inspecciones exactos; schema ADMIN | I,API,E2E,SQL |
| TC-DSH-002 | P1/S3 | RF-DSH-003 | `U_OPERATOR` y órdenes propias/ajenas | Consultar dashboard | Estados globales permitidos, recientes propias y sin stock; sin datos ADMIN/calidad indebidos | I,API,E2E |
| TC-DSH-003 | P1/S3 | RF-DSH-004 | `U_INSPECTOR` | Consultar dashboard | Completadas sin inspección y resultados recientes exactos; discriminator de rol correcto | I,API,E2E,SQL |

## Auditoría

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-AUD-001 | P0/S2 | RF-COM-002 | Ejecutar flujo principal con correlation ID fijo | Consultar eventos como ADMIN | Actores, acciones, recursos, outcomes y correlación permiten reconstruir flujo | I,API,SQL,M |
| TC-AUD-002 | P1/S2 | RF-AUT-009, arquitectura 7.13 | Login fallido/permiso/stock rechazado | Consultar auditoría tras rollback | Rechazos críticos sobreviven en transacción independiente; no hay SUCCESS falso | I,API,SQL |
| TC-AUD-003 | P2/S3 | RF-COM-001 | >20 eventos variados | Filtrar actor/action/resource/outcome/correlation/from/to y paginar | Resultados/totales/orden correctos; solo ADMIN | I,API |
| TC-AUD-004 | P1/S2 | D-0032 | Evento existente, schema desechable | Intentar update/delete SQL/repositorio | Trigger rechaza; evento inmutable | I,SQL |
| TC-AUD-005 | P0/S1 | RNF-SEC-004 | Eventos de auth/error/requests | Inspeccionar summary, response, logs y artifacts | Sin passwords, hashes, JWT completos, secretos ni stack/SQL sensible | I,API,SQL,M |

## Errores y contrato HTTP

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-ERR-001 | P1/S2 | RF-COM-004, D-0046 | Endpoint con DTO | Enviar JSON malformado, tipo/enum inválido, campo desconocido y query inválida | `400` con `MALFORMED_JSON/VALIDATION_ERROR/UNKNOWN_FIELD/INVALID_QUERY_PARAMETER` | W,API |
| TC-ERR-002 | P1/S2 | RF-COM-005, RN-019 | IDs válidos pero inexistentes | Consultar/relacionar recursos | `404 RESOURCE_NOT_FOUND` o `CURRENT_BOM_NOT_FOUND`; forma común | W,I,API |
| TC-ERR-003 | P1/S2 | RF-COM-006 | Preparar duplicado/stock/estado/concurrencia | Provocar cada conflicto | `409` con código estable específico; cero cambio parcial | U,I,API |
| TC-ERR-004 | P0/S1 | RF-COM-007, RNF-SEC-004 | Handler con excepción controlada de test | Provocar error inesperado sin comprometer datos | `500 INTERNAL_ERROR`; mensaje genérico, correlation ID; sin stack/SQL/clase | W,I,API |
| TC-ERR-005 | P1/S2 | RF-COM-008, D-0025 | Requests exitosos y fallidos | Enviar correlation válido/ausente/inválido | Válido se propaga, ausente se genera, inválido devuelve `400 VALIDATION_ERROR`; todas responses declaran header | U,W,I,API |

## No funcionales, plataforma y accesibilidad

| ID | P/S | Trazabilidad | Precondición | Procedimiento resumido | Resultado esperado | Capas |
|---|---|---|---|---|---|---|
| TC-NFR-001 | P1/S3 | RNF-PER-002, D-0043 | Colección >100 | Probar defaults, page vacío, size 1/20/100 y size 0/101/sort no permitido | Envelope/totales correctos; límites inválidos `400`; nunca array ilimitado | I,API |
| TC-NFR-002 | P0/S1 | D-0038, OI-015 | Datos límite | Enviar/recibir `0.000001`, máximo, BIGINT grande, escala 7 y overflow; mostrar en UI | Valores exactos como strings; inválidos rechazados; UI no usa Number destructivo | U,I,API,E2E,M |
| TC-NFR-003 | P0/S1 | D-0017, RNF-DAT-002 | MySQL vacío y schema con versión anterior soportada | Ejecutar Flyway y arrancar JPA validate | Migraciones reproducibles; schema esperado; fallo visible ante divergencia | I,SQL |
| TC-NFR-004 | P1/S2 | RNF-REL-002 | Docker/Compose implementado | Arrancar desde limpio y consultar health de db/backend/frontend | Servicios healthy en orden tolerante; sin secretos en output | API,M |
| TC-NFR-005 | P1/S3 | RNF-PER-001 | Ambiente/dataset Locust documentados | Ejecutar workload de lecturas 20 usuarios/5 min tras warm-up | p95 lecturas ≤500 ms, error técnico <1%, contexto completo | PERF |
| TC-NFR-006 | P1/S3 | RNF-PER-001 | Ambiente/dataset exclusivo | Ejecutar escrituras controladas | p95 escrituras ≤1 s, error técnico <1%; conflictos intencionales separados | PERF |
| TC-NFR-007 | P0/S1 | RNF-SEC-001/002/004 | Build, repo, imágenes/config | Revisar secretos, hashes, logs, responses y variables | Sin secretos/credenciales reales versionados o expuestos; passwords con hash adaptativo | I,API,M |
| TC-NFR-008 | P2/S3 | RNF-USA-001/002 | UI completa, viewport laptop y móvil | Recorrer flujo con teclado, foco, labels, zoom 200%, contraste y mensajes | Flujo operable/comprensible; sin corte crítico; hallazgos registrados | M |

## Mapeo de los once E2E obligatorios

| E2E | Caso | Preparación aislada | Assertion principal |
|---|---|---|---|
| E2E-001 | TC-AUT-001 | Usuario OPERATOR activo | Navega a dashboard con sesión válida |
| E2E-002 | TC-AUT-002 | Login limpio | Mensaje estable y sin sesión |
| E2E-003 | TC-PRD-001 | ADMIN + SKU `runId` | Producto aparece y response `201` |
| E2E-004 | TC-PRD-002 | Producto creado por setup | Error `DUPLICATE_SKU` visible |
| E2E-005 | TC-INV-001 | OPERATOR + material saldo cero | Balance incrementa y movimiento existe |
| E2E-006 | TC-ORD-001 | OPERATOR + producto/BOM | Orden DRAFT visible |
| E2E-007 | TC-ORD-006 | Stock parcial controlado | `INSUFFICIENT_STOCK`, orden sin confirmar |
| E2E-008 | TC-ORD-009/010 | Orden CONFIRMED con reserva | COMPLETED y reservas cero |
| E2E-009 | TC-QLT-001 | COMPLETED sin inspección | Inspección APPROVED visible |
| E2E-010 | TC-QLT-002/003 | Otra COMPLETED | REJECTED exige/guarda observación |
| E2E-011 | TC-SEC-006 | Tres sesiones/roles aislados | UI y API bloquean acción prohibida |

## Checklist de ejecución manual por caso

Antes:

- Registrar build/commit, ambiente, browser/cliente, schema/migration y `runId`.
- Confirmar precondiciones mediante API/SQL, sin reutilizar estado desconocido.
- Marcar caso `NOT_RUN` o `BLOCKED`; nunca asumir PASS.

Durante:

- Ejecutar exactamente el procedimiento y capturar IDs/versiones/correlation IDs.
- Verificar resultado visible, respuesta HTTP y efecto persistente relevante.
- Sanitizar evidencia inmediatamente.

Después:

- Marcar `PASS`, `FAIL`, `BLOCKED` o `SKIPPED` con razón.
- Abrir defecto por fallo reproducible y enlazar caso/evidencia.
- Destruir/reiniciar el ambiente efímero según la suite.

## Estado de ejecución

- Casos diseñados: **98**.
- Casos ejecutados: **0**.
- PASS: **0**.
- FAIL: **0**.
- BLOCKED: **0**.
- NOT_RUN: **98**.
