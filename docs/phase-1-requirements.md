# Fase 1 — Requisitos funcionales

## 1. Nombre de la fase

Fase 1. Requisitos funcionales.

### Resumen del estado anterior

- La Fase 0 fue aprobada el 2026-07-19.
- El alcance sigue limitado a un MVP de manufactura y portafolio técnico, no a un ERP.
- No existe código de aplicación ni pruebas ejecutables.

### Decisiones vigentes

- Trabajo secuencial con aprobación por fase.
- Evidencia obligatoria antes de declarar compilaciones o pruebas exitosas.
- Trazabilidad transversal.
- Consumo de materiales al completar la orden.
- Identificadores técnicos en inglés y documentación de trabajo en español.

### Archivos existentes utilizados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-0-discovery.md`

## 2. Objetivo

Convertir el alcance aprobado en requisitos verificables, casos de uso, historias de usuario, criterios de aceptación, reglas de negocio, permisos y escenarios. Esta fase define **qué** debe hacer QAFlow; no define todavía entidades físicas, endpoints ni implementación.

## 3. Dependencias

- Fase 0 aprobada.
- Roles, flujo principal, tecnologías y exclusiones vigentes.
- Resolución funcional de permisos, reserva de stock, estados, inspecciones y dashboard.

## 4. Entradas requeridas

- Brief original del proyecto.
- Entregable `docs/phase-0-discovery.md`.
- Decisiones D-0001 a D-0006.
- Problemas OI-002, OI-003, OI-004, OI-006 y OI-009.

No se requieren nuevas entradas externas para preparar esta fase. Las decisiones técnicas de bloqueo, JWT, persistencia y formato físico de auditoría permanecen para Fases 2–3.

## 5. Entregables

- Requisitos funcionales numerados.
- Requisitos no funcionales medibles.
- Casos de uso.
- Historias de usuario.
- Criterios de aceptación.
- Reglas de negocio consolidadas.
- Matriz de permisos.
- Escenarios felices, negativos y borde.
- Glosario funcional.
- Trazabilidad inicial entre requisitos y pruebas futuras.

## 6. Decisiones tomadas

Las siguientes decisiones fueron aprobadas el 2026-07-19 y están vigentes:

1. `ADMIN` administra usuarios, productos, materiales y BOM; consulta todos los módulos, pero no ejecuta acciones operativas ni inspecciones.
2. `OPERATOR` registra movimientos manuales y administra el ciclo operativo de las órdenes.
3. `QUALITY_INSPECTOR` consulta órdenes completadas y registra inspecciones; no cambia órdenes ni inventario.
4. El stock se reserva al confirmar, se libera al cancelar y se consume al completar.
5. La transición completa es `DRAFT → CONFIRMED → IN_PROGRESS → COMPLETED`; `CANCELLED` es terminal.
6. Una orden completada admite una sola inspección inmutable en el MVP.
7. Los catálogos y documentos con referencias se desactivan lógicamente; no se borran físicamente por operación normal.
8. El dashboard es visible para los tres roles, pero cada rol recibe únicamente métricas autorizadas.
9. Una inspección rechaza o aprueba el resultado producido por una orden; no cambia el estado del producto maestro.

## 7. Diseño funcional

### 7.1 Glosario

| Término | Definición funcional |
|---|---|
| Stock físico (`on-hand`) | Cantidad registrada físicamente de un material. |
| Stock reservado | Parte del stock físico comprometida por órdenes confirmadas o en proceso. |
| Stock disponible | `stock físico - stock reservado`; nunca puede ser negativo. |
| BOM | Lista vigente de materiales y cantidades necesarias para producir una unidad de producto. |
| Requerimiento de orden | Cantidad de cada material calculada como cantidad de orden × cantidad de BOM. |
| Snapshot de BOM | Copia de los requerimientos usada al confirmar una orden, inmune a cambios posteriores de la BOM. |
| Movimiento de inventario | Registro inmutable que aumenta o disminuye el stock físico. |
| Reserva | Compromiso de stock que no reduce todavía el stock físico. |
| Inspección | Resultado final de calidad asociado a una orden completada. |
| Operación importante | Cambio de seguridad, maestro, inventario, orden o calidad que debe ser auditable. |

### 7.2 Requisitos funcionales

#### Autenticación y sesión

| ID | Requisito |
|---|---|
| RF-AUT-001 | El sistema permitirá iniciar sesión con identificador de usuario y contraseña válidos. |
| RF-AUT-002 | Un inicio válido devolverá un JWT y los datos mínimos del usuario, incluido su rol. |
| RF-AUT-003 | Credenciales incorrectas o una cuenta inactiva devolverán `401` con un mensaje genérico que no revele cuál dato falló. |
| RF-AUT-004 | Todo recurso no público exigirá un JWT válido, no expirado y correctamente firmado. |
| RF-AUT-005 | Un token ausente, inválido, mal formado o expirado devolverá `401` en el contrato JSON común. |
| RF-AUT-006 | Un usuario autenticado sin el permiso necesario recibirá `403` y la operación no producirá cambios. |
| RF-AUT-007 | El usuario autenticado podrá consultar su perfil y rol actuales. |
| RF-AUT-008 | El cierre de sesión del MVP eliminará el token del cliente; expiración, renovación y revocación se concretarán en arquitectura. |
| RF-AUT-009 | Los intentos de autenticación exitosos y fallidos serán auditables sin almacenar contraseñas ni tokens. |

#### Usuarios y roles

