# FASE 4 — CONTRATO DE API

## 1. Nombre de la fase

**Fase 4 — Contrato de API REST y OpenAPI preliminar.**

### Inicio obligatorio de continuidad

#### Resumen del estado anterior

- Las Fases 0, 1, 2 y 3 están completadas y aprobadas.
- La Fase 3 definió 12 tablas, sus relaciones, restricciones, índices, auditoría, estados, scripts SQL, fixture y consultas de validación.
- Los scripts SQL son artefactos de diseño: no se han ejecutado aún contra MySQL y no constituyen migraciones Flyway integradas.
- Todavía no existe código backend ni frontend.

#### Decisiones que siguen vigentes

- Monolito modular Spring Boot, SPA React y una base MySQL.
- API versionada bajo `/api/v1`, DTOs separados de las entidades JPA y validación por capas.
- JWT Bearer HS256 de 15 minutos, sin refresh token ni blacklist; validación de usuario activo y `authVersion` en cada petición.
- `ADMIN` administra usuarios, productos, materiales y BOM; `OPERATOR` opera inventario y órdenes; `QUALITY_INSPECTOR` registra inspecciones.
- Confirmar reserva, cancelar libera y completar consume; el snapshot de BOM queda fijado al confirmar.
- `DRAFT → CONFIRMED → IN_PROGRESS → COMPLETED`; `CANCELLED` es terminal desde estados no terminales.
- Una única inspección final e inmutable por orden completada.
- Borrado lógico para catálogos, ledgers inmutables y eventos de auditoría correlacionados.

#### Archivos existentes utilizados

- `docs/phase-0-discovery.md`
- `docs/phase-1-requirements.md`
- `docs/phase-2-architecture.md`
- `docs/phase-3-data-model.md`
- `docs/database/phase-3-initial-schema.sql`
- `docs/database/phase-3-test-data.sql`
- `docs/database/phase-3-validation.sql`
- Los siete documentos de continuidad de la raíz.

#### Objetivo de la fase actual

Congelar una interfaz HTTP coherente y comprobable antes de implementar controladores: recursos, operaciones, DTOs, validaciones, permisos, respuestas, errores, ejemplos y un contrato OpenAPI 3.1 preliminar.

#### Criterios para considerar terminada la fase

1. Cada capacidad aprobada tiene una operación HTTP o una decisión explícita de no exponerla.
2. Métodos, rutas, DTOs, códigos y permisos no contradicen requisitos ni modelo de datos.
3. Todas las mutaciones relevantes tienen validación, conflicto, autorización y trazabilidad definidos.
4. El error JSON y la paginación son uniformes.
5. El OpenAPI preliminar se puede parsear y contiene todas las operaciones documentadas.
6. No se implementan controladores, servicios, repositorios ni código de aplicación.
7. Documentos de continuidad y trazabilidad quedan actualizados.
8. El usuario aprueba avanzar.

## 2. Objetivo

Proporcionar una fuente contractual para backend, frontend, Postman/Newman, Cypress, pruebas de integración y Swagger UI. La fase define comportamiento observable; no demuestra todavía que la API exista o funcione.

## 3. Dependencias

- Alcance y exclusiones aprobados en Fase 0.
- Requisitos, reglas, permisos y escenarios aprobados en Fase 1.
- límites de módulos, seguridad, transacciones y despliegue aprobados en Fase 2.
- Entidades, precisión, estados, auditoría e integridad aprobados en Fase 3.
- La implementación futura dependerá de Spring Web, Validation, Security, Jackson, Springdoc y la persistencia aprobada, pero esta fase no instala dependencias.

## 4. Entradas requeridas

No se requiere una decisión adicional del usuario para diseñar el contrato. Se toman como entradas firmes:

- Prefijo `/api/v1`.
- Roles `ADMIN`, `OPERATOR`, `QUALITY_INSPECTOR`.
- Estados y enumeraciones de Fase 3.
- Respuestas mínimas `400`, `401`, `403`, `404`, `409` y `500`.
- Fechas UTC, cantidades `DECIMAL(19,6)` y locking optimista/pesimista según el caso.
- Alcance MVP sin refresh token, borrado físico, edición de inspecciones ni consumo de producción manual.

## 5. Entregables

1. Catálogo completo de operaciones REST.
2. Catálogo de DTOs de entrada y salida.
3. Códigos HTTP y cabeceras.
4. Convenciones de nombres, filtros, orden y paginación.
5. Matriz de autorización por operación.
6. Validaciones estructurales, cruzadas y de negocio.
7. Contrato uniforme de errores y códigos estables.
8. Ejemplos JSON del flujo principal y escenarios negativos.
9. `docs/api/openapi.json`, OpenAPI 3.1 preliminar parseable.
10. Actualización de estado, roadmap, decisiones, manifiesto, matriz, incidencias y notas de entrevista.

## 6. Decisiones tomadas

Estas decisiones entraron en vigor con la aprobación explícita de la Fase 4 el 2026-07-19:

