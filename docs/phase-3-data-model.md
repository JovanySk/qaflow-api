# Fase 3 — Modelo de datos

## 1. Nombre de la fase

Fase 3. Modelo de datos.

### Inicio obligatorio de continuidad

1. **Resumen del estado anterior:** las Fases 0, 1 y 2 fueron aprobadas el 2026-07-19. Existen alcance, requisitos y arquitectura; todavía no existe backend ni migración Flyway integrada.
2. **Decisiones vigentes:** monolito modular, MySQL/InnoDB, Flyway, JPA en modo validate, REST con DTOs, reservas transaccionales, locks pesimistas, JWT con `authVersion`, auditoría append-only y pruebas de integración con MySQL.
3. **Archivos existentes utilizados:** los siete documentos de continuidad y `docs/phase-0-discovery.md`, `docs/phase-1-requirements.md`, `docs/phase-2-architecture.md`.
4. **Objetivo de la fase actual:** convertir requisitos e invariantes en un modelo relacional MySQL concreto, con SQL inicial, fixture determinista y consultas de validación.
5. **Criterios de terminación:** entidades, campos, tipos, relaciones, constraints, índices, auditoría, enumeraciones y ER documentados; SQL real creado; datos y validaciones coherentes; estado actualizado; aprobación explícita.

## 2. Objetivo

Diseñar una fuente de verdad relacional que preserve trazabilidad histórica, impida duplicados y cantidades inválidas, soporte reservas concurrentes y permita demostrar integridad mediante consultas SQL independientes de la API.

## 3. Dependencias

- Fase 2 aprobada.
- Requisitos RF/RNF y reglas RN-001 a RN-036.
- Decisiones D-0001 a D-0025.
- MySQL 8.0.16 o superior para enforcement de `CHECK`.
- Flyway futuro para aplicar el esquema y JPA/Hibernate futuro para validarlo.

## 4. Entradas requeridas

- Roles y permisos.
- Catálogos de productos/materiales.
- BOM vigente y snapshot al confirmar.
- Stock físico, reservado y disponible.
- Máquina de estados de órdenes.
- Una inspección final por orden completada.
- Auditoría, correlación, UTC e inmutabilidad histórica.

No se requiere una decisión externa adicional para revisar la propuesta. La forma JSON de DTOs se mantiene fuera de esta fase.

## 5. Entregables

- Catálogo de 12 entidades/tablas.
- Campos, nulabilidad, tipos SQL y tipos Java previstos.
- Relaciones y cardinalidades.
- Restricciones, claves e índices.
- Enumeraciones.
- Estrategia de auditoría e inmutabilidad.
- Diagrama entidad-relación textual.
- Script de esquema MySQL.
- Fixture de prueba determinista.
- 27 consultas de validación.
- Matriz de reglas que requieren servicio además de constraint.

## 6. Decisiones tomadas

Estas decisiones entraron en vigor con la aprobación explícita de la Fase 3 el 2026-07-19:

1. Tablas y columnas en `snake_case`; entidades Java futuras en singular/PascalCase.
2. PK `BIGINT` autoincremental y claves de negocio únicas separadas.
3. Cantidades `DECIMAL(19,6)` / `BigDecimal`; nunca `float` o `double`.
4. Fechas `DATETIME(6)` escritas y leídas en UTC.
5. Enumeraciones como `VARCHAR` con `CHECK`, no como tipo propietario `ENUM` de MySQL.
6. Identificadores normalizados mediante columnas generadas `UPPER(TRIM(...))` con índices únicos.
7. BOM versionada: se inserta una revisión nueva y se conserva la anterior.
8. Una columna generada y única permite solo una BOM vigente por producto.
9. La reserva detallada vive en el snapshot `production_order_materials`; el balance conserva su agregado.
10. Historial, movimientos, inspecciones, líneas de BOM y auditoría son append-only mediante triggers donde son completamente inmutables.
11. Todas las FK usan `ON DELETE RESTRICT`; los maestros se desactivan lógicamente.
12. Los datos demo viven en un script separado y no deben cargarse en producción.

