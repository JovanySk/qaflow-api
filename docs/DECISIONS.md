# DECISIONS

## D-0001 — Ejecución secuencial con compuerta de aprobación

- **Decisión:** ejecutar una sola fase a la vez y requerir aprobación explícita antes de avanzar.
- **Motivo:** preservar coherencia, permitir revisión humana y evitar generar una solución masiva sin validar sus bases.
- **Alternativas consideradas:** implementar todo el MVP de una vez; trabajar por módulos sin documentación de estado.
- **Consecuencias:** progreso más controlado; cada fase debe actualizar los documentos; una entrega preparada no se marca completada antes de la aprobación.
- **Fecha/fase:** 2026-07-19, Fase 0.

## D-0002 — Alcance de MVP en lugar de ERP

- **Decisión:** limitar el producto a autenticación, catálogos, BOM, inventario, órdenes, calidad, dashboard, trazabilidad y capacidades de QA/DevOps.
- **Motivo:** el objetivo es demostrar profundidad técnica y calidad en una entrevista, no cubrir toda la gestión empresarial.
- **Alternativas consideradas:** agregar compras, ventas, proveedores, MRP, múltiples almacenes y costeo.
- **Consecuencias:** el flujo principal puede completarse y probarse; las exclusiones se documentan y cualquier expansión requerirá una nueva decisión.
- **Fecha/fase:** 2026-07-19, Fase 0.

## D-0003 — Evidencia antes de declarar éxito

- **Decisión:** no declarar exitosa una compilación, prueba, conexión, contenedor o pipeline que no haya sido ejecutado.
- **Motivo:** el portafolio debe ser defendible y distinguir diseño, implementación y verificación.
- **Alternativas consideradas:** asumir resultados a partir de inspección estática o plantillas conocidas.
- **Consecuencias:** `TEST_MATRIX.md` y la documentación indicarán `Planificada`, `No ejecutada`, `Aprobada` o `Fallida` con evidencia concreta.
- **Fecha/fase:** 2026-07-19, Fase 0.

## D-0004 — Trazabilidad como requisito transversal

- **Decisión:** registrar actor, fecha y operación para cambios importantes, además del historial propio de inventario y estados.
- **Motivo:** la trazabilidad conecta el dominio de manufactura con objetivos de QA, investigación de defectos y auditoría.
- **Alternativas consideradas:** limitarse a marcas de creación/modificación; depender únicamente de logs de aplicación.
- **Consecuencias:** el modelo de datos y servicios deberán diseñar auditoría explícita sin exponer datos sensibles; el mecanismo exacto se decidirá en Fases 2–3.
- **Fecha/fase:** 2026-07-19, Fase 0.

## D-0005 — Consumo de materiales al completar la orden

- **Decisión:** mantener el flujo solicitado: validar disponibilidad antes de confirmar y descontar materiales cuando la orden se completa.
- **Motivo:** representa el proceso funcional definido en el brief y separa compromiso operativo de consumo real.
- **Alternativas consideradas:** descontar al confirmar; descontar al iniciar; no controlar compromisos.
- **Consecuencias:** debe definirse una estrategia de reserva o control de concurrencia para impedir que varias órdenes confirmadas comprometan el mismo stock. La decisión de implementación queda abierta y no puede posponerse más allá del diseño de inventario/órdenes.
- **Fecha/fase:** 2026-07-19, Fase 0.

## D-0006 — Convenciones de idioma

- **Decisión:** usar inglés para nombres de código, entidades técnicas, campos y rutas de API; mantener la documentación de trabajo en español.
- **Motivo:** facilitar convenciones habituales del ecosistema y conservar claridad para la revisión actual.
- **Alternativas consideradas:** todo en español; todo en inglés.
- **Consecuencias:** se evitarán traducciones inconsistentes y se documentará un glosario si algún término de negocio resulta ambiguo.
- **Fecha/fase:** 2026-07-19, Fase 0.

## D-0007 — Separación de funciones con mínimo privilegio

> Estado de D-0007 a D-0013: aprobadas el 2026-07-19 con la Fase 1.

- **Decisión:** `ADMIN` gestiona usuarios, productos, materiales y BOM y puede consultar todos los módulos; `OPERATOR` realiza inventario manual y transiciones de órdenes; `QUALITY_INSPECTOR` crea inspecciones. Consultar un módulo no concede permiso para mutarlo.
- **Motivo:** completar la asignación faltante de BOM sin convertir `ADMIN` en un superusuario operativo y mantener una separación defendible de responsabilidades.
- **Alternativas consideradas:** permitir a `ADMIN` todas las acciones; permitir a `OPERATOR` editar BOM; crear un cuarto rol de ingeniería.
- **Consecuencias:** las operaciones fuera de rol devuelven `403`; la UI adapta acciones por rol, pero el backend es la autoridad.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0008 — Reserva funcional de inventario

- **Decisión:** confirmar una orden reserva de forma atómica todo el material requerido; cancelar libera la reserva; completar convierte la reserva en consumo y movimiento de salida.
- **Motivo:** conservar el consumo físico al completar sin permitir que órdenes concurrentes comprometan el mismo stock.
- **Alternativas consideradas:** descontar al confirmar; revalidar solo al completar; permitir sobreasignación y resolverla manualmente.
- **Consecuencias:** se manejarán stock físico, reservado y disponible. La técnica concreta de bloqueo se elegirá en arquitectura/modelo y se probará con concurrencia.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0009 — Máquina de estados de órdenes

- **Decisión:** usar `DRAFT → CONFIRMED → IN_PROGRESS → COMPLETED`; permitir cancelar desde los tres primeros estados; `COMPLETED` y `CANCELLED` son terminales.
- **Motivo:** dar significado al estado `IN_PROGRESS` exigido y hacer explícitas las transiciones operativas.
- **Alternativas consideradas:** completar directamente desde `CONFIRMED`; permitir reabrir órdenes; cancelar órdenes completadas.
- **Consecuencias:** solo `DRAFT` es editable; toda transición inválida es conflicto `409` y no genera efectos laterales.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0010 — Una inspección inmutable por orden

- **Decisión:** una orden completada admite como máximo una inspección final `APPROVED` o `REJECTED`; no hay reinspección en el MVP.
- **Motivo:** cumplir la validación de inspección duplicada, preservar trazabilidad y mantener el alcance controlado.
- **Alternativas consideradas:** múltiples intentos; inspección editable; flujo `PENDING` con aprobación posterior.
- **Consecuencias:** un duplicado devuelve `409`; rechazo exige observaciones; una corrección requeriría una ampliación aprobada del alcance.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0011 — Inmutabilidad histórica y desactivación lógica

- **Decisión:** movimientos, inspecciones y estados terminales son inmutables; productos, materiales y usuarios se desactivan en vez de borrarse cuando la historia debe conservarse.
- **Motivo:** asegurar trazabilidad y evitar referencias rotas.
- **Alternativas consideradas:** borrado físico; edición de movimientos; cascadas de eliminación.
- **Consecuencias:** las correcciones de inventario usan movimientos compensatorios y las consultas históricas incluyen referencias inactivas.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0012 — Dashboard autorizado por rol

- **Decisión:** los tres roles acceden al dashboard, pero reciben métricas diferentes según su responsabilidad.
- **Motivo:** ofrecer valor operativo sin conceder acceso indirecto a datos fuera del rol.
- **Alternativas consideradas:** dashboard exclusivo de ADMIN; mismas métricas para todos; crear varios módulos de reportes.
- **Consecuencias:** backend y frontend deberán aplicar el alcance de métricas; los indicadores se derivan de fuentes existentes.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0013 — Snapshot de BOM al confirmar