| ID | Requisito |
|---|---|
| RF-USR-001 | `ADMIN` podrá listar y buscar usuarios con paginación. |
| RF-USR-002 | `ADMIN` podrá consultar el detalle de un usuario existente. |
| RF-USR-003 | `ADMIN` podrá crear usuarios con un identificador único, credenciales iniciales, estado y uno de los tres roles admitidos. |
| RF-USR-004 | `ADMIN` podrá actualizar datos permitidos, rol y estado activo de un usuario. |
| RF-USR-005 | El sistema impedirá identificadores de usuario duplicados después de normalizar espacios y mayúsculas/minúsculas. |
| RF-USR-006 | Las contraseñas se recibirán únicamente para creación o cambio y nunca se devolverán ni se almacenarán de forma reversible. |
| RF-USR-007 | Una cuenta desactivada no podrá obtener nuevos tokens. |
| RF-USR-008 | El sistema impedirá desactivar o degradar al último `ADMIN` activo. |
| RF-USR-009 | La gestión de usuarios será auditable con actor, acción, usuario afectado, fecha y resultado. |

#### Productos

| ID | Requisito |
|---|---|
| RF-PRD-001 | `ADMIN` podrá crear un producto con SKU, nombre, descripción opcional y estado activo. |
| RF-PRD-002 | El SKU será obligatorio y único después de normalizar espacios y mayúsculas/minúsculas. |
| RF-PRD-003 | `ADMIN` podrá consultar, listar, filtrar y actualizar productos. |
| RF-PRD-004 | `ADMIN` podrá desactivar un producto sin eliminar su historial. |
| RF-PRD-005 | Un producto referenciado no se eliminará físicamente mediante operaciones normales. |
| RF-PRD-006 | Productos inactivos seguirán visibles en consultas históricas, pero no podrán usarse para nuevas órdenes. |
| RF-PRD-007 | Crear, actualizar y desactivar productos será auditable. |

#### Materiales

| ID | Requisito |
|---|---|
| RF-MAT-001 | `ADMIN` podrá crear un material con código, nombre, descripción opcional, unidad de medida y estado activo. |
| RF-MAT-002 | El código será obligatorio y único después de normalizar espacios y mayúsculas/minúsculas. |
| RF-MAT-003 | `ADMIN` podrá consultar, listar, filtrar y actualizar materiales. |
| RF-MAT-004 | `ADMIN` podrá desactivar un material sin eliminar historial, movimientos o referencias. |
| RF-MAT-005 | Un material inactivo no podrá agregarse a nuevas BOM ni recibir nuevos movimientos manuales. |
| RF-MAT-006 | La unidad de medida no podrá cambiar después de que el material tenga BOM o movimientos asociados. |
| RF-MAT-007 | El MVP no realizará conversiones automáticas de unidades. |
| RF-MAT-008 | Crear, actualizar y desactivar materiales será auditable. |

#### Lista de materiales (BOM)

| ID | Requisito |
|---|---|
| RF-BOM-001 | `ADMIN` podrá consultar la BOM vigente de un producto. |
| RF-BOM-002 | `ADMIN` podrá crear o reemplazar la BOM vigente de un producto activo. |
| RF-BOM-003 | Una BOM tendrá al menos una línea y cada cantidad requerida por unidad será mayor que cero. |
| RF-BOM-004 | Un material solo podrá aparecer una vez en la misma BOM. |
| RF-BOM-005 | Todas las líneas deberán referir materiales existentes y activos. |
| RF-BOM-006 | Solo existirá una BOM vigente por producto en el MVP. |
| RF-BOM-007 | Reemplazar una BOM no modificará requerimientos ni reservas de órdenes ya confirmadas. |
| RF-BOM-008 | Una orden en `DRAFT` usará la BOM vigente en el momento de confirmación, no una copia tomada al crear el borrador. |
| RF-BOM-009 | Crear o reemplazar una BOM será auditable y conservará información suficiente para explicar el cambio. |

#### Inventario

| ID | Requisito |
|---|---|
| RF-INV-001 | Los usuarios autorizados podrán consultar stock físico, reservado y disponible por material. |
| RF-INV-002 | `OPERATOR` podrá registrar una entrada manual con cantidad mayor que cero y referencia o motivo. |
| RF-INV-003 | `OPERATOR` podrá registrar una salida manual con cantidad mayor que cero solo si el stock disponible posterior no queda negativo. |
| RF-INV-004 | El sistema generará salidas de producción al completar órdenes; ningún usuario podrá crearlas manualmente. |
| RF-INV-005 | Cada movimiento será inmutable y contendrá tipo, cantidad positiva, dirección, material, actor, fecha y referencia. |
| RF-INV-006 | Una corrección se realizará mediante un movimiento compensatorio, nunca editando o eliminando el original. |
| RF-INV-007 | El stock físico, reservado y disponible nunca podrá quedar negativo. |
| RF-INV-008 | Las modificaciones de stock y sus movimientos se confirmarán de forma atómica. |
| RF-INV-009 | El historial podrá consultarse y filtrarse por material, tipo y rango temporal con paginación. |
| RF-INV-010 | Los movimientos manuales sobre materiales inexistentes o inactivos serán rechazados. |
| RF-INV-011 | Las reservas reducirán stock disponible, pero no stock físico. |
| RF-INV-012 | El inventario no expondrá una operación para asignar directamente un saldo arbitrario. |

#### Órdenes de producción