## 7. Diseño o código

### 7.1 Convenciones físicas y mapeo Java

| Concepto | SQL | Java previsto | Regla |
|---|---|---|---|
| Identificador técnico | `BIGINT` | `Long` | PK interna; nunca se reutiliza. |
| Cantidad | `DECIMAL(19,6)` | `BigDecimal` | Máximo 13 enteros y 6 decimales; redondeo explícito. |
| Fecha/hora | `DATETIME(6)` | `Instant` | Conexión, JVM y scripts en UTC. |
| Booleano | `TINYINT(1)` | `Boolean` | `CHECK` restringe a 0/1. |
| Enum | `VARCHAR` + `CHECK` | `enum` con `EnumType.STRING` | Sin ordinales. |
| Optimistic version | `BIGINT` | `Long` con `@Version` | Usuarios, maestros, balance y orden. |
| JSON auditado | `JSON` | `JsonNode`/tipo JSON | Solo resumen sanitizado. |
| Normalización | Columna generada almacenada | Campo read-only | `UPPER(TRIM(valor))`. |

Los valores de negocio deben estar entre `0.000001` y `9999999999999.999999` cuando sean positivos. La aplicación validará multiplicaciones antes de persistir; la base refuerza rango físico y signo.

### 7.2 Catálogo de entidades

| Entidad futura | Tabla | Propósito |
|---|---|---|
| `User` | `users` | Credenciales, rol, estado y versión de autorización. |
| `Product` | `products` | Producto fabricado y SKU único. |
| `Material` | `materials` | Material inventariable, código y unidad. |
| `Bom` | `boms` | Revisión histórica y revisión vigente por producto. |
| `BomItem` | `bom_items` | Material/cantidad por unidad dentro de una revisión. |
| `InventoryBalance` | `inventory_balances` | Stock físico, reservado y disponible por material. |
| `ProductionOrder` | `production_orders` | Cabecera, producto, BOM confirmada y estado actual. |
| `ProductionOrderMaterial` | `production_order_materials` | Snapshot y reserva detallada por material. |
| `InventoryMovement` | `inventory_movements` | Ledger inmutable de entradas y salidas físicas. |
| `ProductionOrderStatusHistory` | `production_order_status_history` | Secuencia inmutable de transiciones. |
| `QualityInspection` | `quality_inspections` | Resultado único e inmutable de una orden completada. |
| `AuditEvent` | `audit_events` | Evento transversal, resultado y correlación. |

No existen tablas independientes para dashboard, roles, refresh tokens o blacklist JWT.

### 7.3 Campos por entidad

#### `users`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK autoincremental. |
| `username` | `VARCHAR(100)` | No | Identificador visible, 3–100 caracteres no vacíos. |
| `username_normalized` | `VARCHAR(100)` generado | No | `UPPER(TRIM(username))`, único. |
| `display_name` | `VARCHAR(150)` | No | Nombre visible no vacío. |
| `password_hash` | `VARCHAR(255)` | No | Hash con identificador de algoritmo; nunca se expone. |
| `role` | `VARCHAR(32)` | No | Uno de los tres roles. |
| `active` | `TINYINT(1)` | No | Impide nuevas sesiones y valida tokens. |
| `auth_version` | `BIGINT` | No | Invalida JWT previos al cambiar seguridad. |
| `version` | `BIGINT` | No | Optimistic locking. |
| `created_at/updated_at` | `DATETIME(6)` | No | Auditoría UTC. |
| `created_by/updated_by` | `BIGINT` FK users | Sí | Nulo solo para bootstrap del primer administrador. |

Índices: username normalizado único, `(role, active)` y `created_at`.

#### `products`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `sku` | `VARCHAR(64)` | No | Clave visible no vacía. |
| `sku_normalized` | `VARCHAR(64)` generado | No | SKU normalizado y único. |
| `name` | `VARCHAR(150)` | No | Nombre no vacío. |
| `description` | `VARCHAR(500)` | Sí | Texto opcional. |
| `active` | `TINYINT(1)` | No | Desactivación lógica. |
| `version` | `BIGINT` | No | Optimistic locking. |
| `created_at/updated_at` | `DATETIME(6)` | No | Auditoría UTC. |
| `created_by/updated_by` | `BIGINT` FK users | No | Actor responsable. |