- **Decisión:** una orden `DRAFT` usa la BOM vigente al confirmarse y conserva un snapshot de requerimientos desde ese momento.
- **Motivo:** impedir que cambios posteriores alteren reservas o trazabilidad de una orden comprometida.
- **Alternativas consideradas:** fijar BOM al crear el borrador; recalcular siempre con la BOM actual; implementar versionado completo de BOM en el MVP.
- **Consecuencias:** cambios de BOM solo afectan confirmaciones futuras; el modelo deberá conservar requerimientos por orden.
- **Fecha/fase:** 2026-07-19, Fase 1.

## D-0014 — Monolito modular con SPA y una base de datos

> Estado de D-0014 a D-0025: aprobadas el 2026-07-19 con la Fase 2.

- **Decisión:** desplegar un backend Spring Boot modular, una SPA React y una instancia MySQL como tres contenedores lógicos.
- **Motivo:** conservar transacciones locales, reducir operación y mantener límites explicables para un MVP.
- **Alternativas consideradas:** microservicios; aplicación monolítica sin módulos; server-side rendering.
- **Consecuencias:** un único backend escala y despliega como unidad; los límites internos deben protegerse por dependencias y no por red.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0015 — Organización feature-first con capas internas

- **Decisión:** organizar backend y frontend por funcionalidad, con capas API, aplicación, dominio e infraestructura según necesidad.
- **Motivo:** localizar requisitos y cambios por módulo sin acumular controladores, servicios o componentes globales.
- **Alternativas consideradas:** estructura global solo por capas; arquitectura hexagonal estricta para cada CRUD; carpetas planas.
- **Consecuencias:** cada módulo expone fachadas y evita ciclos; no se crean carpetas o interfaces vacías por simetría artificial.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0016 — Contratos REST versionados y separados de JPA

- **Decisión:** exponer JSON bajo `/api/v1` mediante request/response DTOs y mappers explícitos; no serializar entidades JPA.
- **Motivo:** desacoplar persistencia, seguridad y contrato público.
- **Alternativas consideradas:** exponer entidades; GraphQL; endpoints no versionados.
- **Consecuencias:** habrá mapeo explícito y DTOs adicionales; cambios internos no alteran automáticamente la API.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0017 — Flyway como autoridad del esquema

- **Decisión:** Flyway crea/versiona MySQL y Hibernate usa `ddl-auto=validate`; integración usa MySQL real, no H2.
- **Motivo:** migraciones reproducibles y pruebas fieles a SQL, constraints y locks de producción del MVP.
- **Alternativas consideradas:** `ddl-auto=update`; scripts manuales sin versionar; H2 en memoria.
- **Consecuencias:** toda modificación del esquema requiere migración; Testcontainers/Docker se vuelve requisito para integración.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0018 — Locks pesimistas ordenados para operaciones críticas

- **Decisión:** usar transacciones de aplicación, aislamiento propuesto `READ_COMMITTED`, lock de orden y `PESSIMISTIC_WRITE` sobre balances ordenados por material.
- **Motivo:** impedir sobreasignación, doble consumo y saldos negativos bajo concurrencia.
- **Alternativas consideradas:** solo optimistic locking; aislamiento serializable; revalidar al completar; mutex en memoria.
- **Consecuencias:** índices y orden de locks son obligatorios; deadlocks residuales deben revertirse y probarse contra MySQL.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0019 — JWT breve con versión de autorización

- **Decisión:** Spring Security Resource Server validará JWT HS256 de 15 minutos; cada petición comprobará usuario activo y `authVersion` actual. No habrá refresh token ni blacklist en el MVP.
- **Motivo:** invalidar inmediatamente cambios de rol/estado manteniendo un esquema Bearer simple.
- **Alternativas consideradas:** JWT completamente stateless hasta expirar; sesiones de servidor; refresh tokens y blacklist; firma asimétrica.
- **Consecuencias:** una consulta de usuario por petición; logout cliente no revoca individualmente un token robado; secreto fuerte y TLS son necesarios.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0020 — Sesión frontend en `sessionStorage`

- **Decisión:** React mantiene el JWT en memoria y `sessionStorage`; Axios lo envía como Bearer y lo elimina en logout/`401`.
- **Motivo:** sobrevivir recargas dentro de la pestaña sin persistencia indefinida y sin introducir cookies/CSRF en el MVP.
- **Alternativas consideradas:** `localStorage`; solo memoria; cookie HttpOnly con protección CSRF.
- **Consecuencias:** sigue existiendo riesgo XSS; se requieren CSP, dependencias controladas y prohibición de HTML no confiable.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0021 — Auditoría persistente separada de logs

- **Decisión:** guardar eventos append-only en MySQL; cambios exitosos en la transacción de negocio y rechazos críticos en una transacción separada corta. No hay purga automática en el MVP.
- **Motivo:** reconstruir acciones aunque los logs roten y preservar rechazo de autenticación/autorización tras rollback.
- **Alternativas consideradas:** solo logs; auditoría asíncrona con broker; triggers para todas las tablas.
- **Consecuencias:** debe sanitizarse payload y controlar crecimiento; no sustituye una política regulatoria productiva.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0022 — Pirámide de pruebas con MySQL Testcontainers

- **Decisión:** combinar unitarias JUnit/Mockito, integración Spring/MySQL Testcontainers, Newman, Cypress, SQL, Locust y smoke Docker.
- **Motivo:** asignar cada riesgo al nivel más barato que pueda demostrarlo sin confundir mocks con integración.
- **Alternativas consideradas:** E2E para todo; H2; solo Postman manual.
- **Consecuencias:** integración requiere Docker y un runner compatible; datos y migraciones deben ser deterministas.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0023 — Contenedores multi-stage y proxy de mismo origen

- **Decisión:** compilar backend y frontend en stages separados; ejecutar backend en JRE y SPA en Nginx, que proxya `/api`.
- **Motivo:** imágenes reproducibles, runtime reducido y menos problemas de CORS en Docker.
- **Alternativas consideradas:** Node sirviendo Vite en producción; una imagen monolítica; ejecutar todo directamente en host.
- **Consecuencias:** Nginx debe manejar fallback SPA, headers, health y proxy; se mantienen imágenes separadas.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0024 — Dependencias deliberadamente limitadas

- **Decisión:** no usar microservicios, Redis, broker, Redux, Lombok o MapStruct en el MVP; React Router, Springdoc y Testcontainers sí cubren necesidades concretas.
- **Motivo:** mantener lectura explícita, superficie de fallos pequeña y explicación defendible.
- **Alternativas consideradas:** incorporar herramientas por anticipación o conveniencia de boilerplate.
- **Consecuencias:** algunos mappers/modelos serán manuales; una nueva dependencia requerirá necesidad y decisión documentada.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0025 — Errores correlacionados en toda la frontera HTTP

- **Decisión:** propagar/generar `X-Correlation-Id` y usar un esquema común también desde handlers de Spring Security.
- **Motivo:** automatizar assertions y vincular respuesta, logs y auditoría.
- **Alternativas consideradas:** mensajes libres; correlación solo en errores `5xx`; formatos separados para seguridad.
- **Consecuencias:** filtros y handlers comparten un factory de errores; el contrato exacto se define en Fase 4.
- **Fecha/fase:** 2026-07-19, Fase 2.

## D-0026 — Convenciones relacionales y precisión numérica

> Estado de D-0026 a D-0037: aprobadas con la Fase 3 el 2026-07-19.

- **Decisión:** usar nombres SQL `snake_case`, PK `BIGINT`, cantidades `DECIMAL(19,6)`/`BigDecimal` y fechas `DATETIME(6)` tratadas como UTC.
- **Motivo:** evitar errores binarios, soportar cantidades industriales y mantener un mapeo JPA predecible.
- **Alternativas consideradas:** `DOUBLE`; cantidades enteras por unidad mínima; UUID para toda PK; `TIMESTAMP` sin política explícita.
- **Consecuencias:** la aplicación debe validar escala/multiplicación y configurar JVM, conexión y scripts en UTC.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0027 — Unicidad mediante columnas normalizadas generadas