1. Los campos HTTP usan inglés y `lowerCamelCase`; rutas usan sustantivos plurales en `kebab-case`.
2. IDs, versiones y decimales se serializan como strings para no perder precisión en JavaScript; backend los convierte a `Long` y `BigDecimal`.
3. Las colecciones usan paginación base cero y un envelope común; no se devuelven arrays ilimitados.
4. Los cambios parciales usan `PATCH` con `version` obligatoria; un desfase de versión responde `409`.
5. Las transiciones de órdenes usan subrecursos de acción con `POST`; repetir una transición ya aplicada responde `409`.
6. No existen endpoints `DELETE`; la desactivación de catálogos es lógica y las entidades históricas son inmutables.
7. El logout es local en React: elimina token y sesión; no existe endpoint de logout porque el MVP no mantiene sesión ni blacklist en servidor.
8. Los códigos de error son estables en inglés; `message` es humano en español y no debe usarse para lógica de cliente.
9. `X-Correlation-Id` enlaza respuesta, auditoría y logs; el servidor lo genera cuando el cliente no lo envía.
10. OpenAPI 3.1 es el contrato de diseño; durante implementación deberá mantenerse alineado con Springdoc y pruebas contractuales.

## 7. Diseño o código

No se genera código de aplicación. El diseño contractual se detalla a continuación.

### 7.1 Límite y representación

- URL local directa prevista: `http://localhost:8080/api/v1`.
- Media type de entrada/salida: `application/json` UTF-8.
- Seguridad por defecto: `Authorization: Bearer <jwt>`; solo login es público.
- Timestamps: ISO 8601 UTC, por ejemplo `2026-07-19T15:30:00Z`.
- IDs y versiones: strings decimales positivos o no negativos, por ejemplo `"42"`, `"0"`.
- Cantidades: strings decimales sin signo, hasta 13 enteros y 6 decimales, por ejemplo `"1250.500000"`.
- Valores enum: mayúsculas exactas según Fase 3.
- Campos desconocidos en requests: se rechazan con `400 UNKNOWN_FIELD` para detectar errores de contrato.
- Strings: se recortan en bordes antes de validar; una cadena de espacios se considera vacía.
- Campos `null`: solo se aceptan cuando el DTO los declara opcionales; omitir no equivale a asignar `null` en un `PATCH`.
- Las contraseñas, hashes, JWT, `authVersion` y columnas normalizadas nunca aparecen en respuestas.

### 7.2 Convenciones HTTP

| Tema | Convención |
|---|---|
| Recursos | Sustantivos plurales: `/products`, `/production-orders`. |
| Subrecursos | Relación estable: `/products/{productId}/boms`. |
| Acciones de estado | `POST /production-orders/{orderId}/confirm`, `/start`, `/complete`, `/cancel`. |
| Crear | `POST`, respuesta `201 Created`, body y `Location`. |
| Consultar | `GET`, respuesta `200 OK`. |
| Modificar parcialmente | `PATCH`, respuesta `200 OK` o `204 No Content` para contraseña. |
| Eliminar | No expuesto en el MVP. |
| Paginación | `page=0`, `size=20`; máximo `100`. |
| Orden | `sort=campo,asc|desc`; un criterio en el MVP y whitelist por recurso. |
| Búsqueda | `search` normalizado, máximo 100 caracteres. |
| Booleanos | `true`/`false`, sin equivalentes `1`, `yes` o strings. |
| Fechas de filtro | `from` inclusivo y `to` exclusivo en UTC; `from < to`. |
| Concurrencia | `version` obligatoria en todo `PATCH`; conflicto `CONCURRENT_MODIFICATION`. |
| Reintentos | `GET` es seguro; mutaciones no deben reintentarse automáticamente sin reconciliar el resultado. |

### 7.3 Cabeceras

| Cabecera | Dirección | Regla |
|---|---|---|
| `Authorization` | Request | Bearer requerido salvo `/auth/login`. |
| `Content-Type` | Ambos | `application/json` en requests con body y respuestas JSON. |
| `Accept` | Request | Se admite `application/json`; otro formato puede responder `406` durante implementación. |
| `X-Correlation-Id` | Ambos | Opcional en request, 1–64 caracteres `[A-Za-z0-9._-]`; siempre presente en response. |
| `Location` | Response | URI relativa del recurso creado en respuestas `201`. |
| `Cache-Control` | Response login | `no-store` para impedir cachear token/credenciales. |
| `WWW-Authenticate` | Response `401` | `Bearer` sin revelar la causa sensible del fallo. |

### 7.4 Paginación y filtros

Envelope común:

```json
{
  "content": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

Reglas:

- `page < 0`, `size < 1`, `size > 100`, dirección inválida o campo no permitido: `400 INVALID_QUERY_PARAMETER`.
- Un `page` posterior al último no es error: devuelve `content: []` con metadatos reales.
- Defaults: recursos de catálogo por `createdAt,desc`; ledgers por su timestamp descendente; órdenes por `createdAt,desc`.
- Whitelists: usuarios `username,displayName,createdAt`; productos/materiales `name,createdAt`; BOM `revisionNumber,createdAt`; movimientos `occurredAt`; órdenes `orderNumber,createdAt,updatedAt`; inspecciones `inspectedAt`; auditoría `occurredAt`.
- `search` busca identificador y nombre donde aplique; no implementa full-text.

### 7.5 Catálogo de DTOs

#### Comunes

| DTO | Campos |
|---|---|
| `ActorSummary` | `id`, `username`, `displayName`, `role`. |
| `PageMetadata` | `number`, `size`, `totalElements`, `totalPages`, `first`, `last`. |
| `PageResponse<T>` | `content: T[]`, `page: PageMetadata`. |
| `ErrorResponse` | `timestamp`, `status`, `error`, `code`, `message`, `path`, `correlationId`, `fieldErrors`. |
| `FieldError` | `field`, `code`, `message`; nunca eco de secretos. |

#### Autenticación y usuarios

| DTO | Campos y validación principal |
|---|---|
| `LoginRequest` | `username` requerido 3–100; `password` requerido 8–128. |
| `LoginResponse` | `accessToken`, `tokenType: BEARER`, `expiresInSeconds: 900`, `user: ActorSummary`. |
| `CurrentUserResponse` | `user: ActorSummary`. |
| `CreateUserRequest` | `username` 3–100; `displayName` 1–150; `password` 8–128; `role`; `active` opcional/default true. |
| `UpdateUserRequest` | `version` requerido; al menos uno de `displayName`, `role`, `active`. |
| `ChangePasswordRequest` | `version` requerido; `newPassword` 8–128. |
| `UserResponse` | `id`, `username`, `displayName`, `role`, `active`, `version`, `createdAt`, `updatedAt`. |

No se permite cambiar `username` después de crear. Cambiar contraseña, rol o estado incrementa `authVersion` internamente e invalida tokens previos.

#### Productos y materiales

| DTO | Campos y validación principal |
|---|---|
| `CreateProductRequest` | `sku` 1–64; `name` 1–150; `description` opcional hasta 500; `active` opcional/default true. |
| `UpdateProductRequest` | `version`; al menos uno de `sku`, `name`, `description`, `active`, con los mismos límites. |
| `ProductResponse` | `id`, `sku`, `name`, `description`, `active`, `version`, timestamps y actores. |
| `CreateMaterialRequest` | `code` 1–64; `name` 1–150; `description` opcional hasta 500; `unitOfMeasure`; `active` opcional/default true. |
| `UpdateMaterialRequest` | `version`; al menos uno de los campos mutables, con los mismos límites. |
| `MaterialResponse` | `id`, `code`, `name`, `description`, `unitOfMeasure`, `active`, `version`, timestamps y actores. |

`description: null` en update elimina la descripción. La unidad solo puede cambiar mientras el material no tenga BOM, movimientos, reserva ni uso histórico; de lo contrario `409 MATERIAL_UNIT_IMMUTABLE`.

#### BOM

| DTO | Campos y validación principal |
|---|---|
| `PublishBomRequest` | `items` requerido, 1–100 líneas. |
| `BomItemRequest` | `materialId`; `quantityPerUnit` positiva. |
| `BomItemResponse` | `materialId`, `materialCode`, `materialName`, `unitOfMeasure`, `quantityPerUnit`. |
| `BomResponse` | `id`, producto resumido, `revisionNumber`, `current`, `items`, `createdAt`, `createdBy`, datos de superseded opcionales. |
| `BomSummaryResponse` | Revisión, vigencia, cantidad de líneas, timestamps. |

`materialId` no puede repetirse dentro del request. Publicar sustituye la revisión vigente en una transacción y crea una nueva revisión; no edita filas históricas.

#### Inventario

| DTO | Campos y validación principal |
|---|---|
| `InventoryBalanceResponse` | Material resumido, `onHandQuantity`, `reservedQuantity`, `availableQuantity`, `version`, `updatedAt`, `updatedBy`. |
| `CreateInventoryMovementRequest` | `materialId`, `type` manual, `quantity` positiva; `referenceCode` hasta 120 o `reason` 1–500, al menos uno. |
| `InventoryMovementResponse` | `id`, material, `type`, `direction`, `quantity`, saldos posteriores, referencias, actor, fecha y `correlationId`. |

Solo se admiten en request `ENTRY`, `MANUAL_OUT`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`. `PRODUCTION_CONSUMPTION` es generado únicamente al completar una orden.

#### Órdenes

| DTO | Campos y validación principal |
|---|---|
| `CreateProductionOrderRequest` | `productId`, `quantity` positiva. |
| `UpdateProductionOrderRequest` | `version`; al menos uno de `productId`, `quantity`; solo en `DRAFT`. |
| `OrderActionRequest` | `version`; `reason` opcional hasta 500, obligatorio para cancelar. |
| `ProductionOrderSummaryResponse` | `id`, `orderNumber`, producto resumido, `quantity`, `status`, inspección `PENDING/APPROVED/REJECTED`, `version`, timestamps. |
| `ProductionOrderResponse` | Summary más BOM/revisión opcional, snapshot `materials`, actores y enlaces lógicos de acciones disponibles. |
| `ProductionOrderMaterialResponse` | Material, cantidad por unidad, requerida y reserva activa. |
| `OrderStatusHistoryResponse` | `sequenceNumber`, `fromStatus`, `toStatus`, actor, fecha y correlación. |

El número de orden lo genera el servidor y no puede enviarse ni cambiarse. Crear exige producto activo con BOM vigente; confirmar vuelve a leer la BOM vigente, bloquea balances ordenados, crea snapshot y reserva.

#### Calidad, dashboard y auditoría

| DTO | Campos y validación principal |
|---|---|
| `CreateApprovedInspectionRequest` | `productionOrderId`, `result: APPROVED`, `observations` opcional hasta 1000. |
| `CreateRejectedInspectionRequest` | `productionOrderId`, `result: REJECTED`, `observations` requerida 1–1000 después de trim. |
| `QualityInspectionResponse` | `id`, orden/producto resumidos, `result`, `observations`, inspector, fecha y correlación. |
| `AdminDashboardResponse` | Catálogos activos, órdenes por estado, materiales sin disponible, inspecciones por resultado. |
| `OperatorDashboardResponse` | Órdenes por estado, órdenes propias recientes, materiales sin disponible. |
| `InspectorDashboardResponse` | Completadas sin inspección e inspecciones recientes por resultado. |
| `AuditEventResponse` | `id`, actor snapshot, `action`, recurso, `outcome`, `summary`, path, fecha y correlación. |