Índices: SKU normalizado único, `(active, name)`, `created_at`.

#### `materials`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `code` | `VARCHAR(64)` | No | Código visible. |
| `code_normalized` | `VARCHAR(64)` generado | No | Código normalizado y único. |
| `name` | `VARCHAR(150)` | No | Nombre no vacío. |
| `description` | `VARCHAR(500)` | Sí | Texto opcional. |
| `unit_of_measure` | `VARCHAR(32)` | No | Unidad sin conversiones automáticas. |
| `active` | `TINYINT(1)` | No | Desactivación lógica. |
| `version` | `BIGINT` | No | Optimistic locking. |
| `created_at/updated_at` | `DATETIME(6)` | No | Auditoría UTC. |
| `created_by/updated_by` | `BIGINT` FK users | No | Actor responsable. |

Índices: código normalizado único, `(active, name)`, unidad y fecha.

#### `boms`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK de revisión. |
| `product_id` | `BIGINT` FK products | No | Producto propietario. |
| `revision_number` | `INT` | No | Secuencia positiva por producto. |
| `is_current` | `TINYINT(1)` | No | Revisión vigente. |
| `current_product_id` | `BIGINT` generado | Sí | Product ID solo si vigente; unique permite una vigente. |
| `created_at/created_by` | Fecha + FK users | No | Publicación de revisión. |
| `superseded_at/superseded_by` | Fecha + FK users | Sí | Obligatorios juntos cuando deja de ser vigente. |

Índices: `(product_id, revision_number)` único, `current_product_id` único y `(product_id, is_current)`.

#### `bom_items`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `bom_id` | `BIGINT` FK boms | No | Revisión propietaria. |
| `material_id` | `BIGINT` FK materials | No | Material requerido. |
| `quantity_per_unit` | `DECIMAL(19,6)` | No | Mayor que cero. |
| `created_at` | `DATETIME(6)` | No | Momento de publicación. |

Único `(bom_id, material_id)`. Las filas no se actualizan ni eliminan; reemplazar BOM crea otra revisión completa.

#### `inventory_balances`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `material_id` | `BIGINT` FK materials | No | PK compartida, una fila por material. |
| `on_hand_quantity` | `DECIMAL(19,6)` | No | Stock físico ≥ 0. |
| `reserved_quantity` | `DECIMAL(19,6)` | No | Agregado reservado ≥ 0. |
| `available_quantity` | `DECIMAL(19,6)` generado | No | `on_hand - reserved`, ≥ 0. |
| `version` | `BIGINT` | No | Optimistic version adicional; locks críticos son pesimistas. |
| `updated_at` | `DATETIME(6)` | No | Último cambio. |
| `updated_by` | `BIGINT` FK users | No | Actor de la última operación. |

La fila se crea con el material y saldo cero. Índices sobre disponible y fecha apoyan dashboard/diagnóstico.

#### `production_orders`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `order_number` | `VARCHAR(40)` | No | Identificador de negocio. |
| `order_number_normalized` | `VARCHAR(40)` generado | No | Único normalizado. |
| `product_id` | `BIGINT` FK products | No | Producto fabricado. |
| `bom_id` | `BIGINT` FK boms | Sí | Nulo en DRAFT; revisión fijada al confirmar. |
| `quantity` | `DECIMAL(19,6)` | No | Cantidad positiva del producto. |
| `status` | `VARCHAR(32)` | No | Estado actual. |
| `version` | `BIGINT` | No | Optimistic version y detección adicional. |
| `created_at/updated_at` | `DATETIME(6)` | No | Fechas UTC. |
| `created_by/updated_by` | `BIGINT` FK users | No | Operador responsable. |

Índices: número único, `(status, created_at)`, `(product_id, status)`, BOM y creador/fecha.