| ID | Requisito |
|---|---|
| RF-ORD-001 | `OPERATOR` podrá crear una orden `DRAFT` para un producto activo con BOM vigente y cantidad mayor que cero. |
| RF-ORD-002 | El sistema asignará a cada orden un identificador de negocio único e inmutable. |
| RF-ORD-003 | `OPERATOR` podrá modificar producto y cantidad únicamente mientras la orden esté en `DRAFT`. |
| RF-ORD-004 | Los usuarios autorizados podrán consultar órdenes según el alcance de su rol y filtrarlas por identificador, producto, estado y fecha. |
| RF-ORD-005 | Al confirmar una orden `DRAFT`, el sistema calculará los requerimientos con la BOM vigente. |
| RF-ORD-006 | La confirmación fallará con `409` si falta BOM válida, existe material inactivo o el stock disponible es insuficiente. |
| RF-ORD-007 | Una confirmación exitosa guardará el snapshot de requerimientos y reservará todo el stock necesario de forma atómica. |
| RF-ORD-008 | `OPERATOR` podrá iniciar una orden `CONFIRMED`, cambiándola a `IN_PROGRESS` sin alterar su reserva. |
| RF-ORD-009 | `OPERATOR` podrá completar únicamente una orden `IN_PROGRESS`. |
| RF-ORD-010 | Completar consumirá las reservas, reducirá stock físico y generará una salida por material en una única transacción. |
| RF-ORD-011 | Si falla cualquier consumo, la orden, reservas, saldos y movimientos permanecerán sin cambios parciales. |
| RF-ORD-012 | `OPERATOR` podrá cancelar una orden `DRAFT`, `CONFIRMED` o `IN_PROGRESS`; cancelar liberará cualquier reserva y no generará consumo. |
| RF-ORD-013 | `COMPLETED` y `CANCELLED` serán estados terminales e inmutables. |
| RF-ORD-014 | Una transición inválida o una repetición incompatible devolverá `409` sin duplicar efectos. |
| RF-ORD-015 | Cada creación y transición registrará estado anterior, estado nuevo, actor y fecha. |

#### Inspecciones de calidad

| ID | Requisito |
|---|---|
| RF-QLT-001 | `QUALITY_INSPECTOR` podrá consultar órdenes `COMPLETED`, incluidas las pendientes de inspección. |
| RF-QLT-002 | Solo una orden `COMPLETED` podrá recibir una inspección. |
| RF-QLT-003 | Existirá como máximo una inspección por orden en el MVP. |
| RF-QLT-004 | La inspección deberá indicar resultado `APPROVED` o `REJECTED`. |
| RF-QLT-005 | Un resultado `REJECTED` exigirá observaciones no vacías después de eliminar espacios exteriores. |
| RF-QLT-006 | Las observaciones serán opcionales para `APPROVED`. |
| RF-QLT-007 | Una inspección registrada será inmutable; la reinspección queda fuera del MVP. |
| RF-QLT-008 | El resultado aplica a la producción de esa orden y no activa ni desactiva el producto maestro. |
| RF-QLT-009 | Los usuarios autorizados podrán consultar inspecciones y filtrarlas por resultado, producto, inspector y fecha. |
| RF-QLT-010 | Crear una inspección será auditable con orden, resultado, inspector y fecha. |

#### Dashboard

| ID | Requisito |
|---|---|
| RF-DSH-001 | Todo usuario autenticado podrá consultar un dashboard ajustado a los permisos de su rol. |
| RF-DSH-002 | `ADMIN` verá conteos de productos/materiales activos, órdenes por estado, materiales sin stock disponible e inspecciones por resultado. |
| RF-DSH-003 | `OPERATOR` verá órdenes por estado, órdenes propias recientes y materiales sin stock disponible. |
| RF-DSH-004 | `QUALITY_INSPECTOR` verá órdenes completadas sin inspección e inspecciones recientes por resultado. |
| RF-DSH-005 | Las métricas se calcularán a partir de datos existentes; no introducirán una fuente de verdad paralela. |

#### Auditoría, consultas y errores

| ID | Requisito |
|---|---|
| RF-COM-001 | `ADMIN` podrá consultar eventos de auditoría con filtros y paginación. |
| RF-COM-002 | Un evento de auditoría será inmutable e incluirá actor cuando exista, acción, tipo e identificador del recurso, fecha y resultado. |
| RF-COM-003 | Las listas devolverán resultados paginados y una colección vacía válida cuando no existan coincidencias. |
| RF-COM-004 | Datos sintáctica o semánticamente inválidos devolverán `400` con detalle por campo cuando aplique. |
| RF-COM-005 | Un recurso inexistente devolverá `404`. |
| RF-COM-006 | Duplicados, estados inválidos, stock insuficiente y otros conflictos de negocio devolverán `409`. |
| RF-COM-007 | Errores inesperados devolverán JSON consistente sin stack traces ni información sensible. |
| RF-COM-008 | Las respuestas de error incluirán al menos fecha, estado HTTP, código estable, mensaje, ruta y un identificador de correlación. |
| RF-COM-009 | Fechas persistidas e intercambiadas por API usarán UTC; la interfaz podrá presentarlas en la zona del usuario. |

### 7.3 Requisitos no funcionales

