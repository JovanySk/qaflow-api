# Fase 0 — Descubrimiento y restricciones

## 1. Nombre de la fase

Fase 0. Descubrimiento y restricciones.

## 2. Objetivo

Definir el límite verificable de **QAFlow Manufacturing Testing Portfolio**, confirmar sus actores, flujo principal, restricciones, herramientas y condiciones de éxito antes de diseñar requisitos o escribir código.

QAFlow será una aplicación web simplificada de manufactura que, además de resolver un flujo funcional completo, demostrará competencias reales de desarrollo full stack, QA Automation y DevOps durante una entrevista técnica.

## 3. Dependencias

- Brief inicial aprobado por la persona responsable del proyecto.
- Disponibilidad posterior de un entorno con Java 17, Node.js, Docker, Git y las herramientas de automatización indicadas.
- Aprobación explícita de esta fase antes de iniciar la Fase 1.

## 4. Entradas requeridas

El brief proporcionado cubre las entradas necesarias para esta fase:

- Objetivo profesional del portafolio.
- Tecnologías obligatorias.
- Módulos, roles, permisos iniciales y flujo de negocio.
- Reglas de negocio mínimas.
- Fases, protocolo de continuidad y criterios de terminación.

No se requiere código ni una base de datos en esta fase. Las ambigüedades que no impiden delimitar el MVP se registran en `OPEN_ISSUES.md` y deberán resolverse en la fase indicada.

## 5. Alcance inicial

El MVP incluye:

1. Autenticación mediante credenciales y JWT.
2. Administración de usuarios y roles `ADMIN`, `OPERATOR` y `QUALITY_INSPECTOR`.
3. Gestión de productos y materiales con identificadores únicos.
4. Definición de una BOM válida por producto.
5. Inventario de materiales con entradas, salidas, stock actual e historial.
6. Órdenes de producción con estados y transiciones controladas.
7. Validación de disponibilidad y consumo transaccional de materiales.
8. Inspecciones de calidad sobre órdenes completadas.
9. Dashboard básico con información operativa útil, no analítica avanzada.
10. Trazabilidad y auditoría de operaciones importantes.
11. API REST documentada, cliente web React y manejo consistente de errores.
12. Automatización unitaria, API, E2E, SQL y de rendimiento en el nivel adecuado.
13. Ejecución reproducible con Docker Compose y pipeline de GitLab CI/CD.
14. Documentación técnica, evidencias y guion de entrevista.

## 6. Exclusiones

Quedan fuera del MVP, salvo aprobación mediante una decisión posterior:

- Contabilidad, facturación, ventas, compras y proveedores.
- Planeación MRP, pronósticos, capacidad finita y programación avanzada.
- Múltiples plantas, múltiples almacenes o transferencias entre almacenes.
- Lotes, números de serie, caducidades y trazabilidad regulatoria especializada.
- Costeo industrial, nómina y mantenimiento de maquinaria.
- Gestión documental, notificaciones por correo, chat o aprobaciones multinivel.
- SSO, OAuth social, MFA y administración corporativa de identidades.
- Aplicación móvil nativa.
- Kubernetes, despliegue productivo en nube y alta disponibilidad.
- Observabilidad empresarial completa, SIEM o respuesta automática a incidentes.
- Reportería BI avanzada y exportaciones masivas.

Estas exclusiones evitan convertir QAFlow en un ERP y permiten priorizar un flujo demostrable, pruebas útiles y documentación honesta.

## 7. Usuarios y roles

### ADMIN

- Gestiona usuarios.
- Gestiona productos.
- Gestiona materiales.
- Consulta todos los módulos.
- Los permisos mutables sobre BOM, inventario y órdenes se detallarán en la Fase 1; no se asumirán silenciosamente.

### OPERATOR

- Registra movimientos de inventario.
- Crea órdenes de producción.
- Confirma, completa o cancela órdenes de acuerdo con las transiciones válidas.

### QUALITY_INSPECTOR

- Consulta órdenes completadas.
- Crea inspecciones.
- Aprueba o rechaza el resultado de calidad.

### Usuario no autenticado

- Solo puede acceder al inicio de sesión y a los endpoints públicos que se definan explícitamente.
- No puede consultar ni modificar datos operativos.

## 8. Flujo principal