#### `production_order_materials`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `production_order_id` | `BIGINT` FK orders | No | Orden propietaria. |
| `material_id` | `BIGINT` FK materials | No | Material snapshot. |
| `quantity_per_unit` | `DECIMAL(19,6)` | No | Copia de BOM al confirmar. |
| `required_quantity` | `DECIMAL(19,6)` | No | Orden × cantidad por unidad, escala 6. |
| `active_reserved_quantity` | `DECIMAL(19,6)` | No | Igual a requerida en estados activos; cero en terminales. |
| `created_at` | `DATETIME(6)` | No | Momento de confirmación. |

Único `(production_order_id, material_id)`. Preserva requerimiento aunque la BOM cambie.

#### `inventory_movements`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `material_id` | `BIGINT` FK materials | No | Material afectado. |
| `movement_type` | `VARCHAR(32)` | No | Tipo permitido. |
| `direction` | `VARCHAR(3)` | No | `IN` o `OUT`, consistente con tipo. |
| `quantity` | `DECIMAL(19,6)` | No | Siempre positiva. |
| `on_hand_after/reserved_after` | `DECIMAL(19,6)` | No | Snapshot posterior del saldo en ese instante. |
| `production_order_id` | `BIGINT` FK orders | Sí | Obligatorio solo para consumo de producción. |
| `reference_code` | `VARCHAR(120)` | Sí | Documento/referencia manual. |
| `reason` | `VARCHAR(500)` | Sí | Justificación o descripción. |
| `performed_by` | `BIGINT` FK users | No | Operador. |
| `occurred_at` | `DATETIME(6)` | No | Momento UTC. |
| `correlation_id` | `VARCHAR(64)` | No | Enlace con petición/log/auditoría. |

Un índice único evita duplicar consumo por orden/material/tipo. Múltiples movimientos manuales siguen permitidos porque su order ID es nulo.

#### `production_order_status_history`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `production_order_id` | `BIGINT` FK orders | No | Orden. |
| `sequence_number` | `INT` | No | Secuencia positiva y única por orden. |
| `from_status` | `VARCHAR(32)` | Sí | Nulo solo en creación. |
| `to_status` | `VARCHAR(32)` | No | Destino permitido por CHECK. |
| `changed_by` | `BIGINT` FK users | No | Operador. |
| `changed_at` | `DATETIME(6)` | No | Fecha UTC. |
| `correlation_id` | `VARCHAR(64)` | No | Correlación. |

El `CHECK` admite únicamente las transiciones de la Fase 1; la aplicación mantiene estado actual y secuencia en la misma transacción.

#### `quality_inspections`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `production_order_id` | `BIGINT` FK orders | No | Único: máximo una inspección. |
| `result` | `VARCHAR(16)` | No | APPROVED/REJECTED. |
| `observations` | `VARCHAR(1000)` | Sí | Obligatoria y no vacía para REJECTED. |
| `inspected_by` | `BIGINT` FK users | No | Inspector. |
| `inspected_at` | `DATETIME(6)` | No | Fecha UTC. |
| `correlation_id` | `VARCHAR(64)` | No | Correlación. |

Inmutable por trigger. El servicio comprueba que la orden esté COMPLETED y el actor tenga permiso.

#### `audit_events`

| Campo | Tipo | Nulo | Propósito/restricción |
|---|---|---:|---|
| `id` | `BIGINT` | No | PK. |
| `actor_user_id` | `BIGINT` FK users | Sí | Nulo para actor no autenticado/sistema. |
| `actor_username_snapshot` | `VARCHAR(100)` | Sí | Identidad histórica sanitizada. |
| `actor_role_snapshot` | `VARCHAR(32)` | Sí | Rol al ocurrir el evento. |
| `action` | `VARCHAR(64)` | No | Código estable extensible. |
| `resource_type/resource_id` | `VARCHAR(64)` | No/Sí | Recurso sin FK polimórfica. |
| `outcome` | `VARCHAR(16)` | No | SUCCESS/REJECTED/FAILURE. |
| `summary` | `JSON` | Sí | Datos mínimos, sin secretos. |
| `request_path` | `VARCHAR(255)` | Sí | Contexto HTTP cuando aplica. |
| `occurred_at` | `DATETIME(6)` | No | Fecha UTC. |
| `correlation_id` | `VARCHAR(64)` | No | Unión con respuesta/log. |