- **Decisión:** username, SKU, código y número de orden tienen columnas almacenadas `UPPER(TRIM(valor))` con unique index.
- **Motivo:** reforzar en base la misma normalización usada por la aplicación y proteger carreras concurrentes.
- **Alternativas consideradas:** confiar en collation; guardar una columna normalizada escrita por Java; validar solo antes de insertar.
- **Consecuencias:** las columnas se mapean read-only en JPA y el soporte de generated columns debe comprobarse con Hibernate/MySQL.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0028 — BOM versionada con una única revisión vigente

- **Decisión:** reemplazar BOM crea una nueva fila `boms` y nuevas `bom_items`; la revisión anterior se marca superseded. Un unique sobre una columna generada impide dos vigentes.
- **Motivo:** conservar historia completa y permitir explicar órdenes confirmadas con revisiones anteriores.
- **Alternativas consideradas:** sobrescribir líneas; guardar solo un JSON de auditoría; versionar únicamente en la orden.
- **Consecuencias:** BOM/items crecen por revisión y no se actualizan; la no-vaciedad se controla en servicio porque no es un constraint simple.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0029 — Reserva detallada y agregado de balance

- **Decisión:** `production_order_materials.active_reserved_quantity` identifica reserva por orden/material; `inventory_balances.reserved_quantity` mantiene el agregado bloqueable.
- **Motivo:** consultar disponibilidad eficientemente y reconciliar exactamente qué órdenes comprometen el stock.
- **Alternativas consideradas:** solo agregado; tabla de reservas separada; calcular reservas con SUM en cada confirmación.
- **Consecuencias:** ambos valores deben modificarse en una transacción y reconciliarse mediante prueba/SQL; se acepta denormalización controlada.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0030 — Estado actual más historial secuencial

- **Decisión:** guardar el estado actual en `production_orders` y cada transición en `production_order_status_history` con secuencia única.
- **Motivo:** combinar consulta rápida con trazabilidad completa.
- **Alternativas consideradas:** derivar estado solo del historial; guardar únicamente timestamps por estado; event sourcing completo.
- **Consecuencias:** cabecera e historial deben actualizarse bajo el mismo lock/transacción y validarse contra divergencia.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0031 — Enumeraciones portables como texto restringido

- **Decisión:** persistir enums en `VARCHAR` con `CHECK`, incluido un conjunto pequeño de unidades sin conversiones automáticas.
- **Motivo:** nombres legibles, compatibilidad JPA y migraciones explícitas sin depender del `ENUM` propietario.
- **Alternativas consideradas:** ordinal numérico; tablas catálogo para cada enum; tipo MySQL `ENUM`.
- **Consecuencias:** agregar un valor requiere migración del `CHECK`; Java usa `EnumType.STRING`.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0032 — Inmutabilidad física para ledgers cerrados

- **Decisión:** triggers bloquean update/delete de BOM items, movimientos, historial de estados, inspecciones y auditoría.
- **Motivo:** reforzar trazabilidad aunque un repositorio se use incorrectamente.
- **Alternativas consideradas:** solo convenciones de servicio; usuario DB con permisos insert-only por tabla; event store separado.
- **Consecuencias:** tests limpian schemas efímeros o truncan al destruir el entorno; correcciones usan nuevos registros/operaciones compensatorias.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0033 — Integridad referencial restrictiva y borrado lógico

- **Decisión:** todas las FK usan `ON DELETE RESTRICT`; usuarios, productos y materiales se desactivan en lugar de borrarse.
- **Motivo:** evitar cascadas que destruyan historial.
- **Alternativas consideradas:** cascade delete; set null; borrado físico solo si parece no referenciado.
- **Consecuencias:** el almacenamiento histórico crece y una purga futura requeriría política/migración explícita.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0034 — Auditoría polimórfica sin FK de recurso

- **Decisión:** `audit_events` referencia opcionalmente al actor, guarda snapshots de identidad y representa recurso mediante `resource_type/resource_id`, sin FK polimórfica.
- **Motivo:** auditar recursos y rechazos diversos sin una tabla por evento ni relaciones imposibles de tipar.
- **Alternativas consideradas:** una tabla audit por módulo; solo logs; JSON completo del request.
- **Consecuencias:** resource ID se valida semánticamente y no referencialmente; summary usa lista permitida y no contiene secretos.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0035 — Hash con identificador de algoritmo

- **Decisión:** `password_hash` admite el prefijo `{bcrypt}` y la implementación futura usará un `DelegatingPasswordEncoder` configurado con BCrypt como algoritmo de escritura.
- **Motivo:** mantener BCrypt aprobado y permitir evolución segura sin reinterpretar hashes existentes.
- **Alternativas consideradas:** almacenar BCrypt sin prefijo; columna separada de algoritmo; texto claro para fixture.
- **Consecuencias:** el fixture puede usar un hash BCrypt de prueba; ninguna contraseña clara se persiste y Fase 8 debe comprobar el match real.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0036 — Fixture separado y prohibido en producción

- **Decisión:** datos demo viven en un script separado del esquema, con credenciales conocidas y una marca de carga masiva.
- **Motivo:** permitir validación determinista sin contaminar migraciones productivas.
- **Alternativas consideradas:** insertar demo en V1; crear todo manualmente; usar secretos reales.
- **Consecuencias:** perfiles/CI deciden explícitamente cargarlo; la mera existencia del fixture no demuestra login ni auditoría implementados.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0037 — Enforcement en capas para reglas cruzadas

- **Decisión:** usar constraints para reglas de fila/clave y servicios transaccionales para permisos, estado relacionado, agregados, BOM no vacía, orden completada e último ADMIN.
- **Motivo:** MySQL no puede expresar de forma segura todas las invariantes cruzadas mediante `CHECK` o FK.
- **Alternativas consideradas:** triggers complejos para toda regla; confiar solo en Java; stored procedures como única API.
- **Consecuencias:** las consultas V-001 a V-027 y pruebas de integración verifican que ambas capas permanezcan coherentes.
- **Fecha/fase:** 2026-07-19, Fase 3.

## D-0038 — Representación pública exacta para IDs y cantidades

> Estado de D-0038 a D-0047: aprobadas con la Fase 4 el 2026-07-19.

- **Decisión:** serializar IDs, versiones y cantidades como strings decimales; convertirlos en backend a `Long` y `BigDecimal`.
- **Motivo:** evitar pérdida de precisión en JavaScript para `BIGINT` y `DECIMAL(19,6)`.
- **Alternativas consideradas:** JSON number; UUID; cantidades en unidades enteras mínimas.
- **Consecuencias:** React, Axios, fixtures y tests deben tratar estos valores como strings y convertir solo para presentación/cálculo decimal controlado.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0039 — Convenciones uniformes de recursos HTTP

- **Decisión:** usar `/api/v1`, sustantivos plurales en `kebab-case`, campos `lowerCamelCase`, UTC ISO 8601 y `application/json`.
- **Motivo:** reducir ambigüedad entre backend, frontend y automatización.
- **Alternativas consideradas:** rutas por verbo; `snake_case` público; URLs sin versión.
- **Consecuencias:** toda nueva operación debe respetar estas convenciones o registrar una excepción explícita.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0040 — PATCH parcial con versión explícita

- **Decisión:** toda edición parcial usa `PATCH` y exige `version` en el body; una versión desactualizada responde `409 CONCURRENT_MODIFICATION`.
- **Motivo:** hacer observable el locking optimista sin introducir ETag en el MVP.
- **Alternativas consideradas:** `PUT` completo; `If-Match`/ETag; last-write-wins.
- **Consecuencias:** el cliente conserva la versión recibida y debe recargar/reconciliar ante conflicto.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0041 — Transiciones como acciones explícitas