### 7.6 Endpoints, métodos, autorización y respuestas

`401` puede ocurrir en toda ruta protegida; `403` cuando el JWT es válido pero el rol/alcance no autoriza. `500` aplica a toda operación inesperada y conserva el envelope. Las tablas muestran además las respuestas específicas.

#### Autenticación

| Método y ruta | Operación | Rol/alcance | Éxito | Errores específicos |
|---|---|---|---|---|
| `POST /auth/login` | Autenticar y emitir JWT. | Público; solo usuario activo. | `200` | `400`, `401` |
| `GET /auth/me` | Consultar perfil del token. | Cualquier rol activo. | `200` | `401` |

No hay endpoint de logout. React borra memoria y `sessionStorage`; el token sigue siendo válido hasta expiración salvo cambio de `authVersion` o desactivación.

#### Usuarios

| Método y ruta | Operación | Rol | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /users` | Listar; filtros `search`, `role`, `active`, paginación. | ADMIN | `200` | `400`, `403` |
| `POST /users` | Crear usuario. | ADMIN | `201` | `400`, `403`, `409` duplicado |
| `GET /users/{userId}` | Consultar detalle. | ADMIN | `200` | `403`, `404` |
| `PATCH /users/{userId}` | Cambiar nombre, rol o estado. | ADMIN | `200` | `400`, `403`, `404`, `409` último admin/concurrencia |
| `PATCH /users/{userId}/password` | Establecer nueva contraseña e invalidar tokens. | ADMIN | `204` | `400`, `403`, `404`, `409` concurrencia |

#### Productos

| Método y ruta | Operación | Rol/alcance | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /products` | Listar; `search`, `active`, paginación. | ADMIN, OPERATOR | `200` | `400`, `403` |
| `POST /products` | Crear. | ADMIN | `201` | `400`, `403`, `409` SKU |
| `GET /products/{productId}` | Consultar. | ADMIN, OPERATOR | `200` | `403`, `404` |
| `PATCH /products/{productId}` | Editar/desactivar lógicamente. | ADMIN | `200` | `400`, `403`, `404`, `409` SKU/concurrencia |

`QUALITY_INSPECTOR` ve el producto resumido dentro de órdenes completadas e inspecciones, pero recibe `403` en endpoints de catálogo.

#### Materiales

| Método y ruta | Operación | Rol | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /materials` | Listar; `search`, `active`, `unitOfMeasure`, paginación. | ADMIN, OPERATOR | `200` | `400`, `403` |
| `POST /materials` | Crear y crear balance cero. | ADMIN | `201` | `400`, `403`, `409` código |
| `GET /materials/{materialId}` | Consultar. | ADMIN, OPERATOR | `200` | `403`, `404` |
| `PATCH /materials/{materialId}` | Editar/desactivar. | ADMIN | `200` | `400`, `403`, `404`, `409` código/unidad/concurrencia |

#### BOM

| Método y ruta | Operación | Rol | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /products/{productId}/boms` | Listar revisiones paginadas. | ADMIN, OPERATOR | `200` | `400`, `403`, `404` producto |
| `POST /products/{productId}/boms` | Publicar nueva revisión y reemplazar vigente. | ADMIN | `201` | `400`, `403`, `404`, `409` inactivo/concurrencia |
| `GET /products/{productId}/boms/current` | Consultar vigente. | ADMIN, OPERATOR | `200` | `403`, `404` producto o BOM vigente |
| `GET /products/{productId}/boms/{revisionNumber}` | Consultar revisión histórica. | ADMIN, OPERATOR | `200` | `400`, `403`, `404` |

#### Inventario

| Método y ruta | Operación | Rol | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /inventory/balances` | Listar; `search`, `active`, `outOfStock`, paginación. | ADMIN, OPERATOR | `200` | `400`, `403` |
| `GET /inventory/balances/{materialId}` | Consultar saldo. | ADMIN, OPERATOR | `200` | `403`, `404` |
| `GET /inventory/movements` | Historial; filtros de material, tipo, fechas, orden y actor. | ADMIN, OPERATOR | `200` | `400`, `403` |
| `POST /inventory/movements` | Registrar entrada/salida/ajuste manual. | OPERATOR | `201` | `400`, `403`, `404`, `409` stock/inactivo |
| `GET /inventory/movements/{movementId}` | Consultar movimiento inmutable. | ADMIN, OPERATOR | `200` | `403`, `404` |

No hay update/delete de balances o movimientos ni endpoint público para `PRODUCTION_CONSUMPTION`.

#### Órdenes de producción

| Método y ruta | Operación | Rol/alcance | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /production-orders` | Listar; `search`, `status`, `productId`, fechas, `inspectionStatus`. | ADMIN/OPERATOR todas; QUALITY_INSPECTOR solo COMPLETED | `200` | `400`, `403` |
| `POST /production-orders` | Crear `DRAFT`. | OPERATOR | `201` | `400`, `403`, `404`, `409` producto/BOM |
| `GET /production-orders/{orderId}` | Consultar detalle. | ADMIN/OPERATOR; inspector solo COMPLETED | `200` | `403`, `404` |
| `PATCH /production-orders/{orderId}` | Cambiar producto/cantidad en `DRAFT`. | OPERATOR | `200` | `400`, `403`, `404`, `409` estado/concurrencia/BOM |
| `GET /production-orders/{orderId}/history` | Consultar transiciones. | Mismo alcance del detalle | `200` | `403`, `404` |
| `POST /production-orders/{orderId}/confirm` | Validar BOM/stock, snapshot y reserva. | OPERATOR | `200` | `400`, `403`, `404`, `409` estado/stock/BOM/concurrencia |
| `POST /production-orders/{orderId}/start` | `CONFIRMED → IN_PROGRESS`. | OPERATOR | `200` | `400`, `403`, `404`, `409` estado/concurrencia |
| `POST /production-orders/{orderId}/complete` | Consumir reserva y completar atómicamente. | OPERATOR | `200` | `400`, `403`, `404`, `409` estado/integridad/concurrencia |
| `POST /production-orders/{orderId}/cancel` | Cancelar y liberar reserva; `reason` requerida. | OPERATOR | `200` | `400`, `403`, `404`, `409` terminal/concurrencia |