| ID | Categoría | Requisito verificable |
|---|---|---|
| RNF-SEC-001 | Seguridad | Las contraseñas se almacenarán con hash adaptativo y salt; nunca en texto claro ni cifrado reversible. |
| RNF-SEC-002 | Seguridad | Secretos, claves JWT y credenciales reales se suministrarán mediante variables y no se versionarán. |
| RNF-SEC-003 | Seguridad | El backend aplicará permisos independientemente de que la UI oculte acciones. |
| RNF-SEC-004 | Seguridad | Errores y logs no expondrán contraseñas, tokens, hashes, secretos ni stack traces al cliente. |
| RNF-SEC-005 | Seguridad | La expiración del JWT será configurable por ambiente; su política exacta se decidirá en Fase 2. |
| RNF-DAT-001 | Integridad | Operaciones de reserva, consumo, liberación y movimiento usarán límites transaccionales que eviten cambios parciales. |
| RNF-DAT-002 | Integridad | Restricciones de unicidad e integridad se harán cumplir también en base de datos, no solo en UI. |
| RNF-DAT-003 | Integridad | La concurrencia no permitirá stock físico, reservado ni disponible negativo. |
| RNF-PER-001 | Rendimiento | Objetivo inicial local: lecturas comunes con p95 ≤ 500 ms y escrituras comunes con p95 ≤ 1 s bajo 20 usuarios concurrentes, excluyendo arranque; se validará y contextualizará en Fase 19. |
| RNF-PER-002 | Rendimiento | Toda consulta de colección potencialmente creciente será paginada. |
| RNF-REL-001 | Fiabilidad | Repetir una transición finalizada no generará movimientos, reservas o auditorías de éxito duplicados. |
| RNF-REL-002 | Fiabilidad | Los servicios expondrán healthchecks útiles para orquestación y diagnóstico. |
| RNF-USA-001 | Usabilidad | Formularios mostrarán validación comprensible y conservarán los datos no sensibles cuando una petición falle. |
| RNF-USA-002 | Usabilidad | Acciones destructivas o terminales pedirán confirmación explícita en la UI. |
| RNF-ACC-001 | Accesibilidad | La UI usará HTML semántico, navegación por teclado, etiquetas asociadas y contraste orientado a WCAG 2.1 AA. |
| RNF-COM-001 | Compatibilidad | La aplicación web soportará una versión estable reciente de Chrome/Chromium usada por Cypress. |
| RNF-POR-001 | Portabilidad | La solución deberá ejecutarse en Windows para desarrollo y en contenedores Linux mediante Docker Compose. |
| RNF-MAN-001 | Mantenibilidad | Backend y frontend separarán responsabilidades y evitarán lógica de negocio duplicada en controladores o componentes visuales. |
| RNF-TST-001 | Testabilidad | Elementos críticos del frontend tendrán selectores `data-cy` estables y la API permitirá preparar datos controlados mediante operaciones autorizadas. |
| RNF-TST-002 | Testabilidad | Cada prueba será independiente, repetible y clasificará correctamente su nivel. |
| RNF-OBS-001 | Observabilidad | Las peticiones tendrán identificador de correlación propagado a errores y logs. |
| RNF-OBS-002 | Observabilidad | Los logs serán estructurados suficientemente para identificar fecha, nivel, correlación y evento sin registrar secretos. |
| RNF-DOC-001 | Documentación | Instalación, configuración, ejecución y pruebas deberán poder seguirse sin conocimiento implícito. |
| RNF-CIC-001 | CI/CD | Los jobs producirán reportes y artefactos; una etapa obligatoria fallida impedirá empaquetar como exitoso. |
| RNF-QUA-001 | Calidad | No habrá métodos vacíos, pseudocódigo de producción ni `TODO` usado como solución final. |

### 7.4 Casos de uso

| ID | Actor principal | Precondición | Flujo principal resumido | Alternativas/errores | Resultado |
|---|---|---|---|---|---|
| CU-01 Iniciar sesión | Usuario activo | Credenciales registradas | Envía credenciales; sistema valida; entrega JWT y rol. | Credenciales inválidas/inactivo: `401`. | Sesión autenticada. |
| CU-02 Gestionar usuarios | ADMIN | Sesión ADMIN | Lista, crea o actualiza usuario/rol/estado. | Duplicado, último admin o datos inválidos. | Usuario persistido y auditado. |
| CU-03 Gestionar productos | ADMIN | Sesión ADMIN | Crea, consulta, actualiza o desactiva producto. | SKU duplicado, inexistente o inválido. | Catálogo actualizado y auditado. |
| CU-04 Gestionar materiales | ADMIN | Sesión ADMIN | Crea, consulta, actualiza o desactiva material. | Código duplicado; cambio de unidad bloqueado. | Catálogo actualizado y auditado. |
| CU-05 Definir BOM | ADMIN | Producto/materiales activos | Envía una lista no vacía; sistema valida y reemplaza BOM vigente. | Línea duplicada, cantidad inválida o referencia inválida. | BOM vigente y auditada. |
| CU-06 Registrar entrada | OPERATOR | Material activo | Indica cantidad y referencia; sistema incrementa stock y crea movimiento. | Cantidad inválida o material no disponible. | Stock incrementado atómicamente. |
| CU-07 Registrar salida manual | OPERATOR | Stock disponible | Indica cantidad y motivo; sistema valida disponibilidad y crea salida. | Saldo disponible insuficiente: `409`. | Stock reducido sin afectar reservas. |
| CU-08 Crear/editar orden | OPERATOR | Producto activo con BOM | Crea borrador; opcionalmente cambia producto/cantidad. | Producto/BOM inválidos o estado no editable. | Orden `DRAFT`. |
| CU-09 Confirmar orden | OPERATOR | Orden `DRAFT` | Recalcula BOM; valida disponibilidad; guarda snapshot y reserva. | Stock/BOM/material no válido: `409`. | Orden `CONFIRMED`. |
| CU-10 Iniciar orden | OPERATOR | Orden `CONFIRMED` | Solicita inicio; sistema valida transición. | Estado distinto: `409`. | Orden `IN_PROGRESS`. |
| CU-11 Completar orden | OPERATOR | Orden `IN_PROGRESS` y reserva íntegra | Convierte reservas en consumo y movimientos; cambia estado. | Fallo de integridad revierte toda la operación. | Orden `COMPLETED`, stock consumido. |
| CU-12 Cancelar orden | OPERATOR | Orden no terminal | Cancela y libera reservas si existen. | Orden terminal: `409`. | Orden `CANCELLED`, sin consumo. |
| CU-13 Registrar inspección | QUALITY_INSPECTOR | Orden completada sin inspección | Selecciona resultado y observación; sistema valida y registra. | Duplicada, orden no completada o rechazo sin observación. | Inspección inmutable y auditada. |
| CU-14 Consultar dashboard | Usuario autenticado | JWT válido | Solicita métricas; sistema calcula solo las autorizadas. | Token inválido o rol sin dato específico. | Resumen por rol. |
| CU-15 Consultar auditoría | ADMIN | Sesión ADMIN | Filtra y pagina eventos. | Otro rol: `403`. | Trazabilidad consultable. |