- **Decisión:** confirmar, iniciar, completar y cancelar usan `POST` sobre subrutas de acción; no se acepta asignar `status` directamente.
- **Motivo:** cada transición ejecuta reglas y efectos transaccionales distintos.
- **Alternativas consideradas:** `PATCH {status}`; endpoint genérico `/transitions`; comandos RPC fuera de recursos.
- **Consecuencias:** las acciones no son idempotentes; ante timeout el cliente consulta estado antes de reintentar, y una repetición incompatible devuelve `409`.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0042 — Ausencia de DELETE y logout servidor

- **Decisión:** no exponer `DELETE`; catálogos se desactivan con `PATCH`. El logout elimina el JWT en React y no tiene endpoint porque no existe sesión/blacklist de servidor.
- **Motivo:** preservar trazabilidad y mantener coherencia con el JWT corto aprobado.
- **Alternativas consideradas:** borrado físico; endpoint logout sin efecto; blacklist por token.
- **Consecuencias:** un token copiado sigue válido hasta expirar salvo desactivación/cambio de `authVersion`; la UI debe explicarlo y limpiar `sessionStorage`.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0043 — Paginación base cero y orden controlado

- **Decisión:** todas las colecciones usan envelope `content/page`, `page=0`, `size=20`, máximo 100 y whitelist de un criterio `sort=field,direction`.
- **Motivo:** impedir respuestas ilimitadas y consultas/ordenamientos arbitrarios.
- **Alternativas consideradas:** arrays completos; cursor pagination; headers `Link` y totales separados.
- **Consecuencias:** el frontend comparte un modelo de paginación; cursores pueden evaluarse fuera del MVP si crece el volumen.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0044 — Error estable, sanitizado y correlacionado

- **Decisión:** toda frontera HTTP devuelve el mismo `ErrorResponse`; `code` en inglés es estable, `message` humano no controla lógica, y `X-Correlation-Id` siempre se propaga/genera.
- **Motivo:** permitir assertions robustas y diagnóstico sin filtrar detalles internos.
- **Alternativas consideradas:** mensajes libres; Problem Details RFC 9457; formatos distintos para Spring Security.
- **Consecuencias:** handlers de validación, seguridad y excepciones deben compartir factory; `500` nunca expone stack/SQL/token.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0045 — Autorización por rol y alcance observable

- **Decisión:** aplicar la matriz aprobada también en cada operación; inspector solo ve órdenes `COMPLETED`, `ADMIN` no ejecuta operaciones y producto para inspector se embebe como contexto.
- **Motivo:** evitar que permisos visuales sustituyan seguridad de servidor.
- **Alternativas consideradas:** `ADMIN` omnipotente; ocultar siempre recursos fuera de alcance con `404`; endpoints de catálogo para inspector.
- **Consecuencias:** rol incorrecto devuelve `403`; el filtro de órdenes del inspector es obligatorio y pedir otro estado devuelve `ORDER_SCOPE_FORBIDDEN`.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0046 — Requests estrictos y conflictos separados de validación

- **Decisión:** rechazar campos desconocidos y forma inválida con `400`; usar `404` para referencias ausentes y `409` para duplicados, estado, stock, concurrencia e invariantes relacionadas.
- **Motivo:** detectar divergencia contractual pronto y conservar semántica consistente con los requisitos.
- **Alternativas consideradas:** ignorar campos extra; devolver `422`; usar `400` para todos los errores de cliente.
- **Consecuencias:** cambios de contrato requieren coordinación; frontend/tests dependen de códigos estables, no del texto.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0047 — OpenAPI 3.1 como contrato preliminar verificable

- **Decisión:** mantener `docs/api/openapi.json` como contrato de diseño OpenAPI 3.1 y comprobar parseo, referencias, paridad de rutas y unicidad antes de implementar.
- **Motivo:** alinear backend, Swagger, frontend y automatización desde una fuente explícita.
- **Alternativas consideradas:** solo tablas Markdown; generar contrato únicamente desde anotaciones; OpenAPI YAML sin validación disponible.
- **Consecuencias:** Fases 7–13 deben comparar el documento servido por Springdoc con este contrato; la validación estática actual no demuestra una API ejecutable.
- **Fecha/fase:** 2026-07-19, Fase 4.

## D-0048 — Cobertura guiada por riesgo y capa mínima suficiente

> Estado de D-0048 a D-0057: aprobadas con la Fase 5 el 2026-07-19.

- **Decisión:** priorizar riesgos de integridad, seguridad y flujo; probar cada regla en la capa más rápida que pueda demostrarla sin imponer porcentajes artificiales de pirámide.
- **Motivo:** maximizar señal y diagnóstico, evitando repetir toda la combinatoria en Cypress.
- **Alternativas consideradas:** misma cobertura en todas las capas; E2E-first; cuotas fijas por número de tests.
- **Consecuencias:** unitarias cubren ramas puras, integración demuestra MySQL/transacciones, API el contrato y E2E solo journeys críticos.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0049 — Datos aislados sin endpoints productivos de test

- **Decisión:** usar `runId`, builders/fixtures controlados y schemas/contenedores efímeros; no añadir endpoints de reset/seed al artefacto productivo.
- **Motivo:** eliminar dependencia de orden y evitar una superficie peligrosa solo para automatización.
- **Alternativas consideradas:** base compartida; limpiar con DELETE; endpoint `/test/reset`; encadenar casos.
- **Consecuencias:** API/E2E necesitan bootstrap externo o API pública y destruyen el ambiente completo por suite cuando los ledgers impidan limpieza normal.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0050 — Cobertura JaCoCo enfocada en lógica

- **Decisión:** proponer gate de ≥80% líneas y ≥70% branches para paquetes `domain`/`application`, con exclusiones explícitas y revisión obligatoria de ramas críticas.
- **Motivo:** usar cobertura como detector de huecos sin premiar tests triviales de DTO/configuración.
- **Alternativas consideradas:** 100% global; sin umbral; solo line coverage.
- **Consecuencias:** una excepción debe documentar clase/línea/riesgo; cumplir porcentaje no sustituye tests de inventario, estados y calidad.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0051 — Integración únicamente con semántica MySQL real

- **Decisión:** usar Testcontainers MySQL para JPA, Flyway, constraints, triggers, locks y concurrencia; Mockito queda reservado para unitarias.
- **Motivo:** H2 no reproduce generated columns, JSON, locking ni comportamiento MySQL aprobado.
- **Alternativas consideradas:** H2 en memoria; mocks de repositorio para llamar integración; MySQL compartido permanente.
- **Consecuencias:** integración será más lenta que unitarias, pero aislada y con evidencia del motor real; concurrencia usa barreras/latches, no sleeps.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0052 — División explícita entre API y once E2E

- **Decisión:** Newman cubre las 39 operaciones, contratos y combinaciones negativas; Cypress se limita a los once journeys exigidos y usa backend real.
- **Motivo:** mantener E2E estable/rápido y concentrar la combinatoria donde el diagnóstico es mejor.
- **Alternativas consideradas:** automatizar todo en Cypress; solo API sin journeys; Cypress con todas las llamadas interceptadas.
- **Consecuencias:** un Cypress con API simulada se clasifica componente, no E2E; permisos tienen cobertura exhaustiva en API y representativa en navegador.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0053 — Evidencia ligada a ejecución y estados honestos

- **Decisión:** usar `NOT_RUN/PASS/FAIL/BLOCKED/SKIPPED`; evidencia incluye runId, UTC, commit/build, ambiente, caso, reporte y correlación sin secretos.
- **Motivo:** impedir afirmaciones de éxito sin ejecución reproducible.
- **Alternativas consideradas:** checkbox manual sin contexto; PASS parcial; screenshots como única evidencia.
- **Consecuencias:** ambiente roto produce BLOCKED; resultados de diseño/parseo nunca se presentan como prueba funcional.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0054 — Flaky test tratado como defecto