#### Inspecciones, dashboard y auditoría

| Método y ruta | Operación | Rol/alcance | Éxito | Errores específicos |
|---|---|---|---|---|
| `GET /quality-inspections` | Listar; filtros resultado, orden, producto, inspector y fechas. | ADMIN, QUALITY_INSPECTOR | `200` | `400`, `403` |
| `POST /quality-inspections` | Crear inspección final. | QUALITY_INSPECTOR | `201` | `400`, `403`, `404`, `409` estado/duplicada |
| `GET /quality-inspections/{inspectionId}` | Consultar inspección. | ADMIN, QUALITY_INSPECTOR | `200` | `403`, `404` |
| `GET /dashboard` | Obtener vista determinada exclusivamente por rol del token. | Todos los roles | `200` | `403` si rol no soportado |
| `GET /audit-events` | Listar por actor, acción, recurso, resultado, correlación y fechas. | ADMIN | `200` | `400`, `403` |
| `GET /audit-events/{auditEventId}` | Consultar evento. | ADMIN | `200` | `403`, `404` |

### 7.7 Reglas de autorización y visibilidad

1. El rol se obtiene del usuario validado, no de headers ni del body.
2. Un token válido de rol incorrecto produce `403 ACCESS_DENIED` sin ejecutar la operación ni modificar datos.
3. `QUALITY_INSPECTOR` recibe colecciones de órdenes filtradas siempre a `COMPLETED`; solicitar explícitamente otro estado produce `403 ORDER_SCOPE_FORBIDDEN`.
4. Si un inspector conoce el ID de una orden no completada, detalle e historial responden `403`, no se degrada a `404`, conforme a la matriz aprobada.
5. `ADMIN` puede leer todos los módulos, pero no registrar movimientos, operar órdenes ni crear inspecciones.
6. `OPERATOR` puede leer catálogos/BOM necesarios, pero no usuarios, inspecciones ni auditoría global.
7. El contexto de producto para un inspector se entrega embebido en orden/inspección; no amplía acceso al catálogo.
8. Usuario inactivo o `authVersion` distinto se trata como autenticación inválida (`401`), aun si el JWT no expiró.

### 7.8 Validaciones

#### Estructurales — `400`

- JSON mal formado, media type incorrecto, campo desconocido o tipo incompatible.
- Campos requeridos ausentes; strings vacíos; longitudes excedidas.
- IDs/rutas no numéricos o no positivos; versiones no numéricas o negativas.
- Cantidades que no coinciden con precisión/rango o no son positivas cuando aplica.
- Enum, fecha, booleano, paginación, sort o filtro inválido.
- Request de `PATCH` sin ningún cambio además de `version`.
- BOM vacía, más de 100 líneas o material repetido.
- Movimiento sin `referenceCode` ni `reason`.
- Cancelación sin `reason`.
- Rechazo sin observaciones no vacías.

#### Referencias — `404`

- Recurso objetivo inexistente.
- Producto/material/orden relacionado inexistente.
- BOM vigente inexistente al consultar `/current`.

La ausencia de BOM necesaria para crear/confirmar una orden es `409 BOM_NOT_DEFINED`, porque el producto sí existe pero no está listo para el proceso.

#### Conflictos de negocio — `409`

- Username, SKU o código normalizado duplicado.
- Último ADMIN activo intentado desactivar/degradar.
- Versión desactualizada.
- Producto/material inactivo para una operación nueva.
- Cambio de unidad con historial/referencias.
- BOM modificada concurrentemente o material inactivo.
- Stock disponible insuficiente o riesgo de saldo negativo.
- Estado/transición inválidos, orden terminal o no editable.
- Snapshot/reserva inconsistente al completar.
- Orden no completada o ya inspeccionada.

#### Rango de cantidades

- Formato público: hasta 13 dígitos enteros y 1–6 decimales.
- El backend usa `BigDecimal`, normaliza a escala 6 y rechaza redondeo implícito.
- Antes de confirmar se valida que `quantity × quantityPerUnit` cabe en `DECIMAL(19,6)`; desborde: `409 QUANTITY_OVERFLOW`.

### 7.9 Manejo de errores

Envelope:

```json
{
  "timestamp": "2026-07-19T15:32:11.245Z",
  "status": 409,
  "error": "Conflict",
  "code": "INSUFFICIENT_STOCK",
  "message": "No hay stock disponible suficiente para confirmar la orden.",
  "path": "/api/v1/production-orders/42/confirm",
  "correlationId": "qa-demo-00042",
  "fieldErrors": []
}
```

Reglas:

- `code`, no `message`, es la interfaz estable para frontend y tests.
- `500` usa `INTERNAL_ERROR`, mensaje genérico y correlation ID; nunca expone stack trace, SQL, clase Java, JWT o datos sensibles.
- Errores de contraseña usan el campo `password`/`newPassword` sin valor rechazado.
- Login usa siempre `INVALID_CREDENTIALS` para usuario inexistente, contraseña incorrecta o usuario inactivo, evitando enumeración.
- Violaciones de autorización y negocio importantes se auditan con `REJECTED`; fallos inesperados con `FAILURE` cuando sea seguro registrar el evento.

Códigos mínimos:

| HTTP | Código estable | Uso |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | Campos/body inválidos. |
| 400 | `MALFORMED_JSON` | JSON no parseable. |
| 400 | `UNKNOWN_FIELD` | Campo no contractual. |
| 400 | `INVALID_QUERY_PARAMETER` | Paginación/filtro/sort inválido. |
| 401 | `INVALID_CREDENTIALS` | Login fallido sin revelar causa. |
| 401 | `INVALID_TOKEN` | Firma/formato/usuario/version inválidos. |
| 401 | `TOKEN_EXPIRED` | JWT expirado. |
| 403 | `ACCESS_DENIED` | Rol sin permiso. |
| 403 | `ORDER_SCOPE_FORBIDDEN` | Inspector fuera de órdenes completadas. |
| 404 | `RESOURCE_NOT_FOUND` | ID/recurso inexistente. |
| 404 | `CURRENT_BOM_NOT_FOUND` | Consulta de BOM vigente ausente. |
| 409 | `DUPLICATE_USERNAME` | Username normalizado repetido. |
| 409 | `DUPLICATE_SKU` | SKU normalizado repetido. |
| 409 | `DUPLICATE_MATERIAL_CODE` | Código normalizado repetido. |
| 409 | `LAST_ACTIVE_ADMIN` | Se conservaría sin ADMIN activo. |
| 409 | `CONCURRENT_MODIFICATION` | Versión desactualizada/contención no recuperada. |
| 409 | `RESOURCE_INACTIVE` | Recurso inactivo para una operación nueva. |
| 409 | `MATERIAL_UNIT_IMMUTABLE` | Cambio de unidad inseguro. |
| 409 | `BOM_NOT_DEFINED` | Producto sin BOM vigente operable. |
| 409 | `BOM_CONCURRENTLY_CHANGED` | Publicación concurrente. |
| 409 | `INSUFFICIENT_STOCK` | Disponible menor al requerido. |
| 409 | `NEGATIVE_INVENTORY_CONFLICT` | Operación dejaría saldo negativo. |
| 409 | `INVALID_ORDER_TRANSITION` | Estado origen no admite acción. |
| 409 | `ORDER_IMMUTABLE` | Orden terminal intentada modificar. |
| 409 | `RESERVATION_INTEGRITY_CONFLICT` | Reserva/snapshot no cuadra al completar. |
| 409 | `ORDER_NOT_COMPLETED` | Inspección sobre orden no completada. |
| 409 | `INSPECTION_ALREADY_EXISTS` | Segunda inspección. |
| 409 | `QUANTITY_OVERFLOW` | Resultado fuera de DECIMAL(19,6). |
| 500 | `INTERNAL_ERROR` | Error no previsto sanitizado. |

### 7.10 Ejemplos JSON

#### Login correcto

Request:

```json
{
  "username": "operator.demo",
  "password": "Demo123!"
}
```