### 7.5 Historias de usuario y criterios de aceptación

#### HU-01 — Inicio de sesión

Como usuario activo, quiero iniciar sesión para acceder únicamente a las funciones de mi rol.

- **CA-01.1:** Dadas credenciales válidas, cuando se envían, entonces se obtiene JWT, perfil mínimo y rol.
- **CA-01.2:** Dadas credenciales inválidas o cuenta inactiva, entonces se recibe `401` genérico y no se entrega token.
- **CA-01.3:** Dado un token expirado o alterado, al acceder a un recurso protegido se recibe `401`.

#### HU-02 — Administración de usuarios

Como `ADMIN`, quiero gestionar usuarios y roles para controlar el acceso.

- **CA-02.1:** Un `ADMIN` puede crear y actualizar usuarios con uno de los tres roles válidos.
- **CA-02.2:** Un identificador duplicado produce `409`.
- **CA-02.3:** No se puede desactivar o degradar al último administrador activo.
- **CA-02.4:** Un usuario no administrador recibe `403` en cualquier mutación de usuarios.

#### HU-03 — Productos únicos

Como `ADMIN`, quiero mantener productos para utilizarlos en BOM y órdenes.

- **CA-03.1:** Un SKU nuevo y datos válidos crean el producto.
- **CA-03.2:** El mismo SKU con diferencias de espacios o mayúsculas produce `409`.
- **CA-03.3:** Un producto desactivado conserva su historia y no admite nuevas órdenes.

#### HU-04 — Materiales únicos

Como `ADMIN`, quiero mantener materiales con unidad consistente para controlar inventario.

- **CA-04.1:** Un código nuevo y una unidad válida crean el material.
- **CA-04.2:** Un código normalizado duplicado produce `409`.
- **CA-04.3:** La unidad no se puede cambiar cuando existen referencias.

#### HU-05 — BOM válida

Como `ADMIN`, quiero definir materiales por producto para calcular requerimientos de producción.

- **CA-05.1:** Una lista no vacía con materiales activos únicos y cantidades positivas se guarda como BOM vigente.
- **CA-05.2:** Una BOM vacía, con duplicados o cantidades no positivas recibe `400`.
- **CA-05.3:** Una referencia inexistente recibe `404`; una referencia inactiva produce `409`.
- **CA-05.4:** Cambiar la BOM no altera órdenes ya confirmadas.

#### HU-06 — Entrada de inventario

Como `OPERATOR`, quiero registrar entradas para disponer de materiales.

- **CA-06.1:** Una cantidad positiva incrementa exactamente el stock físico y crea un movimiento inmutable.
- **CA-06.2:** Cero, negativos o formato inválido reciben `400`.
- **CA-06.3:** Si la operación falla no queda saldo sin movimiento ni movimiento sin saldo.

#### HU-07 — Salida manual segura

Como `OPERATOR`, quiero registrar salidas justificadas sin utilizar stock reservado.

- **CA-07.1:** Una salida menor o igual al disponible reduce stock físico y disponible.
- **CA-07.2:** Una salida mayor al disponible recibe `409` sin cambios.
- **CA-07.3:** El movimiento original no puede editarse ni borrarse.

#### HU-08 — Confirmación con reserva

Como `OPERATOR`, quiero confirmar una orden solo con stock suficiente para asegurar su ejecución.

- **CA-08.1:** Con stock disponible exacto o superior se guarda el snapshot, se reserva y la orden queda `CONFIRMED`.
- **CA-08.2:** Con cualquier material insuficiente se recibe `409` y no queda reserva parcial.
- **CA-08.3:** Dos confirmaciones concurrentes no pueden reservar la misma unidad de stock.

#### HU-09 — Ejecución y finalización

Como `OPERATOR`, quiero iniciar y completar una orden para registrar el consumo real.

- **CA-09.1:** Solo `CONFIRMED` cambia a `IN_PROGRESS`.
- **CA-09.2:** Solo `IN_PROGRESS` cambia a `COMPLETED`.
- **CA-09.3:** Completar reduce stock físico, libera reserva y crea una salida por material atómicamente.
- **CA-09.4:** Repetir la finalización no duplica consumos.

#### HU-10 — Cancelación

Como `OPERATOR`, quiero cancelar una orden no terminal para liberar recursos sin consumo.

