# Fase 9 — Productos y materiales

## 1. Nombre de la fase

**Fase 9 — Productos y materiales.**

## 2. Objetivo

Implementar y verificar los catálogos de productos y materiales sobre el contrato, esquema y seguridad aprobados, sin iniciar BOM.

Resumen del estado anterior: las Fases 0–8 están aprobadas. El backend ya contaba con Java 17, Spring Boot, MySQL/Flyway, errores consistentes, auditoría, JWT, roles y administración de usuarios; el gate Fase 8 acumulaba 21 pruebas Java PASS.

Criterios de terminación aplicados:

- ocho operaciones de catálogo funcionales y visibles en Springdoc;
- CRUD lógico verificado sin borrado físico;
- unicidad normalizada, validación, `400/404/409` y permisos comprobados;
- balance inicial material atómico y unidad histórica protegida;
- unitarias, integración MySQL, HTTP y carrera optimista ejecutadas;
- colección Postman válida sin atribuir una ejecución Newman inexistente;
- control documental y manifiesto actualizados;
- aprobación del usuario registrada el 2026-07-19.

## 3. Dependencias

- Fase 8 aprobada y D-0001–D-0082 vigentes.
- Tablas V1 `products`, `materials`, `inventory_balances`, `bom_items`, `inventory_movements` y `production_order_materials`.
- `SecurityConfiguration`, `QaFlowPrincipal`, usuarios/actores y JWT.
- `AuditEventRecorder`/`DeferredAuditEventRecorder`.
- error JSON, correlación, `Clock`, Spring Data JPA, Flyway y Testcontainers.
- contrato `docs/api/openapi.json` y casos TC-PRD-001–006/TC-MAT-001–007.

## 4. Entradas requeridas

No se requirieron decisiones externas nuevas. Se conservaron estas entradas aprobadas:

- ADMIN administra y consulta catálogos;
- OPERATOR solo consulta catálogos;
- QUALITY_INSPECTOR no accede a endpoints generales de catálogo;
- SKU/código son únicos después de `UPPER(TRIM(...))`;
- no existe `DELETE`; la baja es lógica;
- `description: null` limpia la descripción;
- material nuevo debe crear balance `0/0/0`;
- unidad puede cambiar solo antes de cualquier historia relevante;
- versión desactualizada es `409 CONCURRENT_MODIFICATION`.

Archivos existentes reutilizados: V1 Flyway, seguridad, auditoría, errores, usuario/actor, configuración OpenAPI, tests base y los documentos aprobados de Fases 1–5.

## 5. Entregables

- Dominio JPA y repositories de producto/material.
- Enum `UnitOfMeasure` con siete valores del esquema.
- DTOs create/update/page/response y mappers.
- Services transaccionales y sorts con whitelist.
- Controllers con ocho operaciones y autorización por método.
- Balance inicial material y detección de historia de unidad.
- Auditoría de altas, actualizaciones, bajas y rechazos.
- 6 pruebas unitarias nuevas y 7 integraciones de catálogo.
- Carrera de dos escritores reales sobre MySQL.
- OpenAPI runtime de catálogo.
- Colección Postman de 12 requests, environment sin secretos y guía.
- Documentación de fase y continuidad actualizada.

## 6. Decisiones tomadas

- D-0083: catálogos con baja lógica y ocho operaciones exactas.
- D-0084: PATCH sensible a presencia y null explícito.
- D-0085: balance inicial y seguridad semántica de unidad en una frontera transaccional.
- D-0086: unicidad normalizada y optimistic locking con defensa en profundidad.
- D-0087: paginación común y actores resueltos por lote.
- D-0088: Postman acotado ahora; Newman en Fase 16.

Estas decisiones fueron aprobadas junto con la Fase 9 el 2026-07-19.

## 7. Diseño o código

### Productos

- `GET /api/v1/products`: búsqueda en SKU/nombre, filtro active, página y sort permitido.
- `POST /api/v1/products`: alta ADMIN y `201 Location`.
- `GET /api/v1/products/{productId}`: detalle ADMIN/OPERATOR.
- `PATCH /api/v1/products/{productId}`: edición/baja ADMIN con versión.
- Precheck entrega `DUPLICATE_SKU`; unique generated column resuelve la carrera final.

### Materiales