Índices por fecha, actor, recurso, acción/resultado y correlación. Inmutable por trigger.

### 7.4 Enumeraciones

| Enum Java futuro | Valores persistidos |
|---|---|
| `Role` | `ADMIN`, `OPERATOR`, `QUALITY_INSPECTOR` |
| `UnitOfMeasure` | `PIECE`, `KILOGRAM`, `GRAM`, `LITER`, `MILLILITER`, `METER`, `CENTIMETER` |
| `ProductionOrderStatus` | `DRAFT`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `QualityResult` | `APPROVED`, `REJECTED` |
| `InventoryMovementType` | `ENTRY`, `MANUAL_OUT`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `PRODUCTION_CONSUMPTION` |
| `InventoryDirection` | `IN`, `OUT` |
| `AuditOutcome` | `SUCCESS`, `REJECTED`, `FAILURE` |

`AuditAction` y `resource_type` usan códigos string estables, no un `CHECK`, para permitir nuevos eventos sin migrar una lista cerrada.

### 7.5 Relaciones y cardinalidades

```text
users 1 -------- N products/materials/boms/orders/movements/history/inspections
  |               (created_by, updated_by o actor)
  +------------- N audit_events (actor opcional)

products 1 ------ N boms
products 1 ------ N production_orders

boms 1 ---------- N bom_items N ---------- 1 materials
  |                                      |
  +------------- N production_orders     +--- 1 inventory_balances
                                                |
materials 1 ----- N production_order_materials  +--- N inventory_movements
                        N
                        |
production_orders 1 ----+
        |
        +------------- N production_order_status_history
        +------------- N inventory_movements
        +------------- 0..1 quality_inspections
```

Reglas cardinales adicionales:

- Un producto tiene cero o muchas revisiones BOM, pero como máximo una vigente.
- Una BOM contiene uno o más items; la no-vaciedad se valida en servicio/transacción.
- Un material tiene exactamente un balance una vez creado.
- Una orden DRAFT no tiene snapshot; al confirmar tiene uno o más materiales.
- Una orden tiene uno o más registros de estado, empezando en DRAFT.
- Una orden completada tiene cero o una inspección; nunca más de una.

### 7.6 Índices y objetivo de consulta

| Tabla | Índices principales | Uso |
|---|---|---|
| `users` | username unique; role/active | Login, permisos, último ADMIN. |
| `products` | SKU unique; active/name | CRUD, filtros y selección. |
| `materials` | code unique; active/name; unit | CRUD, BOM e inventario. |
| `boms` | product/revision unique; current product unique | Revisión vigente e historia. |
| `bom_items` | BOM/material unique; material | Cargar BOM y detectar referencias. |
| `inventory_balances` | PK material; available | Lock por ID y dashboard sin stock. |
| `production_orders` | number unique; status/date; product/status | Listas operativas y dashboard. |
| `production_order_materials` | order/material unique; material/reserved | Reservas agregadas y locks. |
| `inventory_movements` | material/date; type/date; order; actor; correlation | Historial y reconciliación. |
| `production_order_status_history` | order/sequence unique; status/date; actor | Trazabilidad y último estado. |
| `quality_inspections` | order unique; result/date; inspector/date | Duplicado, dashboard y filtros. |
| `audit_events` | date; actor/date; resource/date; action/outcome/date; correlation | Auditoría paginada y diagnóstico. |

Los índices se verificarán con planes reales en las fases de implementación/rendimiento; su presencia no demuestra por sí sola rendimiento.

### 7.7 Restricciones y nivel de enforcement