- **CA-10.1:** Cancelar `DRAFT`, `CONFIRMED` o `IN_PROGRESS` produce `CANCELLED`.
- **CA-10.2:** Las reservas se liberan y el stock físico no cambia.
- **CA-10.3:** Una orden cancelada no puede iniciarse ni completarse.

#### HU-11 — Inspección aprobada

Como `QUALITY_INSPECTOR`, quiero aprobar la producción de una orden completada.

- **CA-11.1:** Una orden `COMPLETED` sin inspección acepta resultado `APPROVED`.
- **CA-11.2:** La observación puede omitirse.
- **CA-11.3:** El producto maestro no cambia de estado.

#### HU-12 — Inspección rechazada

Como `QUALITY_INSPECTOR`, quiero rechazar una producción explicando la causa.

- **CA-12.1:** `REJECTED` con observación no vacía crea una inspección.
- **CA-12.2:** `REJECTED` sin observación o solo con espacios recibe `400`.
- **CA-12.3:** Una segunda inspección sobre la misma orden recibe `409`.

#### HU-13 — Dashboard por rol

Como usuario autenticado, quiero ver indicadores relevantes sin acceder a datos fuera de mis permisos.

- **CA-13.1:** Cada rol recibe únicamente el conjunto definido en RF-DSH-002 a RF-DSH-004.
- **CA-13.2:** Los conteos corresponden a los datos persistidos al momento de consultar.
- **CA-13.3:** Un usuario no autenticado recibe `401`.

#### HU-14 — Auditoría consultable

Como `ADMIN`, quiero consultar eventos importantes para explicar quién hizo qué y cuándo.

- **CA-14.1:** Crear/cambiar usuarios, maestros, BOM, inventario, órdenes e inspecciones genera eventos.
- **CA-14.2:** Los eventos no pueden editarse ni eliminarse mediante la API funcional.
- **CA-14.3:** `OPERATOR` y `QUALITY_INSPECTOR` reciben `403` al consultar el registro global.

#### HU-15 — Errores consistentes

Como consumidor de la API, quiero errores consistentes para diagnosticar y automatizar validaciones.

- **CA-15.1:** `400`, `401`, `403`, `404`, `409` y errores inesperados siguen el mismo esquema base.
- **CA-15.2:** Validaciones por campo se incluyen cuando corresponda.
- **CA-15.3:** Ninguna respuesta expone stack trace o datos sensibles.

### 7.6 Reglas de negocio

| ID | Regla |
|---|---|
| RN-001 | No se permite un SKU duplicado después de normalización. |
| RN-002 | No se permite un código de material duplicado después de normalización. |
| RN-003 | Ninguna cantidad de negocio puede ser negativa. |
| RN-004 | Cantidades de movimientos, BOM y órdenes deben ser estrictamente mayores que cero. |
| RN-005 | Stock físico, reservado y disponible nunca pueden quedar negativos. |
| RN-006 | Una BOM debe tener al menos un material. |
| RN-007 | Un material no se repite dentro de la misma BOM. |
| RN-008 | No se confirma una orden sin BOM válida y stock disponible suficiente para todos sus materiales. |
| RN-009 | Al confirmar se reserva todo o no se reserva nada. |
| RN-010 | `disponible = físico - reservado` y `físico ≥ reservado ≥ 0`. |
| RN-011 | Una orden cancelada no puede iniciarse ni completarse. |
| RN-012 | Una orden completada no puede modificarse, cancelarse ni completarse otra vez. |
| RN-013 | Solo se completa una orden `IN_PROGRESS`. |
| RN-014 | Solo se inspeccionan órdenes completadas. |
| RN-015 | Una orden admite como máximo una inspección en el MVP. |
| RN-016 | Una inspección rechazada requiere observaciones no vacías. |
| RN-017 | Un usuario autenticado sin permisos recibe `403`; la UI no sustituye esta validación. |
| RN-018 | Datos inválidos reciben `400`. |
| RN-019 | Recursos inexistentes reciben `404`. |
| RN-020 | Duplicados, stock insuficiente y transiciones inválidas reciben `409`. |
| RN-021 | Errores inesperados devuelven JSON consistente y no filtran detalles internos. |
| RN-022 | Toda operación importante queda registrada en auditoría. |
| RN-023 | Un JWT ausente o inválido recibe `401` en recursos protegidos. |
| RN-024 | El snapshot de una orden confirmada no cambia cuando cambia la BOM. |
| RN-025 | Movimientos e inspecciones son inmutables; sus correcciones requieren operaciones explícitas permitidas por el alcance. |
| RN-026 | Cancelar una orden libera reservas y no descuenta stock físico. |
| RN-027 | Completar convierte la reserva en consumo dentro de una sola transacción. |
| RN-028 | Fallar una operación transaccional no deja efectos parciales. |
| RN-029 | Catálogos referenciados se desactivan lógicamente para conservar trazabilidad. |
| RN-030 | Productos o materiales inactivos no pueden iniciar nuevas operaciones que los utilicen. |
| RN-031 | La unidad de un material referenciado es inmutable y no se realizan conversiones automáticas. |
| RN-032 | Stock disponible exactamente igual al requerido es suficiente. |
| RN-033 | Repetir una acción terminal incompatible no duplica efectos y devuelve `409`. |
| RN-034 | El último `ADMIN` activo no puede desactivarse ni perder ese rol. |
| RN-035 | El resultado de inspección afecta la producción de la orden, no el estado del producto maestro. |
| RN-036 | Solo `OPERATOR` genera movimientos manuales y transiciones operativas; el consumo de producción lo genera el sistema. |