- **Decisión:** un retry sirve para diagnóstico, pero no cambia el primer fallo a éxito; tests flaky se registran, corrigen o aíslan visiblemente con responsable/plazo.
- **Motivo:** evitar pipelines verdes por repetición y pérdida de confianza.
- **Alternativas consideradas:** retries ilimitados; ignorar el caso; cuarentena silenciosa.
- **Consecuencias:** se medirá flaky rate y no se usarán esperas fijas como sincronización de concurrencia/E2E.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0055 — Severidad y prioridad independientes

- **Decisión:** clasificar impacto S1–S4 y urgencia P0–P3 por separado.
- **Motivo:** un impacto técnico alto puede quedar fuera del alcance inmediato y un fallo visual de demo puede requerir atención rápida sin ser severo.
- **Alternativas consideradas:** un único nivel High/Medium/Low; derivar siempre prioridad de severidad.
- **Consecuencias:** triage documenta ambos e incluye aceptación explícita de riesgos residuales.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0056 — Baseline de rendimiento contextualizado

- **Decisión:** proponer 20 usuarios, spawn 2/s, warm-up 1 min y medición 5 min; evaluar lecturas p95≤500 ms, escrituras p95≤1 s y error técnico <1% en ambiente dedicado.
- **Motivo:** convertir RNF-PER en escenario reproducible sin presentar capacidad local como SLA productivo.
- **Alternativas consideradas:** carga sin warm-up; runner CI compartido como benchmark; máximo throughput sin workload definido.
- **Consecuencias:** Fase 19 debe registrar hardware, versiones, commit, datos, percentiles y separar conflictos intencionales de errores técnicos.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0057 — Manual/exploratorio y defectos auténticos

- **Decisión:** conservar charters para usabilidad, accesibilidad, concurrencia UI y recuperación; documentar solo defectos observados, nunca introducir fallos para aparentar experiencia.
- **Motivo:** riesgos humanos/emergentes no tienen siempre un oracle automatizable y la evidencia del portafolio debe ser honesta.
- **Alternativas consideradas:** automatizar toda inspección; inventar cinco defectos; omitir exploración.
- **Consecuencias:** hallazgos siguen plantilla, retest y regresión; Fase 22 usará defectos reales acumulados o declarará honestamente su ausencia.
- **Fecha/fase:** 2026-07-19, Fase 5.

## D-0058 — Monorepo único con raíz aislada

> Estado de D-0058 a D-0066: aprobadas con la Fase 6 el 2026-07-19.

