# Fase 10 — BOM

## 1. Nombre de la fase

**Fase 10 — Lista de materiales (BOM).**

## 2. Objetivo

Implementar y verificar la publicación versionada de listas de materiales por producto, conservando historia, exactitud decimal, permisos, concurrencia y auditoría, sin iniciar inventario operativo.

Resumen del estado anterior: las Fases 0–9 están aprobadas. El backend ya contaba con Java 17, Spring Boot, MySQL/Flyway, seguridad JWT, usuarios/actores, auditoría, errores consistentes y maestros activos de productos/materiales; el gate Fase 9 acumulaba 34 pruebas Java PASS.

Criterios de terminación aplicados:

- cuatro operaciones BOM funcionales y visibles en Springdoc;
- al menos una línea, máximo 100, sin material duplicado y con cantidad positiva exacta;
- producto/material existente y activo al publicar;
- nueva revisión supersede la anterior sin mutar líneas históricas;
- una sola revisión vigente incluso bajo dos publicaciones concurrentes;
- `400/401/403/404/409`, auditoría y paginación comprobados;
- unitarias, integración HTTP/MySQL, trigger y carrera real ejecutadas;
- documentos de continuidad y manifiesto actualizados;
- aprobación del usuario registrada el 2026-07-20.

## 3. Dependencias

- Fase 9 aprobada y D-0001–D-0088 vigentes.
- Tablas V1 `boms`, `bom_items`, `products`, `materials`, usuarios y auditoría.
- Generated column `current_product_id`, uniques, checks, FK e inmutabilidad de líneas.
- `ProductRepository`, `MaterialRepository`, `QaFlowPrincipal`, usuarios/actores y JWT.
- `AuditEventRecorder`/`DeferredAuditEventRecorder`.
- error JSON, correlación, `Clock`, paginación, Spring Data JPA, Flyway y Testcontainers.
- contrato `docs/api/openapi.json` y casos TC-BOM-001–008.

## 4. Entradas requeridas

No se requirieron decisiones externas nuevas. Se conservaron estas entradas aprobadas:

- ADMIN publica y consulta BOM; OPERATOR solo consulta; QUALITY_INSPECTOR no accede;
- una BOM contiene 1–100 materiales únicos;
- `quantityPerUnit` es positiva, hasta 13 enteros/6 decimales y viaja como string;
- producto y materiales deben existir y estar activos al publicar;
- reemplazar crea revisión nueva y conserva la anterior;
- solo una revisión puede ser vigente;
- `/current` sin vigente devuelve `404 CURRENT_BOM_NOT_FOUND`;
- una publicación concurrente detectada devuelve `409 BOM_CONCURRENTLY_CHANGED`.

Archivos existentes reutilizados: entidades/repositorios de producto/material, V1 Flyway, seguridad, auditoría, errores, usuario/actor, OpenAPI estático y harnesses de integración.

## 5. Entregables

- Dominio JPA `Bom`/`BomItem` y repositories.
- DTOs de publicación, detalle, resumen y página.
- Parser compartido de cantidades exactas.
- Mapper con materiales, unidades, actores y escala decimal canónica.
- Service transaccional con publicación, historial, detalle y concurrencia.
- Controller con cuatro operaciones y autorización por método.
- Locks pesimistas de producto/material añadidos a repositories de maestros.
- Auditoría de publicación exitosa y rechazos de negocio.
- 7 pruebas unitarias nuevas y 7 integraciones BOM.
- Carrera concurrente HTTP/MySQL determinista y verificación de trigger.
- OpenAPI runtime con cuatro operaciones BOM.
- Corrección y smoke de arranque de Maven Wrapper en Windows.
- Documentación de fase y continuidad actualizada.

## 6. Decisiones tomadas

- D-0089: revisiones append-only con transición vigente → superseded.
- D-0090: serialización por producto y locks de materiales por id ascendente.
- D-0091: cantidad string, `BigDecimal` y escala canónica seis.
- D-0092: maestros activos al publicar e historia siempre consultable.
- D-0093: carrera determinista con barrera solo en la observación, HTTP/JPA/MySQL reales.

Estas decisiones fueron aprobadas junto con la Fase 10 el 2026-07-20.

## 7. Diseño o código

### Modelo y revisión