### 7.7 Matriz de permisos

Leyenda: `Sí` permitido; `Solo alcance` permitido con filtro indicado; `No` prohibido y debe responder `403` si se intenta con autenticación válida.

| Recurso / acción | ADMIN | OPERATOR | QUALITY_INSPECTOR |
|---|---:|---:|---:|
| Iniciar/cerrar sesión y consultar perfil propio | Sí | Sí | Sí |
| Listar/consultar usuarios | Sí | No | No |
| Crear/actualizar/activar/desactivar usuarios | Sí | No | No |
| Listar/consultar productos | Sí | Sí | Solo contexto de órdenes completadas/inspecciones |
| Crear/actualizar/desactivar productos | Sí | No | No |
| Listar/consultar materiales | Sí | Sí | No |
| Crear/actualizar/desactivar materiales | Sí | No | No |
| Consultar BOM | Sí | Sí | No |
| Crear/reemplazar BOM | Sí | No | No |
| Consultar saldos e historial de inventario | Sí | Sí | No |
| Registrar entrada/salida manual | No | Sí | No |
| Crear consumo de producción manualmente | No | No | No |
| Consultar órdenes | Sí, todas | Sí, todas las operativas | Solo `COMPLETED` |
| Crear/modificar orden `DRAFT` | No | Sí | No |
| Confirmar/iniciar/completar/cancelar orden | No | Sí | No |
| Consultar inspecciones | Sí, todas | No | Sí, todas |
| Crear inspección | No | No | Sí |
| Modificar/eliminar inspección | No | No | No |
| Consultar dashboard | Sí, vista ADMIN | Sí, vista OPERATOR | Sí, vista QUALITY_INSPECTOR |
| Consultar auditoría global | Sí | No | No |

### 7.8 Escenarios felices

| ID | Escenario | Resultado esperado |
|---|---|---|
| EF-001 | Usuario activo inicia sesión con credenciales válidas. | Recibe JWT y rol correctos. |
| EF-002 | ADMIN crea material y producto con identificadores nuevos. | Ambos quedan activos y auditados. |
| EF-003 | ADMIN define BOM con dos materiales activos y cantidades positivas. | BOM queda vigente. |
| EF-004 | OPERATOR registra entradas de ambos materiales. | Saldos y movimientos coinciden. |
| EF-005 | OPERATOR crea y confirma una orden cuyo requerimiento iguala el disponible. | Se reserva todo y queda `CONFIRMED`. |
| EF-006 | OPERATOR inicia y completa la orden. | Queda `COMPLETED`; reservas en cero y consumos registrados. |
| EF-007 | QUALITY_INSPECTOR aprueba la orden completada. | Se crea una inspección inmutable `APPROVED`. |
| EF-008 | QUALITY_INSPECTOR rechaza otra orden con observación. | Se crea una inspección `REJECTED` con texto. |
| EF-009 | OPERATOR cancela una orden confirmada. | Reserva liberada; stock físico intacto. |
| EF-010 | Cada rol consulta su dashboard. | Obtiene solo sus métricas autorizadas. |
| EF-011 | ADMIN consulta auditoría del flujo. | Puede reconstruir actores, fechas y transiciones. |

### 7.9 Escenarios negativos

| ID | Escenario | Resultado esperado |
|---|---|---|
| EN-001 | Login con contraseña incorrecta o cuenta inactiva. | `401`, sin token y mensaje genérico. |
| EN-002 | Petición protegida sin token o con token alterado. | `401`, sin cambios. |
| EN-003 | OPERATOR intenta crear un producto. | `403`, sin cambios. |
| EN-004 | ADMIN intenta registrar una entrada de inventario. | `403`, sin cambios. |
| EN-005 | Crear SKU o código material duplicado. | `409`, conserva registro original. |
| EN-006 | Crear movimiento, BOM u orden con cero/negativo. | `400`. |
| EN-007 | Guardar BOM vacía o con material repetido. | `400`. |
| EN-008 | Referir producto/material inexistente. | `404`. |
| EN-009 | Usar producto/material inactivo en una operación nueva. | `409`. |
| EN-010 | Salida manual mayor al stock disponible. | `409`, sin saldo ni movimiento parcial. |
| EN-011 | Confirmar una orden sin stock suficiente. | `409`, sin reservas parciales. |
| EN-012 | Modificar una orden confirmada o completada. | `409`. |
| EN-013 | Completar una orden `DRAFT`, `CONFIRMED` o `CANCELLED`. | `409`, sin consumo. |
| EN-014 | Cancelar una orden `COMPLETED`. | `409`. |
| EN-015 | Inspeccionar una orden no completada. | `409`. |
| EN-016 | Rechazar sin observaciones o solo con espacios. | `400`. |
| EN-017 | Crear segunda inspección para la misma orden. | `409`. |
| EN-018 | QUALITY_INSPECTOR intenta cambiar inventario u orden. | `403`. |
| EN-019 | Desactivar al último ADMIN activo. | `409`. |
| EN-020 | Cambiar unidad de un material referenciado. | `409`. |
| EN-021 | Error inesperado del servidor. | `5xx` JSON consistente, correlación y sin stack trace. |

### 7.10 Casos borde