- Las cuatro operaciones equivalentes agregan filtro `unitOfMeasure`.
- El alta inserta `inventory_balances` dentro de la misma `@Transactional`.
- Un balance cero no es historia; BOM item, movimiento o material de orden sí lo son.
- El cambio inseguro devuelve `409 MATERIAL_UNIT_IMMUTABLE`.

### PATCH, mapeo y concurrencia

- Los DTOs registran presencia por setter Jackson: ausencia conserva, null explícito limpia solo campos nullable.
- El response mantiene IDs/versiones como strings e incluye actores sanitizados.
- Los actores únicos se consultan por lote por página.
- La versión del body se valida antes de mutar y JPA `@Version` impide dos commits con la misma versión.

### Seguridad y trazabilidad

- URL rules y `@PreAuthorize` aplican defensa doble.
- Éxitos se auditan con el negocio; rechazos se persisten después de completar la transacción.
- Correlation ID y error JSON permanecen uniformes.

## 8. Archivos creados

- `backend/src/main/java/com/qaflow/shared/api/PageMetadataResponse.java`
- `backend/src/main/java/com/qaflow/auth/application/ActorSummaryResolver.java`
- `backend/src/main/java/com/qaflow/product/domain/Product.java`
- `backend/src/main/java/com/qaflow/product/infrastructure/ProductRepository.java`
- `backend/src/main/java/com/qaflow/product/application/ProductService.java`
- `backend/src/main/java/com/qaflow/product/application/ProductMapper.java`
- `backend/src/main/java/com/qaflow/product/application/ProductSort.java`
- `backend/src/main/java/com/qaflow/product/api/CreateProductRequest.java`
- `backend/src/main/java/com/qaflow/product/api/UpdateProductRequest.java`
- `backend/src/main/java/com/qaflow/product/api/ProductResponse.java`
- `backend/src/main/java/com/qaflow/product/api/ProductPageResponse.java`
- `backend/src/main/java/com/qaflow/product/api/ProductController.java`
- `backend/src/main/java/com/qaflow/material/domain/Material.java`
- `backend/src/main/java/com/qaflow/material/domain/UnitOfMeasure.java`
- `backend/src/main/java/com/qaflow/material/infrastructure/MaterialRepository.java`
- `backend/src/main/java/com/qaflow/material/infrastructure/MaterialPersistenceSupport.java`
- `backend/src/main/java/com/qaflow/material/application/MaterialService.java`
- `backend/src/main/java/com/qaflow/material/application/MaterialMapper.java`
- `backend/src/main/java/com/qaflow/material/application/MaterialSort.java`
- `backend/src/main/java/com/qaflow/material/api/CreateMaterialRequest.java`
- `backend/src/main/java/com/qaflow/material/api/UpdateMaterialRequest.java`
- `backend/src/main/java/com/qaflow/material/api/MaterialResponse.java`
- `backend/src/main/java/com/qaflow/material/api/MaterialPageResponse.java`
- `backend/src/main/java/com/qaflow/material/api/MaterialController.java`
- `backend/src/test/java/com/qaflow/product/application/ProductServiceTest.java`
- `backend/src/test/java/com/qaflow/material/application/MaterialServiceTest.java`
- `backend/src/test/java/com/qaflow/integration/CatalogApiIT.java`
- `tests/postman/QAFlow.phase9.postman_collection.json`
- `tests/postman/QAFlow.local.postman_environment.json`
- `tests/postman/README.md`
- `docs/phase-9-products-materials.md`

Se retiró `backend/src/main/java/com/qaflow/user/api/PageMetadataResponse.java` después de mover su responsabilidad a `shared/api`; todas las referencias fueron actualizadas y `clean verify` descartó dependencia de la clase antigua.

## 9. Archivos modificados

- `backend/src/main/java/com/qaflow/security/SecurityConfiguration.java`
- `backend/src/main/java/com/qaflow/user/application/UserService.java`
- `backend/src/main/java/com/qaflow/user/api/UserPageResponse.java`
- `README.md`
- `backend/README.md`
- `PROJECT_STATE.md`
- `ROADMAP.md`
- `DECISIONS.md`
- `FILE_MANIFEST.md`
- `TEST_MATRIX.md`
- `OPEN_ISSUES.md`
- `INTERVIEW_NOTES.md`
- `docs/phase-8-authentication-security.md`

## 10. Comandos

Compilación intermedia reproducible:

```powershell
docker run --rm -v D:\qaflow\backend:/workspace -w /workspace `
  maven:3.9.11-eclipse-temurin-17 mvn -B -ntp -DskipTests compile
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

En un host con JDK 17 y Docker directo:

```powershell
cd backend
.\mvnw.cmd clean verify
```

Validación estructural Postman:

```powershell
Get-Content tests/postman/QAFlow.phase9.postman_collection.json -Raw | ConvertFrom-Json
Get-Content tests/postman/QAFlow.local.postman_environment.json -Raw | ConvertFrom-Json
```

Un intento con `-Dit.test=CatalogApiIT` fue fragmentado por PowerShell y no ejecutó pruebas; no se contabiliza. Los gates posteriores no usaron ese selector.

## 11. Cómo verificar

1. Ejecutar `clean verify` con Java 17 y Docker.
2. Confirmar 19 Surefire + 15 Failsafe sin failures/errors/skips.
3. Consultar `/v3/api-docs`: deben existir los ocho métodos/rutas de catálogo.
4. Como ADMIN, crear producto/material y comprobar `Location`, actores y versión `0`.
5. Confirmar balance material `on_hand=reserved=available=0`.
6. Repetir SKU/código con espacios/caso distintos y esperar `409` estable.
7. Usar OPERATOR para GET (`200`) y POST (`403`); inspector GET (`403`).
8. Probar PATCH con null, baja lógica, versión vieja y material con historia.
9. Revisar `audit_events` para SUCCESS/REJECTED y correlación.
10. Parsear ambos JSON Postman; para runner manual, completar contraseñas solo localmente.
11. Confirmar `docker ps` sin contenedores activos al terminar.

## 12. Pruebas relacionadas

Ejecución definitiva `2026-07-20T04:24:15Z–04:27:26Z` UTC:

| Suite | Tipo real | Casos | Resultado |
|---|---|---:|---|
| `ProductServiceTest` | Unitaria Mockito | 3 | PASS |
| `MaterialServiceTest` | Unitaria Mockito | 3 | PASS |
| Suites unit/web de Fase 8 | Unitarias/MockMvc | 13 | PASS |
| `CatalogApiIT` | Integración HTTP/JPA concurrente/MySQL | 7 | PASS |
| `BackendInfrastructureIT` | Integración Spring/MySQL | 2 | PASS |
| `SecurityApiIT` | Integración HTTP/Security/MySQL | 6 | PASS |
| Total | 19 Surefire + 15 Failsafe | 34 | PASS |

La prueba concurrente sincroniza dos transacciones después de leer versión 0 y verifica un commit, un optimistic conflict y versión final 1. No usa `sleep`.

Postman: 12 requests/7 variables parseados correctamente. Estado funcional Newman: `NOT_RUN` hasta Fase 16.

## 13. Riesgos

- El predicado de unidad debe ampliarse si otra tabla futura representa uso histórico material.
- Resolver actores por lote evita N+1 por fila, pero su costo debe medirse con páginas/datos reales.
- La colección Postman depende de usuarios demo y contraseñas locales no versionadas.
- No hay ETag; el MVP usa versión contractual en body.
- Fase 9 no demuestra reglas de producto/material inactivo dentro de BOM/orden porque esos módulos aún no existen.
- HMAC, `sessionStorage`, warnings `TINYINT(1)`, política de triggers y runner anidado conservan los riesgos ya registrados.

## 14. Estado actualizado

- Fases 0–8: completadas y aprobadas.
- Fase 9: completada, verificada y aprobada el 2026-07-19.
- Build Java 17 desde limpio: PASS.
- 19 pruebas unitarias/web: PASS.
- 15 integraciones MySQL: PASS.
- OpenAPI runtime: 15/39 operaciones; ocho de catálogo PASS.
- Postman Fase 9: JSON PASS estructural; Newman `NOT_RUN`.
- Fase 10: autorizada para iniciar.

## 15. Próximo paso

La aprobación autorizó iniciar únicamente Fase 10: BOM versionada con al menos un material, materiales únicos, cantidades positivas, producto/material activo, cuatro endpoints, auditoría y pruebas. No iniciar inventario.

## 16. Registro de aprobación

Fase 9 aprobada por el usuario el 2026-07-19. Se autoriza iniciar únicamente BOM de la Fase 10.