1. El usuario inicia sesión.
2. Un usuario autorizado registra un material.
3. Un usuario autorizado registra un producto.
4. Se define una BOM no vacía para el producto.
5. Un operador registra la entrada de materiales al inventario.
6. Un operador crea una orden de producción.
7. El sistema calcula la necesidad según BOM y cantidad de la orden.
8. El sistema impide confirmar si no existe stock suficiente.
9. La orden avanza mediante transiciones válidas hasta completarse.
10. Al completar, el sistema descuenta los materiales de forma transaccional y conserva el historial.
11. Un inspector autorizado registra una inspección de la orden completada.
12. La inspección se aprueba o rechaza; un rechazo exige observaciones.
13. El sistema conserva actor, fecha y operación para la trazabilidad relevante.

La estrategia para evitar sobreasignación de stock entre varias órdenes confirmadas se resolverá antes de implementar inventario y órdenes.

## 9. Restricciones técnicas

- Backend: Java 17, Spring Boot, Spring Data JPA, Spring Security y JWT.
- Persistencia: MySQL y SQL explícito para validaciones de datos.
- API: REST sobre HTTP con JSON y códigos semánticos `400`, `401`, `403`, `404`, `409` y `5xx`.
- Frontend: React con Vite, Bootstrap y Axios.
- Pruebas backend: JUnit 5 y Mockito; las pruebas con mocks no se etiquetarán como integración.
- Pruebas API: Postman y Newman.
- Pruebas E2E: Cypress con `data-cy`, interceptaciones y sin esperas fijas injustificadas.
- Rendimiento: Locust con alcance y limitaciones documentados.
- Empaquetado: Docker, Docker Compose, volúmenes, red y healthchecks.
- CI/CD: GitLab CI con reportes y artefactos.
- Control de versiones: Git.
- No se afirmará que una compilación, prueba, conexión o despliegue funciona sin ejecutarlo y conservar evidencia.
- No se usarán métodos vacíos, pseudocódigo como entrega final ni `TODO` como sustituto de una solución.
- Los identificadores técnicos de código y API se escribirán en inglés; la documentación del proyecto podrá mantenerse en español.

## 10. Definición del MVP

El MVP está completo cuando existe un recorrido integrado y reproducible desde login hasta inspección, con permisos, errores y trazabilidad verificables. Debe incluir como mínimo:

- Los nueve módulos del alcance en el nivel necesario para el flujo principal.
- Tres roles y una matriz de autorización aplicada tanto en backend como en UI.
- Persistencia MySQL con restricciones e índices coherentes.
- Contratos REST documentados.
- Pruebas automatizadas de los riesgos principales y casos negativos.
- Arranque documentado local y mediante Docker Compose.
- Pipeline documentado y ejecutado cuando exista un runner compatible.
- Evidencia verificable y limitaciones declaradas.
- Ausencia de defectos críticos conocidos que bloqueen el flujo de demostración.

## 11. Herramientas requeridas

### Obligatorias para construir y ejecutar

- JDK 17.
- Maven Wrapper; una instalación global de Maven será opcional.
- Una versión LTS de Node.js compatible con la versión de Vite seleccionada y npm.
- MySQL 8.x o el contenedor equivalente.
- Docker Engine o Docker Desktop con Docker Compose v2.
- Git.
- Navegador Chromium compatible con Cypress.

### Obligatorias para probar y documentar

- JUnit 5 y Mockito, gestionados por Maven.
- Postman y Newman, con Newman gestionado por npm.
- Cypress, gestionado por npm.
- Python 3 y Locust.
- Cliente MySQL o herramienta equivalente para consultas de validación.
- Swagger UI/OpenAPI integrado en el backend.

### Necesarias para CI/CD

- Repositorio GitLab.
- GitLab Runner con soporte para los servicios o contenedores definidos por el pipeline.
- Almacenamiento de artefactos y variables protegidas en GitLab.

Las versiones exactas se fijarán únicamente después de validar compatibilidad en las fases de arquitectura e inicialización.

## 12. Requisitos del ambiente local