- `Bom` referencia producto, revisión, vigencia, creador y datos de superseded.
- `BomItem` referencia material y `quantity_per_unit DECIMAL(19,6)`.
- Cada POST calcula `max(revision)+1`, marca la vigente anterior como superseded y persiste una revisión completa nueva.
- No existe endpoint para editar o eliminar una revisión; los triggers bloquean update/delete de líneas.

### Operaciones

- `GET /api/v1/products/{productId}/boms`: historial paginado, sort por revisión o creación.
- `POST /api/v1/products/{productId}/boms`: publicación ADMIN y `201 Location` a la revisión.
- `GET /api/v1/products/{productId}/boms/current`: detalle vigente.
- `GET /api/v1/products/{productId}/boms/{revisionNumber}`: detalle histórico.

### Validación y exactitud

- Bean Validation controla forma, tamaño y formato en HTTP; el servicio repite invariantes críticas para uso interno.
- Duplicado en request devuelve `400 DUPLICATE_BOM_MATERIAL` antes de leer/escribir la BOM.
- `ExactQuantityParser` no redondea: rechaza cero, negativos, notación científica y más de seis decimales.
- Responses usan `toPlainString()` y escala seis.

### Concurrencia y transacción

1. La transacción observa el id vigente sin lock.
2. Bloquea el producto con `PESSIMISTIC_WRITE`.
3. Relee la vigente con lock y compara contra lo observado.
4. Bloquea materiales por id ascendente y valida presencia/estado.
5. Supersede la anterior, inserta cabecera/líneas nuevas y auditoría de éxito.
6. Cualquier excepción revierte toda la publicación; unique current es defensa final.

### Seguridad y trazabilidad

- `@PreAuthorize` aplica ADMIN para POST y ADMIN/OPERATOR para GET; la URL de productos conserva defensa adicional.
- Éxitos se auditan dentro de la transacción; rechazos de negocio se registran después de completion.
- El historial resuelve actores por lote y los responses no exponen datos sensibles.

## 8. Archivos creados

- `backend/src/main/java/com/qaflow/bom/domain/Bom.java`
- `backend/src/main/java/com/qaflow/bom/domain/BomItem.java`
- `backend/src/main/java/com/qaflow/bom/infrastructure/BomRepository.java`
- `backend/src/main/java/com/qaflow/bom/infrastructure/BomItemRepository.java`
- `backend/src/main/java/com/qaflow/bom/application/BomService.java`
- `backend/src/main/java/com/qaflow/bom/application/BomMapper.java`
- `backend/src/main/java/com/qaflow/bom/application/BomSort.java`
- `backend/src/main/java/com/qaflow/bom/api/BomController.java`
- `backend/src/main/java/com/qaflow/bom/api/BomItemRequest.java`
- `backend/src/main/java/com/qaflow/bom/api/PublishBomRequest.java`
- `backend/src/main/java/com/qaflow/bom/api/BomItemResponse.java`
- `backend/src/main/java/com/qaflow/bom/api/BomProductSummaryResponse.java`
- `backend/src/main/java/com/qaflow/bom/api/BomResponse.java`
- `backend/src/main/java/com/qaflow/bom/api/BomSummaryResponse.java`
- `backend/src/main/java/com/qaflow/bom/api/BomSummaryPageResponse.java`
- `backend/src/main/java/com/qaflow/shared/validation/ExactQuantityParser.java`
- `backend/src/test/java/com/qaflow/bom/domain/BomTest.java`
- `backend/src/test/java/com/qaflow/bom/application/BomServiceTest.java`
- `backend/src/test/java/com/qaflow/integration/BomApiIT.java`
- `docs/phase-10-bom.md`

## 9. Archivos modificados

- `backend/src/main/java/com/qaflow/product/infrastructure/ProductRepository.java`
- `backend/src/main/java/com/qaflow/material/infrastructure/MaterialRepository.java`
- `backend/mvnw.cmd`
- `README.md`
- `backend/README.md`
- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-9-products-materials.md`

## 10. Comandos

Pruebas unitarias Java 17:

```powershell
docker run --rm -v D:\qaflow\backend:/workspace -w /workspace `
  maven:3.9.11-eclipse-temurin-17 mvn -B -ntp test
```

Gate definitivo desde limpio con Testcontainers anidado:

```powershell
docker run --rm `
  -e TESTCONTAINERS_RYUK_DISABLED=true `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v D:\qaflow\backend:/workspace -w /workspace `
  maven:3.9.11-eclipse-temurin-17 mvn -B -ntp clean verify