Response `200`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.demo.signature",
  "tokenType": "BEARER",
  "expiresInSeconds": 900,
  "user": {
    "id": "2",
    "username": "operator.demo",
    "displayName": "Demo Operator",
    "role": "OPERATOR"
  }
}
```

El token mostrado es deliberadamente no funcional y solo documenta la forma.

#### Crear material y producto

`POST /materials`:

```json
{
  "code": "MAT-AL-01",
  "name": "Aluminum sheet",
  "description": "1.5 mm sheet",
  "unitOfMeasure": "KILOGRAM",
  "active": true
}
```

Response `201`:

```json
{
  "id": "101",
  "code": "MAT-AL-01",
  "name": "Aluminum sheet",
  "description": "1.5 mm sheet",
  "unitOfMeasure": "KILOGRAM",
  "active": true,
  "version": "0",
  "createdAt": "2026-07-19T15:40:00Z",
  "createdBy": {
    "id": "1",
    "username": "admin.demo",
    "displayName": "Demo Administrator",
    "role": "ADMIN"
  },
  "updatedAt": "2026-07-19T15:40:00Z",
  "updatedBy": {
    "id": "1",
    "username": "admin.demo",
    "displayName": "Demo Administrator",
    "role": "ADMIN"
  }
}
```

`POST /products`:

```json
{
  "sku": "FG-PANEL-01",
  "name": "Control panel",
  "description": "Finished assembly",
  "active": true
}
```

#### Publicar BOM

`POST /products/201/boms`:

```json
{
  "items": [
    { "materialId": "101", "quantityPerUnit": "2.500000" },
    { "materialId": "102", "quantityPerUnit": "4.000000" }
  ]
}
```

Response `201`:

```json
{
  "id": "301",
  "product": { "id": "201", "sku": "FG-PANEL-01", "name": "Control panel" },
  "revisionNumber": 1,
  "current": true,
  "items": [
    {
      "materialId": "101",
      "materialCode": "MAT-AL-01",
      "materialName": "Aluminum sheet",
      "unitOfMeasure": "KILOGRAM",
      "quantityPerUnit": "2.500000"
    },
    {
      "materialId": "102",
      "materialCode": "MAT-SC-01",
      "materialName": "Steel screw",
      "unitOfMeasure": "PIECE",
      "quantityPerUnit": "4.000000"
    }
  ],
  "createdAt": "2026-07-19T15:45:00Z",
  "createdBy": {
    "id": "1",
    "username": "admin.demo",
    "displayName": "Demo Administrator",
    "role": "ADMIN"
  },
  "supersededAt": null,
  "supersededBy": null
}
```

#### Entrada de inventario

`POST /inventory/movements`:

```json
{
  "materialId": "101",
  "type": "ENTRY",
  "quantity": "100.000000",
  "referenceCode": "PO-7781",
  "reason": "Supplier receipt"
}
```

Response `201` incluye:

```json
{
  "id": "401",
  "material": { "id": "101", "code": "MAT-AL-01", "name": "Aluminum sheet" },
  "type": "ENTRY",
  "direction": "IN",
  "quantity": "100.000000",
  "onHandAfter": "100.000000",
  "reservedAfter": "0.000000",
  "referenceCode": "PO-7781",
  "reason": "Supplier receipt",
  "occurredAt": "2026-07-19T16:00:00Z",
  "correlationId": "qa-demo-00401"
}
```

#### Crear, confirmar y completar orden

`POST /production-orders`:

```json
{ "productId": "201", "quantity": "10.000000" }
```

Response `201`: orden `DRAFT`, `orderNumber` generado y `version: "0"`.

`POST /production-orders/501/confirm`:

```json
{ "version": "0" }
```

Response `200` (campos centrales):

```json
{
  "id": "501",
  "orderNumber": "PO-20260719-0001",
  "product": { "id": "201", "sku": "FG-PANEL-01", "name": "Control panel" },
  "quantity": "10.000000",
  "status": "CONFIRMED",
  "bom": { "id": "301", "revisionNumber": 1 },
  "materials": [
    {
      "materialId": "101",
      "materialCode": "MAT-AL-01",
      "unitOfMeasure": "KILOGRAM",
      "quantityPerUnit": "2.500000",
      "requiredQuantity": "25.000000",
      "activeReservedQuantity": "25.000000"
    }
  ],
  "version": "1"
}
```

Después de `start` con versión `"1"`, `complete` recibe la versión nueva `"2"`. Su `200` muestra `COMPLETED`, versión incrementada y reservas activas `"0.000000"`; los consumos aparecen en el ledger, no se aceptan en el request.

#### Inspección rechazada válida

`POST /quality-inspections`:

```json
{
  "productionOrderId": "501",
  "result": "REJECTED",
  "observations": "Torque measurement outside tolerance."
}
```

Response `201`:

```json
{
  "id": "601",
  "productionOrder": {
    "id": "501",
    "orderNumber": "PO-20260719-0001",
    "product": { "id": "201", "sku": "FG-PANEL-01", "name": "Control panel" }
  },
  "result": "REJECTED",
  "observations": "Torque measurement outside tolerance.",
  "inspectedBy": {
    "id": "3",
    "username": "inspector.demo",
    "displayName": "Demo Inspector",
    "role": "QUALITY_INSPECTOR"
  },
  "inspectedAt": "2026-07-19T17:00:00Z",
  "correlationId": "qa-demo-00601"
}
```

#### Validación `400`

```json
{
  "timestamp": "2026-07-19T17:01:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "La solicitud contiene datos inválidos.",
  "path": "/api/v1/quality-inspections",
  "correlationId": "qa-demo-00602",
  "fieldErrors": [
    {
      "field": "observations",
      "code": "requiredForRejected",
      "message": "Las observaciones son obligatorias para un rechazo."
    }
  ]
}
```

#### Permiso `403`

```json
{
  "timestamp": "2026-07-19T17:02:00Z",
  "status": 403,
  "error": "Forbidden",
  "code": "ACCESS_DENIED",
  "message": "No tienes permiso para ejecutar esta operación.",
  "path": "/api/v1/inventory/movements",
  "correlationId": "qa-demo-00603",
  "fieldErrors": []
}
```

### 7.11 Contrato OpenAPI preliminar

`docs/api/openapi.json` usa OpenAPI 3.1.0 e incluye:

- Servidor local y prefijo versionado.
- Esquema `bearerAuth` JWT.
- Tags por módulo.
- Todas las operaciones de 7.6 con `operationId` único.
- Parámetros de path, filtros y paginación.
- Schemas de request/response, enums y precisión pública.
- Respuestas reutilizables `400`, `401`, `403`, `404`, `409`, `500`.
- Ejemplos representativos sin token o secreto real.

Es preliminar: en Fase 7 se conectará a Springdoc y en Fases 8–13 se comprobará que el contrato generado/servido no diverja. El JSON de esta fase no prueba que exista una API desplegada.

### 7.12 Trazabilidad del flujo principal

| Paso | Operación contractual |
|---:|---|
| 1 | `POST /auth/login` |
| 2 | `POST /materials` como ADMIN |
| 3 | `POST /products` como ADMIN |
| 4 | `POST /products/{productId}/boms` como ADMIN |
| 5 | `POST /inventory/movements` tipo ENTRY como OPERATOR |
| 6 | `POST /production-orders` como OPERATOR |
| 7–8 | `POST /production-orders/{orderId}/confirm`; stock se valida/reserva atómicamente |
| 9 | `POST /production-orders/{orderId}/start` |
| 10 | `POST /production-orders/{orderId}/complete`; se crean consumos y descuenta físico |
| 11–12 | `POST /quality-inspections` como QUALITY_INSPECTOR |
| 13 | GET de historial/movimientos/inspección/auditoría con `correlationId` |

## 8. Archivos creados

- `docs/phase-4-api-contract.md`
- `docs/api/openapi.json`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-3-data-model.md` (solo registro de aprobación)