| ID | Caso borde | Comportamiento esperado |
|---|---|---|
| EB-001 | SKU `ABC-1` y luego ` abc-1 `. | Se consideran duplicados. |
| EB-002 | Stock disponible exactamente igual al requerimiento. | Confirmación permitida; disponible queda cero. |
| EB-003 | Dos órdenes se confirman simultáneamente contra el último stock. | Solo las que caben se confirman; ninguna deja stock negativo. |
| EB-004 | BOM cambia después de confirmar una orden. | La orden conserva snapshot y reserva originales. |
| EB-005 | BOM cambia mientras una orden sigue `DRAFT`. | Al confirmar se usa la BOM vigente más reciente. |
| EB-006 | Orden confirmada se cancela y otra intenta usar el stock. | La reserva liberada vuelve a estar disponible. |
| EB-007 | Se solicita completar dos veces por doble clic o reintento. | Un solo consumo; la repetición recibe `409`. |
| EB-008 | Falla el último movimiento durante finalización. | Toda la transacción se revierte. |
| EB-009 | Una salida manual compite con una confirmación. | Serialización funcional: solo una usa el disponible; invariantes se conservan. |
| EB-010 | Observación rechazada contiene únicamente saltos o espacios. | Se considera vacía y recibe `400`. |
| EB-011 | Consulta paginada sin coincidencias. | `200` con colección vacía y metadatos válidos. |
| EB-012 | Token expira entre cargar una pantalla y guardar. | Guardado recibe `401`; UI solicita nueva autenticación sin afirmar éxito. |
| EB-013 | Usuario es desactivado después de recibir un token. | La política de validez residual se decidirá en Fase 2 y se probará en Fase 8. |
| EB-014 | Cantidad excede precisión o máximo admitido. | `400`; límites exactos se fijan en Fase 3. |
| EB-015 | Producto desactivado aparece en una orden histórica. | La orden sigue consultable; el producto no aparece como opción nueva. |
| EB-016 | Fechas cruzan cambio de día para la zona del navegador. | API conserva UTC; UI presenta la conversión sin alterar el instante. |

### 7.11 Trazabilidad de las reglas mínimas del brief

| Regla original | Reglas consolidadas | Escenario de referencia |
|---:|---|---|
| 1. SKU no duplicado | RN-001 | EN-005, EB-001 |
| 2. Código de material no duplicado | RN-002 | EN-005 |
| 3. Cantidades no negativas | RN-003, RN-004 | EN-006 |
| 4. Inventario no negativo | RN-005, RN-010 | EN-010, EB-003, EB-009 |
| 5. BOM con al menos un material | RN-006 | EN-007 |
| 6. No confirmar sin stock | RN-008, RN-009 | EN-011 |
| 7. Cancelada no se completa | RN-011 | EN-013 |
| 8. Completada no se modifica | RN-012 | EN-012, EN-014 |
| 9. Solo inspeccionar completadas | RN-014 | EN-015 |
| 10. Rechazo requiere observaciones | RN-016 | EN-016, EB-010 |
| 11. Sin permiso devuelve `403` | RN-017 | EN-003, EN-004, EN-018 |
| 12. Datos inválidos devuelven `400` | RN-018 | EN-006, EN-007, EN-016 |
| 13. Recurso inexistente devuelve `404` | RN-019 | EN-008 |
| 14. Conflicto de negocio devuelve `409` | RN-020 | EN-005, EN-010 a EN-017 |
| 15. Error inesperado usa JSON consistente | RN-021 | EN-021 |
| 16. Operaciones importantes registradas | RN-022 | EF-011 |

## 8. Archivos creados

- `docs/phase-1-requirements.md`

## 9. Archivos modificados

- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-0-discovery.md` (solo estado de aprobación)

## 10. Comandos

No se requieren comandos de compilación, ejecución o pruebas. Esta fase es documental y no genera código.

## 11. Cómo verificar

1. Confirmar que cada módulo del alcance tiene requisitos funcionales.
2. Comprobar que RN-001 a RN-036 incluyen las 16 reglas mínimas del brief.
3. Verificar que toda acción mutable tenga un rol autorizado.
4. Recorrer el flujo principal y comprobar que no exista una transición sin actor o precondición.
5. Verificar coherencia entre reserva, cancelación y consumo.
6. Confirmar cobertura de escenarios positivos, negativos y de concurrencia.
7. Confirmar que no se definieron endpoints, tablas o clases antes de sus fases.

## 12. Pruebas relacionadas

No se crean ni ejecutan pruebas en esta fase. Los requisitos y escenarios proporcionan trazabilidad inicial para `TEST_MATRIX.md`; el diseño completo de casos se realizará en Fase 5.

## 13. Riesgos

- La estrategia funcional de reserva está definida, pero el mecanismo técnico de bloqueo/concurrencia sigue pendiente de Fases 2–3.
- La validez de tokens emitidos antes de desactivar un usuario sigue pendiente de arquitectura.
- Precisión y límites máximos de cantidades se fijarán en el modelo de datos.
- Una sola inspección simplifica el MVP y excluye formalmente reinspecciones.
- Los objetivos de rendimiento son provisionales hasta disponer de ambiente y medición.

## 14. Estado actualizado

- Fase 0: completada y aprobada.
- Fase 1: completada y aprobada el 2026-07-19.
- Código creado: ninguno.
- Pruebas ejecutadas: ninguna.

## 15. Próximo paso

Tras aprobación, iniciar la Fase 2 para diseñar arquitectura general, backend, frontend, pruebas, DevOps, autenticación, componentes, capas y dependencias. No inicializar todavía el proyecto completo.

## 16. Pregunta de aprobación

Estado de la compuerta: aprobada el 2026-07-19; la Fase 2 fue autorizada.