- **Decisión:** alojar backend, frontend, pruebas de sistema, Docker, CI y documentación en un único repositorio cuya raíz exacta será `D:\qaflow`.
- **Motivo:** conservar trazabilidad entre una funcionalidad, su contrato, pruebas, infraestructura y evidencia sin complejidad de versionado distribuido.
- **Alternativas consideradas:** repositorios separados por aplicación; inicializar Git en `D:\`; un repositorio solo para código y otro para QA.
- **Consecuencias:** cambios transversales pueden revisarse juntos; antes de inicializar Git se verifica la ruta y nunca se incorporan contenidos ajenos de `D:\`.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0059 — Estructura feature-first materializada bajo demanda

- **Decisión:** organizar backend y frontend por funcionalidad y crear únicamente carpetas que tengan contenido real; no usar `.gitkeep` o capas vacías por simetría.
- **Motivo:** hacer localizables los cambios de cada módulo sin generar una jerarquía ceremonial o un manifiesto ruidoso.
- **Alternativas consideradas:** paquetes globales por capa; árbol completo vacío desde Fase 6; carpetas planas.
- **Consecuencias:** cada feature usa solo las capas `api/application/domain/infrastructure` necesarias y el árbol crece con su fase propietaria.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0060 — Separación física de pruebas Java y pruebas de sistema

- **Decisión:** alojar unitarias, slices e integración Spring en `backend/src/test`; alojar Postman, Cypress, Locust, SQL externo, manuales y defectos en `tests/`.
- **Motivo:** cada suite debe vivir junto a su runtime y herramienta de construcción, manteniendo clara su clasificación.
- **Alternativas consideradas:** todas las pruebas en una carpeta raíz; Cypress dentro del frontend; integración Java fuera de Maven.
- **Consecuencias:** Maven controla pruebas Java; el harness Node de `tests/` controla Newman/Cypress; E2E no se confunde con pruebas de componente.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0061 — Autoridad única y destinos separados para SQL

- **Decisión:** usar `backend/src/main/resources/db/migration/V*.sql` como única autoridad ejecutable del esquema; separar fixtures Java, validaciones externas y SQL histórico en rutas distintas.
- **Motivo:** evitar que Docker, documentación y pruebas creen versiones divergentes del esquema.
- **Alternativas consideradas:** `docker-entrypoint-initdb.d`; ejecutar directamente el SQL documental; duplicar DDL en tests.
- **Consecuencias:** Fase 7 deriva y compara `V1__initial_schema.sql` con el diseño aprobado; Docker deja que Flyway migre y Fase 18 usa `tests/sql` solo para ambientes de prueba.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0062 — Dockerfiles junto a su contexto y orquestadores en raíz

- **Decisión:** ubicar cada Dockerfile en `backend/` o `frontend/`, configuración compartida en `docker/` y `docker-compose.yml`/`.gitlab-ci.yml` en la raíz.
- **Motivo:** mantener contextos de build pequeños y una única entrada de orquestación del monorepo.
- **Alternativas consideradas:** todos los Dockerfiles en `docker/`; Compose dentro del backend; una imagen única para SPA/API.
- **Consecuencias:** cada contexto requiere `.dockerignore`; Compose y CI deben declarar working directories explícitos.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0063 — Documentos de continuidad estables en la raíz

- **Decisión:** conservar sin renombrar los siete documentos de continuidad en la raíz y los entregables históricos bajo `docs/`.
- **Motivo:** mantener enlaces, historial y protocolo de recuperación de contexto consistentes entre fases.
- **Alternativas consideradas:** mover todo a `docs/project`; renombrar por fase; consolidar los siete documentos en uno.
- **Consecuencias:** la raíz contiene documentación de control además de orquestadores; `FILE_MANIFEST.md` distingue archivos existentes y planificados.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0064 — Dependencias reproducibles por cada runtime

- **Decisión:** versionar Maven Wrapper, ambos `package-lock.json` y dependencias Locust fijadas; ejecutar Maven/npm desde su módulo propietario.
- **Motivo:** reducir variaciones de herramienta en local y CI sin imponer un gestor de monorepo adicional.
- **Alternativas consideradas:** Maven/npm global sin wrapper/lock; un `package.json` raíz; workspaces Node para todo el repositorio.
- **Consecuencias:** existirán locks independientes en `frontend/` y `tests/`; CI usa `npm ci` y working directories explícitos.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0065 — Secretos y artefactos generados fuera de Git

- **Decisión:** ignorar secretos, dependencias, builds y reports generados; versionar solo configuración fuente y evidencia pequeña, seleccionada y sanitizada.
- **Motivo:** proteger credenciales y evitar que archivos voluminosos o efímeros oculten cambios reales.
- **Alternativas consideradas:** versionar reports completos; guardar `.env`; ignorar también locks/migraciones/configuración de pruebas.
- **Consecuencias:** CI publica artifacts con expiración; `.env.example` solo enumera variables; toda evidencia curada exige revisión de datos sensibles.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0066 — Inicialización Git posterior a la aprobación de estructura

- **Decisión:** no inicializar Git ni materializar el árbol durante la propuesta de Fase 6; después de aprobarla, crear primero archivos raíz de seguridad, ejecutar `git init` únicamente en `D:\qaflow` y verificar el toplevel antes de staging.
- **Motivo:** respetar la compuerta de aprobación y evitar capturar archivos fuera del proyecto.
- **Alternativas consideradas:** inicializar en esta fase; inicializar en `D:\`; realizar staging/commit automático.
- **Consecuencias:** la Fase 6 termina documentalmente; Fase 7 comienza con materialización controlada y ningún commit se crea sin solicitud explícita.
- **Fecha/fase:** 2026-07-19, Fase 6.

## D-0067 — Línea Spring Boot 3.5 compatible con Java 17

> Estado de D-0067 a D-0075: aprobadas con la Fase 7 el 2026-07-19.

- **Decisión:** fijar Spring Boot `3.5.13`, Java target/runtime 17, springdoc `2.8.17`, Maven Wrapper `3.9.11` y MySQL `8.0.42` para el baseline.
- **Motivo:** mantener la arquitectura Boot 3 ya aprobada, usar una combinación compatible con springdoc 2.8 y demostrar el requisito Java 17 en un runtime reproducible.
- **Alternativas consideradas:** Spring Boot 4.1; usar el JDK 21 del host como única evidencia; MySQL 8.4 con una versión Flyway que advirtió soporte no probado.
- **Consecuencias:** el host puede usar JDK 17 o Docker; cambios de versión requieren repetir `clean verify`, migración y smoke.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0068 — Perfiles externos sin secretos versionados

- **Decisión:** usar `application.yml` común y perfiles `local`, `test`, `prod`; contraseñas y URL productiva llegan por variables, Swagger queda deshabilitado por defecto en `prod`.
- **Motivo:** separar conveniencia local, aislamiento de integración y configuración productiva sin introducir credenciales en Git.
- **Alternativas consideradas:** un solo YAML; credenciales locales embebidas; `.env` versionado; Swagger siempre activo.
- **Consecuencias:** el perfil local falla temprano sin `DB_PASSWORD`; `.env.example` solo documenta nombres/valores ficticios.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0069 — Migración V1 idéntica al diseño aprobado

- **Decisión:** copiar sin cambios el SQL histórico aprobado a `V1__initial_schema.sql` y verificar igualdad SHA-256; Flyway migra y Hibernate usa `ddl-auto=validate`.
- **Motivo:** evitar divergencia silenciosa durante la transición de diseño a artefacto runtime.
- **Alternativas consideradas:** reescribir el esquema con JPA; editar V1 para eliminar warnings; usar init scripts Docker.
- **Consecuencias:** el hash común es `0981995F...B5E49C`; cambios futuros usan nuevas migraciones, no modifican V1 aplicada.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0070 — Correlación en filtro y errores sanitizados en frontera web

- **Decisión:** validar/generar `X-Correlation-Id` en un filtro de máxima precedencia y producir el mismo `ErrorResponse` para validación, recursos, conflictos e inesperados.
- **Motivo:** asegurar el encabezado incluso antes del controller y alinear respuesta, logs, auditoría y automatización.
- **Alternativas consideradas:** generar correlación solo en controllers; mensajes libres; exponer stack trace en desarrollo.
- **Consecuencias:** IDs inválidos devuelven `400`; un `500` registra solo tipo/correlación y nunca expone mensaje/stack al cliente.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0071 — Puerto de auditoría con persistencia append-only

- **Decisión:** exponer `AuditEventRecorder` y una implementación JPA que permite registrar en la transacción de negocio o en una transacción aislada `REQUIRES_NEW`.
- **Motivo:** conservar éxitos junto al cambio principal y rechazos/fallos aunque la transacción de negocio haga rollback.
- **Alternativas consideradas:** solo logs; guardar auditoría siempre en la misma transacción; exponer el repositorio Spring Data a todos los módulos.
- **Consecuencias:** módulos futuros dependen del puerto y no del repositorio; JSON y triggers de inmutabilidad se verifican contra MySQL.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0072 — Surefire y Failsafe separan tipos de prueba

- **Decisión:** ejecutar `*Test` mediante Surefire y `*IT` mediante Failsafe en `verify`; integración usa Testcontainers MySQL real.
- **Motivo:** impedir que una prueba con mocks se presente como integración y permitir gates independientes/diagnosticables.
- **Alternativas consideradas:** ejecutar todo como unit test; usar H2; requerir una base compartida permanente.
- **Consecuencias:** `clean test` no demuestra MySQL; el gate del backend es `clean verify` con Docker disponible.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0073 — Build Java 17 reproducible pese al host Java 21

- **Decisión:** usar Maven Wrapper y el runtime `maven:3.9.11-eclipse-temurin-17` para la evidencia de Fase 7; no instalar ni reemplazar el JDK del host.
- **Motivo:** el host tiene Java 21 y no Maven global, pero Docker permite verificar el requisito exacto sin mutar su toolchain.
- **Alternativas consideradas:** compilar solo con Java 21 `--release 17`; instalar Maven/JDK global; declarar Java 17 sin ejecutarlo.
- **Consecuencias:** Docker es prerrequisito para reproducir esta evidencia en este host; ejecución directa requiere JDK 17.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0074 — Política explícita para creación de triggers con binlog

- **Decisión:** configurar MySQL local/test con `--log-bin-trust-function-creators=1`; no conceder `SUPER` al usuario de aplicación.
- **Motivo:** MySQL rechazó Flyway con error 1419 al crear triggers bajo binlog; el privilegio amplio sería una corrección insegura.
- **Alternativas consideradas:** otorgar `SUPER`; eliminar triggers; desactivar binlog silenciosamente; ejecutar migraciones como root permanente.
- **Consecuencias:** despliegues deben usar una cuenta/política de migración revisada por DBA; este ajuste no pertenece al JDBC de la aplicación.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0075 — Seguridad funcional diferida a su fase propietaria

- **Decisión:** no añadir Spring Security/JWT ni endpoints de negocio en Fase 7; se materializan en Fase 8 y siguientes sobre la infraestructura verificada.
- **Motivo:** evitar credenciales autogeneradas/configuración temporal y respetar la compuerta que exige probar `401/403` en Fase 8.
- **Alternativas consideradas:** añadir Security Starter sin autenticación completa; crear un endpoint público de prueba en producción; implementar login anticipadamente.
- **Consecuencias:** health y Swagger son accesibles durante Fase 7; no debe afirmarse que existen autenticación, autorización o API funcional de dominio.
- **Fecha/fase:** 2026-07-19, Fase 7.

## D-0076 — Resource Server JWT HS256 con configuración estricta

> Estado de D-0076 a D-0082: aprobadas con la Fase 8 el 2026-07-19.

- **Decisión:** usar Spring Security OAuth2 Resource Server/Nimbus, HS256, secreto Base64 externo que decodifique al menos 32 bytes, `iss`/`aud` obligatorios y expiración default de 900 segundos.
- **Motivo:** aprovechar validación JOSE mantenida por Spring y cumplir la arquitectura sin implementar criptografía propia.
- **Alternativas consideradas:** filtro JWT artesanal; secreto corto por defecto; RSA/JWKS; sesiones de servidor.
- **Consecuencias:** la aplicación falla temprano sin secreto válido; rotación/JWKS quedan fuera del MVP; `JWT_SECRET_BASE64` nunca se versiona.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0077 — Revocación inmediata mediante usuario actual y authVersion

- **Decisión:** después de firma/claims, consultar `users` en cada petición y exigir usuario existente/activo, rol actual y `authVersion` idéntica.
- **Motivo:** hacer efectivos inmediatamente cambio de rol, contraseña o estado sin mantener blacklist de tokens.
- **Alternativas consideradas:** confiar solo en claims hasta `exp`; blacklist JTI; refresh tokens; caché distribuido.
- **Consecuencias:** el token sigue siendo JWT pero la autenticación no es stateless pura; existe una lectura breve adicional por request, aceptada para el MVP.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0078 — DelegatingPasswordEncoder con BCrypt y frontera sin secretos

- **Decisión:** almacenar hashes `{bcrypt}` mediante `DelegatingPasswordEncoder`, validar longitud 8–128 y usar DTOs write-only separados para password/newPassword.
- **Motivo:** evitar almacenamiento reversible, permitir identificar/migrar algoritmos y excluir credenciales de responses/auditoría.
- **Alternativas consideradas:** BCrypt directo sin prefijo; Argon2 con dependencia adicional; texto/ cifrado reversible; reutilizar entidad en API.
- **Consecuencias:** el costo BCrypt afecta deliberadamente login/tests; hashes y passwords no aparecen en JSON, JWT, logs ni summaries.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0079 — Último ADMIN protegido con locks y versión contractual

- **Decisión:** bloquear en orden los ADMIN activos antes de degradar/desactivar y comparar el `version` enviado antes de mutar; username es inmutable.
- **Motivo:** preservar al menos un ADMIN bajo concurrencia y evitar last-write-wins.
- **Alternativas consideradas:** solo contar sin lock; constraint imposible de expresar localmente; permitir auto-bloqueo; ETag en esta versión del contrato.
- **Consecuencias:** conflictos devuelven `LAST_ACTIVE_ADMIN` o `CONCURRENT_MODIFICATION`; mutaciones incrementan `version`, y las de seguridad también `authVersion`.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0080 — Bootstrap demo opt-in, idempotente y prohibido en prod

- **Decisión:** crear los tres roles solo con `QAFLOW_BOOTSTRAP_ENABLED=true`, credenciales externas, usernames idempotentes y un guard que aborta en perfil `prod`.
- **Motivo:** ofrecer datos iniciales demostrables sin introducir una contraseña demo activa por defecto o un endpoint secreto de reset.
- **Alternativas consideradas:** credenciales hardcoded; cargar siempre el fixture SQL; endpoint productivo de seed; crear solo ADMIN.
- **Consecuencias:** una demo requiere variables explícitas; el fixture histórico sigue siendo solo test/demo y el reset de suites futuras usa ambientes efímeros.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0081 — Auditoría de rechazo posterior a transacciones con locks

- **Decisión:** registrar rechazos de negocio mediante sincronización `afterCompletion` y luego `REQUIRES_NEW`; éxitos siguen en la transacción principal.
- **Motivo:** una auditoría aislada inmediata que referenciaba al actor compitió con el lock de `users` de la transacción exterior y produjo MySQL 1205.
- **Alternativas consideradas:** perder auditoría al hacer rollback; actor FK nulo; registrar todo en logs; mantener `REQUIRES_NEW` inmediato.
- **Consecuencias:** el `409` original no se oculta por auto-bloqueo; un fallo de persistencia de auditoría se registra técnicamente sin cambiar la respuesta de negocio.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0082 — Adaptación explícita del runner Testcontainers anidado

- **Decisión:** al ejecutar Maven dentro de Docker Desktop usar `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`; deshabilitar Ryuk solo en ese runner y verificar/limpiar contenedores al terminar.
- **Motivo:** callbacks y puertos publicados vía `172.17.0.1` no fueron accesibles desde el contenedor Maven aunque MySQL estaba saludable.
- **Alternativas consideradas:** presentar las integraciones como bloqueadas; usar H2; instalar JDK 17/Maven global; dejar contenedores residuales.
- **Consecuencias:** el comando de evidencia es más largo; un host JDK 17 directo no necesita este override; interrupciones requieren revisión explícita con `docker ps`.
- **Fecha/fase:** 2026-07-19, Fase 8.

## D-0083 — Catálogos con baja lógica y ocho operaciones exactas

> Estado de D-0083 a D-0088: aprobadas con la Fase 9 el 2026-07-19.

- **Decisión:** exponer para productos y materiales solo listar, crear, consultar y `PATCH`; la eliminación normal es `active=false` y no existe endpoint `DELETE`.
- **Motivo:** conservar referencias e historial y cumplir el contrato aprobado sin convertir el MVP en un catálogo con borrado físico peligroso.
- **Alternativas consideradas:** CRUD con `DELETE`; endpoint separado `/deactivate`; ocultar inactivos siempre.
- **Consecuencias:** los inactivos siguen consultables/filtrables; BOM y órdenes futuras deberán rechazar nuevos usos con `RESOURCE_INACTIVE`.
- **Fecha/fase:** 2026-07-19, Fase 9.

## D-0084 — PATCH sensible a presencia y null explícito

- **Decisión:** modelar los DTOs update con setters Jackson y banderas internas de presencia; `description: null` limpia el campo, omitirlo lo conserva y al menos un campo mutable es obligatorio.
- **Motivo:** un record con valores nullable no distingue omisión de null y contradeciría el contrato Fase 4.
- **Alternativas consideradas:** tratar null como omisión; JSON Patch; dependencia `JsonNullable`; reemplazo completo con `PUT`.
- **Consecuencias:** los DTOs son clases mutables solo en la frontera de deserialización; las entidades siguen controlando mutaciones y la validación cubre PATCH vacío.
- **Fecha/fase:** 2026-07-19, Fase 9.

## D-0085 — Balance inicial y seguridad semántica de unidad en la misma frontera

- **Decisión:** crear el balance material `0/0/0` mediante JDBC en la misma transacción del alta y consultar BOM items, movimientos y materiales de orden antes de cambiar unidad.
- **Motivo:** el balance ya pertenece al contrato de creación aunque el módulo operativo de inventario llegue en Fase 11; reinterpretar cantidades históricas sería incorrecto.
- **Alternativas consideradas:** diferir el balance a Fase 11; crearlo perezosamente; bloquear siempre la unidad por existir balance; conversiones automáticas.
- **Consecuencias:** un fallo de balance revierte el material; el balance cero por sí solo no inmoviliza la unidad; nuevas tablas con referencia material deberán incorporarse al predicado.
- **Fecha/fase:** 2026-07-19, Fase 9.

## D-0086 — Unicidad normalizada y optimistic locking con defensa en profundidad

- **Decisión:** prevalidar SKU/código normalizados para mensajes estables, conservar unique generated columns como autoridad de carrera y combinar versión contractual con JPA `@Version`.
- **Motivo:** ofrecer `DUPLICATE_*`/`CONCURRENT_MODIFICATION` claros sin confiar solo en checks no atómicos.
- **Alternativas consideradas:** validación solo en servicio; locks pesimistas de catálogo; last-write-wins; ETag en esta versión del contrato.
- **Consecuencias:** duplicados de carrera siguen siendo rechazados por MySQL; una integración con dos transacciones demostró un commit y un conflicto optimista.
- **Fecha/fase:** 2026-07-19, Fase 9.

## D-0087 — Paginación común y actores de auditoría resueltos por lote

- **Decisión:** mover `PageMetadataResponse` a `shared.api` y resolver `createdBy`/`updatedBy` únicos por página antes de mapear responses de catálogo.
- **Motivo:** evitar que productos/materiales dependan de un DTO del módulo usuario y evitar una consulta por actor por fila.
- **Alternativas consideradas:** duplicar PageMetadata; depender de `user.api`; asociaciones JPA eager; omitir actores del response.
- **Consecuencias:** user/product/material comparten forma de página; las FK garantizan actores existentes y el mapper falla temprano si el esquema está corrupto.
- **Fecha/fase:** 2026-07-19, Fase 9.

## D-0088 — Postman de fase acotado y Newman en su fase propietaria

- **Decisión:** crear una colección Fase 9 de 12 requests y environment sin secretos; validar su JSON ahora y reservar ejecución Newman, reports y colección consolidada para Fase 16.
- **Motivo:** cumplir el artefacto Postman solicitado por Fase 9 sin afirmar una herramienta/ejecución que pertenece explícitamente a una fase posterior.
- **Alternativas consideradas:** no crear Postman hasta F16; adelantar todo Newman; versionar contraseñas demo.
- **Consecuencias:** el Collection Runner puede usarse al completar credenciales locales; el estado actual es estructuralmente válido pero `NOT_RUN` en Newman.
- **Fecha/fase:** 2026-07-19, Fase 9.

## D-0089 — Revisiones BOM append-only con transición controlada de vigencia

> Estado de D-0089 a D-0093: aprobadas con la Fase 10 el 2026-07-20.

- **Decisión:** cada publicación crea una cabecera y líneas nuevas; la revisión anterior solo cambia de vigente a superseded y sus líneas no tienen operaciones de actualización o borrado.
- **Motivo:** conservar trazabilidad exacta y permitir que órdenes futuras referencien la revisión usada sin que una edición retroactiva altere requisitos.
- **Alternativas consideradas:** editar la BOM vigente en sitio; borrar/reinsertar líneas; guardar solo la versión actual; copiar JSON sin relaciones.
- **Consecuencias:** el historial crece de forma append-only; MySQL impide dos vigentes mediante columna generada unique y triggers rechazan `UPDATE/DELETE` de líneas.
- **Fecha/fase:** 2026-07-19, Fase 10.

## D-0090 — Serialización por producto y locks de materiales en orden estable

- **Decisión:** observar primero el id vigente, adquirir `PESSIMISTIC_WRITE` sobre el producto, volver a leer la vigente con lock y bloquear todos los materiales por id ascendente antes de publicar.
- **Motivo:** detectar una publicación concurrente, estabilizar estado activo de maestros y evitar que dos transacciones dejen dos revisiones vigentes o resultados parciales.
- **Alternativas consideradas:** solo confiar en el unique; lock global de BOM; versión enviada por cliente; isolation serializable para toda la aplicación.
- **Consecuencias:** publicaciones del mismo producto se serializan; si la vigente observada cambió se devuelve `409 BOM_CONCURRENTLY_CHANGED`; el orden común reduce deadlocks y el unique sigue como defensa final.
- **Fecha/fase:** 2026-07-19, Fase 10.

## D-0091 — Cantidad exacta pública como string y escala canónica seis

- **Decisión:** recibir cantidades BOM como strings con hasta 13 enteros/6 decimales, parsearlas con `BigDecimal` y persistir/responder siempre con escala seis.
- **Motivo:** preservar `DECIMAL(19,6)` sin introducir redondeo binario ni aceptar silenciosamente precisión fuera del contrato.
- **Alternativas consideradas:** JSON number/double; redondear exceso de escala; aceptar notación científica; value object exclusivo del módulo.
- **Consecuencias:** cero, negativos, notación científica y séptimo decimal devuelven `400`; `2.5` se representa como `2.500000`; el parser compartido puede reutilizarse en inventario y órdenes.
- **Fecha/fase:** 2026-07-19, Fase 10.

## D-0092 — Maestros activos al publicar e historia consultable después

- **Decisión:** exigir producto y materiales existentes/activos solo al crear una revisión; las revisiones ya creadas siguen visibles aunque un maestro se desactive posteriormente.
- **Motivo:** impedir nuevas configuraciones con datos fuera de uso sin destruir evidencia histórica.
- **Alternativas consideradas:** ocultar historia de inactivos; impedir toda desactivación con BOM; copiar todos los atributos como texto; reactivar automáticamente.
- **Consecuencias:** ausencia devuelve `404`, inactividad `409 RESOURCE_INACTIVE`; los responses históricos resuelven el nombre/código actual del maestro y no constituyen un snapshot completo de sus etiquetas.
- **Fecha/fase:** 2026-07-19, Fase 10.

## D-0093 — Carrera BOM determinista sobre HTTP/MySQL sin sleeps

- **Decisión:** en integración, usar un spy únicamente para sincronizar las dos lecturas no bloqueantes de la vigente con `CountDownLatch`; mantener controllers, transactions, repositories, locks y MySQL reales.
- **Motivo:** demostrar de forma repetible la condición de carrera aprobada sin depender de timing ni esperas fijas.
- **Alternativas consideradas:** ejecutar dos requests y confiar en azar; `Thread.sleep`; simular todo con Mockito; omitir la carrera hasta rendimiento.
- **Consecuencias:** la prueba acredita una respuesta `201`, una `409`, una sola fila vigente y una sola revisión total; no demuestra todavía throughput ni políticas óptimas de lock timeout.
- **Fecha/fase:** 2026-07-19, Fase 10.

## D-0094 — Saldo materializado más ledger inmutable de inventario

> Estado de D-0094 a D-0097: implementadas como propuestas de Fase 11; requieren gate final y aprobación de la fase.

- **Decisión:** conservar `inventory_balances` como estado actual y `inventory_movements` como ledger append-only; no exponer update/delete ni asignación arbitraria del saldo.
- **Motivo:** ofrecer consultas eficientes sin sacrificar trazabilidad ni permitir correcciones retroactivas.
- **Alternativas consideradas:** calcular siempre desde movimientos; editar el saldo directamente; permitir update/delete de movimientos; event sourcing completo.
- **Consecuencias:** cada escritura debe reconciliar saldo, fotografía posterior, movimiento y auditoría; los ajustes son nuevos movimientos compensatorios y los triggers protegen historia.
- **Fecha/fase:** 2026-07-20, Fase 11.

## D-0095 — Escritura de inventario con READ_COMMITTED y locks ordenados

- **Decisión:** bloquear primero el material y después su balance con `PESSIMISTIC_WRITE`, usando aislamiento `READ_COMMITTED`; Fase 12 deberá adquirir múltiples materiales por id ascendente.
- **Motivo:** serializar salidas concurrentes sobre el mismo stock y conservar el mismo orden global de locks iniciado en BOM.
- **Alternativas consideradas:** optimistic locking con retries; isolation serializable global; update condicional JDBC; lock solo del balance.
- **Consecuencias:** una salida concurrente observa el saldo confirmado anterior y como máximo una puede consumir stock insuficiente para ambas; timeout/deadlock se traduce a conflicto y rendimiento se medirá después.
- **Fecha/fase:** 2026-07-20, Fase 11.

## D-0096 — Frontera manual separada del consumo de producción

- **Decisión:** permitir por `POST /api/v1/inventory/movements` solo `ENTRY`, `MANUAL_OUT`, `ADJUSTMENT_IN` y `ADJUSTMENT_OUT`; `PRODUCTION_CONSUMPTION` será una operación interna de completar orden en Fase 12.
- **Motivo:** impedir que un cliente fabrique consumos de orden sin máquina de estados, reserva, snapshot ni idempotencia.
- **Alternativas consideradas:** aceptar cualquier tipo y validar campos; endpoint público separado; adelantar órdenes; omitir el tipo de producción del modelo.
- **Consecuencias:** el intento manual devuelve `400 VALIDATION_ERROR`; ADMIN solo lee, OPERATOR escribe y QUALITY_INSPECTOR no accede.
- **Fecha/fase:** 2026-07-20, Fase 11.

## D-0097 — Cantidades y filtros de inventario contractuales y estables

- **Decisión:** reutilizar `ExactQuantityParser`, responder cantidades con seis decimales como string, limitar sort de movimientos a `occurredAt` y ofrecer filtros paginados inclusivo/exclusivo `[from,to)`.
- **Motivo:** conservar precisión y hacer reproducibles las consultas de un ledger grande sin aceptar propiedades arbitrarias.
- **Alternativas consideradas:** números JSON; ordenamiento libre; historial completo sin paginación; fechas inclusivas en ambos extremos.
- **Consecuencias:** cantidades fuera de rango/escala dan `400`, overflow acumulado da `409`, y las páginas agregan `id` como desempate estable.
- **Fecha/fase:** 2026-07-20, Fase 11.