```

Smoke del wrapper corregido:

```powershell
cd backend
.\mvnw.cmd -version
```

En un host con JDK 17 y Docker directo:

```powershell
cd backend
.\mvnw.cmd clean verify
```

## 11. Cómo verificar

1. Ejecutar `clean verify` con Java 17 y Docker.
2. Confirmar 26 Surefire + 22 Failsafe sin failures/errors/skips.
3. Consultar `/v3/api-docs` y confirmar las cuatro rutas/métodos BOM.
4. Crear producto y materiales activos; publicar una BOM y comprobar `201`, `Location`, revisión 1 y cantidades con seis decimales.
5. Publicar otra y comprobar revisión 2 vigente, revisión 1 superseded y solo una fila current.
6. Probar lista vacía, duplicado, cero, negativo, séptimo decimal y esperar `400` sin filas parciales.
7. Probar referencias ausentes (`404`) e inactivas (`409`).
8. Usar OPERATOR para GET (`200`) y POST (`403`); inspector GET (`403`); anónimo (`401`).
9. Revisar `audit_events` para SUCCESS/REJECTED y correlación.
10. Intentar actualizar `bom_items` con SQL y confirmar rechazo del trigger.
11. Ejecutar la carrera sincronizada y confirmar un `201`, un `409` y una sola revisión.
12. Confirmar `docker ps` sin contenedores activos al terminar.

## 12. Pruebas relacionadas

Ejecución definitiva terminada `2026-07-20T05:30:48Z` UTC:

| Suite | Tipo real | Casos | Resultado |
|---|---|---:|---|
| `BomServiceTest` | Unitaria Mockito | 5 | PASS |
| `BomTest` | Unitaria de dominio | 2 | PASS |
| Suites unit/web previas | Unitarias/MockMvc | 19 | PASS |
| `BomApiIT` | Integración HTTP/JPA/MySQL/concurrencia | 7 | PASS |
| `CatalogApiIT` | Integración HTTP/JPA/MySQL | 7 | PASS |
| `SecurityApiIT` | Integración HTTP/Security/MySQL | 6 | PASS |
| `BackendInfrastructureIT` | Integración Spring/Flyway/MySQL | 2 | PASS |
| Total | 26 Surefire + 22 Failsafe | 48 | PASS |

La carrera BOM no usa `sleep`: una barrera sincroniza únicamente la observación inicial; ambas solicitudes ejecutan HTTP, transacciones, locks, JPA y MySQL reales.

## 13. Riesgos

- El lock de producto serializa publicaciones del mismo producto; throughput, timeout y deadlock bajo carga aún no se han medido.
- La historia conserva referencias a maestros y muestra sus etiquetas actuales; no es snapshot textual de nombre/código.
- La regla 1–100 se impone en aplicación; una escritura SQL privilegiada puede infringir el mínimo de líneas aunque FK/check/unique protejan otras invariantes.
- No existe todavía orden que capture una revisión BOM; esa trazabilidad se implementa en Fase 12.
- No se creó una colección Postman específica de BOM: la colección consolidada y Newman pertenecen a Fase 16; la evidencia actual es JUnit HTTP/MySQL.
- HMAC, `sessionStorage`, warnings `TINYINT(1)`, política de triggers y runner anidado conservan sus riesgos registrados.

## 14. Estado actualizado

- Fases 0–9: completadas y aprobadas.
- Fase 10: completada, verificada y aprobada el 2026-07-20.
- Build Java 17 desde limpio: PASS.
- 26 pruebas unitarias/web: PASS.
- 22 integraciones MySQL: PASS.
- OpenAPI runtime: 19/39 operaciones; cuatro de BOM PASS.
- Maven Wrapper Windows: arranque Maven 3.9.11 PASS; host Java 21 no usado como gate.
- Fase 11: autorizada para iniciar.

## 15. Próximo paso

La aprobación autoriza iniciar únicamente Fase 11: revisar el contrato aprobado de inventario e implementar movimientos, balance no negativo, historial, locks/transacciones, endpoints, consultas y pruebas. No iniciar órdenes de producción.

## 16. Registro de aprobación

Fase 10 aprobada por el usuario el 2026-07-20. Se autoriza iniciar únicamente inventario de la Fase 11.