| Regla | Base de datos | Servicio/transacción | Validación futura |
|---|---|---|---|
| SKU/código/username único normalizado | Columna generada + unique | Traduce colisión a 409 | Integración concurrente |
| Cantidades positivas | `CHECK` | Bean Validation y límites | Unit/API/SQL |
| Saldo no negativo | `CHECK` balance | Locks y cálculo atómico | Concurrencia + SQL |
| BOM no vacía | No expresable con FK simple | Inserta cabecera/items en una transacción | Integración + V-006 |
| Una BOM vigente | Unique sobre columna generada | Supersede/crea atómicamente | Integración + V-005 |
| Material único en BOM | Unique compuesto | Validación previa | API/SQL |
| Reserva agregada coherente | No es constraint entre agregados | Actualiza snapshot y balance juntos | Concurrencia + V-009/V-010 |
| Estado actual = último historial | No es constraint cruzado | Mismo lock/transacción | Integración + V-017 |
| Transiciones válidas | `CHECK` en historial | Máquina de estados | Unit/API/SQL |
| Solo inspeccionar COMPLETED | FK no puede comprobar estado | Servicio con lock/consulta | Integración + V-019 |
| Una inspección | Unique order | Maneja colisión 409 | Integración concurrente |
| Rechazo con observación | `CHECK` | Validación de DTO/dominio | Unit/API/SQL |
| Último ADMIN activo | No es constraint de fila | Lock/consulta de usuarios | Integración |
| Permisos por rol | No se confía a FK | Spring Security | Security/API/E2E |
| Inmutabilidad | Triggers en ledger/historia | Sin operaciones update/delete | Integración negativa |

### 7.8 Auditoría y trazabilidad

La trazabilidad utiliza tres mecanismos complementarios:

1. **Campos de creación/modificación** en entidades mutables.
2. **Ledgers específicos:** movimientos, snapshot de orden, historial de estados e inspecciones.
3. **`audit_events`:** acción transversal, resultado, actor snapshot y correlación.

No se almacenan contraseñas claras, JWT, secretos ni payloads completos. El JSON de auditoría debe usar una lista permitida de campos. Los triggers rechazan update/delete en las tablas completamente inmutables.

### 7.9 Diagrama de estados respaldado por datos

```text
NULL -> DRAFT
DRAFT -> CONFIRMED -> IN_PROGRESS -> COMPLETED
   \          \              \
    +----------+---------------> CANCELLED

COMPLETED y CANCELLED no tienen transición de salida.
```

La cabecera conserva el estado actual para consultas rápidas y el historial conserva la secuencia defendible. Ambos se escriben dentro de la misma transacción.

### 7.10 Scripts SQL

| Archivo | Contenido | Estado de ejecución |
|---|---|---|
| `docs/database/phase-3-initial-schema.sql` | 12 tablas, constraints, índices y 10 triggers. | Creado; no ejecutado. |
| `docs/database/phase-3-test-data.sql` | Fixture determinista con todos los estados y dos resultados de calidad. | Creado; no ejecutado. |
| `docs/database/phase-3-validation.sql` | 27 consultas read-only de integridad y resumen. | Creado; no ejecutado. |

Estos archivos son artefactos de diseño. Su conversión a migraciones Flyway y su ejecución contra MySQL se realizarán después de aprobar estructura/inicialización.

### 7.11 Datos de prueba

El fixture contiene:

- Tres usuarios: `ADMIN`, `OPERATOR`, `QUALITY_INSPECTOR`.
- Dos productos y cuatro materiales.
- Dos BOM vigentes con seis líneas.
- Cuatro balances y diez movimientos.
- Seis órdenes que cubren los cinco estados.
- Reservas activas en una orden CONFIRMED y una IN_PROGRESS.
- Dos órdenes completadas con consumos.
- Una inspección APPROVED y una REJECTED con observación.
- Un evento `DEMO_DATA_LOADED` que identifica la carga masiva como fixture, no como evidencia del pipeline de auditoría.

Las credenciales conocidas (`password`) y su hash BCrypt son solo para test/demo. La carga debe quedar excluida de producción; autenticación real no se declara verificada en esta fase.

Balances esperados del fixture:

| Material | Físico | Reservado | Disponible |
|---|---:|---:|---:|
| MAT-BLT-001 | 480.000000 | 56.000000 | 424.000000 |
| MAT-CAB-001 | 97.000000 | 12.000000 | 85.000000 |
| MAT-PNT-001 | 29.500000 | 1.250000 | 28.250000 |
| MAT-STL-001 | 93.800000 | 17.300000 | 76.500000 |

### 7.12 Consultas de validación

Las 27 consultas comprueban:

- Duplicados normalizados.
- Existencia de ADMIN activo.
- BOM vigente, no vacía y sin referencias inactivas.
- Stock no negativo y fórmula de disponible.
- Reconciliación de reservas detalladas/agregadas.
- Reconciliación de stock físico con movimientos.
- Consumo por orden completada.
- Snapshot y cálculo de requerimientos.
- Coherencia de estado actual/historial.
- Secuencia de transiciones.
- Reglas y actor de inspección.
- Roles de actores del fixture.
- Campos de auditoría.
- Existencia de triggers de inmutabilidad.
- Conteos y balances esperados del fixture.

## 8. Archivos creados

- `docs/phase-3-data-model.md`
- `docs/database/phase-3-initial-schema.sql`
- `docs/database/phase-3-test-data.sql`
- `docs/database/phase-3-validation.sql`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-2-architecture.md` (solo estado de aprobación)

## 10. Comandos

No se ejecutó MySQL, Flyway, JPA ni la aplicación. Se usaron comprobaciones estáticas para contar tablas, constraints, triggers, identificadores y referencias.

Comandos de ejecución futura, no ejecutados todavía:

```powershell
mysql --user=<user> --password <database>
```

Después, desde el cliente MySQL:

```sql
SOURCE docs/database/phase-3-initial-schema.sql;
SOURCE docs/database/phase-3-test-data.sql;
SOURCE docs/database/phase-3-validation.sql;
```

Las rutas pueden requerir su forma absoluta según el cliente. Credenciales reales no deben escribirse en scripts ni historial de shell.

## 11. Cómo verificar

1. Aplicar el esquema a MySQL vacío compatible.
2. Comprobar que las 12 tablas y 10 triggers existen.
3. Cargar el fixture una sola vez.
4. Ejecutar las validaciones V-001 a V-027.
5. Confirmar que consultas de violaciones devuelven cero filas.
6. Confirmar conteos y balances esperados.
7. Intentar duplicados, negativos y update/delete de tablas inmutables dentro de un entorno desechable.
8. Ejecutar después las pruebas concurrentes previstas; esta fase no las sustituye.

## 12. Pruebas relacionadas

No se ejecutaron pruebas. El esquema y las consultas son entradas para integración MySQL, API, SQL, concurrencia y auditoría. `TEST_MATRIX.md` conserva estado `Planificada/No ejecutada`.

## 13. Riesgos

- El SQL no se ha ejecutado todavía contra una versión real de MySQL.
- `DECIMAL(19,6)` puede desbordarse si el servicio no valida multiplicaciones.
- Triggers de inmutabilidad requieren estrategia de limpieza mediante schemas efímeros, no deletes normales.
- La coherencia de agregados y estados cruza tablas y depende también de transacciones del servicio.
- Las credenciales demo conocidas deben estar estrictamente aisladas de producción.
- Índices y locks aún deben medirse bajo concurrencia real.
- El mapeo exacto Hibernate de columnas generadas/JSON debe comprobarse en Fase 7.

## 14. Estado actualizado

- Fases 0–2: completadas y aprobadas.
- Fase 3: completada y aprobada el 2026-07-19.
- Backend implementado: no.
- SQL creado: sí, como artefacto de diseño.
- SQL ejecutado: no.
- Pruebas ejecutadas: ninguna.

## 15. Próximo paso

Se inició la Fase 4 para definir endpoints, métodos HTTP, DTOs, códigos, ejemplos JSON, validaciones, autorización, manejo de errores, convenciones y OpenAPI preliminar.

## 16. Registro de aprobación

Fase 3 aprobada explícitamente por el usuario el 2026-07-19. La aprobación no cambia el hecho de que los scripts SQL todavía no se han ejecutado contra MySQL.