- Sistema operativo capaz de ejecutar Java 17, Node.js y Docker Compose; el workspace actual usa Windows y PowerShell.
- Puertos configurables y libres para frontend, backend y MySQL.
- Al menos 8 GB de RAM recomendados; 4 GB libres para el conjunto de contenedores es una referencia inicial, no una medición.
- Al menos 5 GB de espacio libre para imágenes, dependencias, base de datos y reportes.
- Virtualización habilitada si se utiliza Docker Desktop.
- Acceso a registros de Maven, npm, imágenes Docker y GitLab durante instalación o CI.
- Variables sensibles en `.env`/variables de CI no versionadas; se proporcionarán plantillas sin secretos.
- Terminaciones de línea y scripts compatibles con Windows y el entorno Linux de CI.

Estado de validación: las versiones y disponibilidad real del toolchain local aún no se han verificado. Esta comprobación se mantiene abierta y deberá cerrarse antes de declarar ejecutable el backend.

## 13. Criterios de éxito del proyecto

1. `docker compose up --build` inicia los componentes y los healthchecks alcanzan estado saludable.
2. Un usuario autorizado completa el flujo material → producto → BOM → inventario → orden → inspección.
3. Las reglas de negocio críticas bloquean datos o transiciones inválidas con el código HTTP acordado.
4. Un usuario sin permisos recibe `403` y una petición sin autenticación válida recibe `401` cuando corresponda.
5. El inventario no queda negativo, incluso ante operaciones concurrentes consideradas por el diseño.
6. Las operaciones importantes pueden rastrearse hasta actor y fecha.
7. Las suites automatizadas relevantes generan resultados reproducibles y evidencia.
8. La documentación permite instalar, ejecutar, probar y demostrar el sistema sin conocimiento implícito.
9. El repositorio no contiene secretos ni afirmaciones de calidad sin verificación.
10. La demostración de entrevista puede ejecutarse en ocho minutos y dispone de un plan alterno.

## 14. Riesgos iniciales

| ID | Riesgo | Impacto | Tratamiento inicial |
|---|---|---|---|
| R-001 | Sobreasignación de inventario entre órdenes concurrentes | Alto | Definir reserva o bloqueo transaccional antes de Fases 11–12. |
| R-002 | Alcance creciente hacia funciones de ERP | Alto | Aplicar exclusiones y aprobación por fase. |
| R-003 | Permisos ambiguos para BOM, inventario y dashboard | Medio | Cerrar matriz detallada en Fase 1. |
| R-004 | Diferencias entre Windows local y Linux CI/contenedores | Alto | Usar wrappers, contenedores, variables y pruebas de arranque. |
| R-005 | Pruebas E2E frágiles o dependientes de datos | Medio | Definir datos controlados, API de preparación e identificadores estables. |
| R-006 | Secretos o credenciales de demostración versionados | Alto | Plantillas, variables y escaneo final; separar credenciales de producción. |
| R-007 | Pipeline demasiado pesado para un portafolio | Medio | Separar pruebas obligatorias y rendimiento opcional con criterios explícitos. |
| R-008 | Evidencia no reproducible | Alto | Registrar comandos, versiones, reportes y resultados reales por fase. |

El detalle y seguimiento se mantiene en `OPEN_ISSUES.md`.

## 15. Roadmap general

El orden obligatorio es secuencial y cada fase tiene una compuerta de aprobación:

1. **Definición (Fases 0–5):** descubrimiento, requisitos, arquitectura, datos, API y estrategia QA.
2. **Fundación (Fases 6–8):** repositorio, backend base y seguridad.
3. **Dominio y UI (Fases 9–14):** módulos del flujo y frontend aprobado por módulo.
4. **Automatización y entrega (Fases 15–21):** pruebas, SQL, rendimiento, Docker y CI/CD.
5. **Evidencia y cierre (Fases 22–25):** defectos, documentación, entrevista y auditoría final.

El detalle, dependencias y criterio de cierre de las 26 fases se encuentra en `ROADMAP.md`.

## 16. Criterios para considerar terminada la Fase 0

- Alcance y exclusiones explícitos.
- Actores, roles y flujo principal identificados.
- Restricciones, herramientas y ambiente local definidos.
- MVP y criterios de éxito delimitados.
- Riesgos y ambigüedades registrados con fase objetivo.
- Roadmap general y documentos de continuidad creados.
- No se ha generado código.
- La persona responsable aprueba avanzar a la Fase 1.

Estado: completada y aprobada el 2026-07-19. Esta línea fue actualizada administrativamente durante la Fase 1; el contenido de descubrimiento no cambió.