## 10. Comandos

No se ejecutó aplicación, servidor, base de datos, Swagger UI ni pruebas HTTP porque aún no existen. Se aplicarán comprobaciones estáticas al OpenAPI:

```powershell
node -e "const fs=require('fs'); const api=JSON.parse(fs.readFileSync('docs/api/openapi.json','utf8')); console.log(api.openapi, Object.keys(api.paths).length)"
```

También se comprobó el estado Git previsto:

```powershell
git -C D:\ status --short -- qaflow
```

El comando respondió que no existe un repositorio Git; se registró como `OI-016` y no se inicializó fuera de la Fase 6.

También se ejecutó una auditoría Node.js de rutas, `operationId`, `$ref`, seguridad, respuestas, correlación, precisión y paridad con este documento. Resultado real: OpenAPI 3.1.0 parseable; 28 rutas, 39 operaciones, 71 schemas y 538 referencias; 39 `operationId` únicos, cero referencias rotas, cero diferencias con el catálogo, cero `DELETE`, solo `login` público, correlación declarada en todas las respuestas y contratos de cantidad conformes a los límites seleccionados. No se encontró instalado un linter OpenAPI/Swagger; por eso no se afirma validación oficial del estándar.

## 11. Cómo verificar

1. Parsear `docs/api/openapi.json` como JSON.
2. Comprobar `openapi: 3.1.0`, servidor, seguridad y tags.
3. Confirmar que cada endpoint de 7.6 existe en `paths` con el mismo método.
4. Confirmar que cada `operationId` es único y toda referencia `$ref` resuelve.
5. Confirmar que toda mutación protegida declara `400/401/403/404/409/500` cuando aplica.
6. Revisar que no existan operaciones `DELETE`, logout servidor ni consumo manual.
7. Cruzar permisos con la matriz aprobada de Fase 1.
8. Cruzar enums, campos y precisión con Fase 3.
9. En implementación futura, servir Swagger, ejecutar pruebas contractuales y validar ejemplos contra respuestas reales.

## 12. Pruebas relacionadas

En esta fase no se ejecutan pruebas funcionales. Se preparan contratos para:

- Unitarias de validadores y servicios.
- Integración de seguridad, persistencia, transacciones y errores.
- API Postman/Newman por módulo y casos negativos.
- E2E Cypress del flujo principal y permisos.
- SQL de trazabilidad e integridad.
- Contract tests que comparen rutas, schemas y errores con OpenAPI.

`TEST_MATRIX.md` debe distinguir la validación estática del contrato de una prueba de API ejecutada.

## 13. Riesgos

- El OpenAPI puede divergir de Springdoc si no se añade una comprobación automática en Fases 7–13.
- Strings para IDs/cantidades preservan precisión, pero requieren conversión explícita en frontend, fixtures y tests.
- `PATCH` con versión en body no usa ETag/`If-Match`; es simple para el MVP, menos idiomático para clientes HTTP genéricos.
- Acciones `POST` no son idempotentes; un timeout obliga al cliente a consultar el estado antes de reintentar.
- El `403` explícito para una orden fuera del alcance del inspector revela que el ID existe; se conserva por la regla aprobada de permiso, pero debe revisarse si cambia el modelo de amenaza.
- `UNKNOWN_FIELD` exige configurar Jackson de forma estricta y mantener compatibilidad cuidadosamente.
- El catálogo de códigos de error deberá mantenerse sincronizado entre excepciones, frontend y tests.
- No hay evidencia runtime: MySQL, Spring, JWT, Swagger y HTTP siguen sin verificarse.
- El directorio `D:\qaflow` aún no es un repositorio Git; se inicializará en la fase de estructura, no durante este contrato.

## 14. Estado actualizado

- Fases 0–3: completadas y aprobadas.
- Fase 4: completada y aprobada el 2026-07-19.
- Código backend/frontend: no creado.
- OpenAPI: contrato preliminar de diseño con auditoría estática local aprobada; sin linter oficial ni runtime.
- API/Swagger ejecutados: no.
- Pruebas HTTP: no ejecutadas.

## 15. Próximo paso

Se inició únicamente la Fase 5 para definir estrategia general de QA, matriz, casos manuales, pruebas unitarias/integración/API/E2E/SQL/rendimiento, regresión, defectos, automatización, prioridad y severidad.

## 16. Registro de aprobación

Fase 4 aprobada explícitamente por el usuario el 2026-07-19. La aprobación confirma el contrato de diseño; no convierte la auditoría estática en evidencia de una API ejecutada.
