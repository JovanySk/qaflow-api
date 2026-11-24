# ROADMAP

## Reglas de ejecución

- Las fases se ejecutan en orden.
- Una fase permanece pendiente de aprobación aunque sus entregables estén preparados.
- No se inicia la fase siguiente hasta actualizar estado, manifiesto, decisiones, pruebas e incidencias aplicables.
- Los criterios de terminación generales del brief se suman al criterio específico indicado abajo.

## Plan completo

| Orden | Fase | Estado | Dependencia | Criterio de terminación específico |
|---:|---|---|---|---|
| 0 | Descubrimiento y restricciones | Completada y aprobada | Brief inicial | Alcance, exclusiones, MVP, ambiente, riesgos y roadmap documentados y aprobados. |
| 1 | Requisitos funcionales | Completada y aprobada | Fase 0 aprobada | Requisitos, usos, historias, aceptación, permisos y escenarios sin contradicciones. |
| 2 | Arquitectura | Completada y aprobada | Fase 1 aprobada | Arquitecturas y flujos documentados; decisiones técnicas registradas. |
| 3 | Modelo de datos | Completada y aprobada | Fase 2 aprobada | Modelo, restricciones, auditoría, SQL, datos y consultas validados conceptualmente. |
| 4 | Contrato de API | Completada y aprobada | Fase 3 aprobada | Endpoints, DTOs, autorización, errores y OpenAPI preliminar coherentes. |
| 5 | Estrategia de pruebas | Completada y aprobada | Fase 4 aprobada | Estrategia, matriz, casos, prioridades, severidades y criterios definidos. |
| 6 | Estructura del repositorio | Completada y aprobada | Fase 5 aprobada | Árbol, ownership, autoridades, convenciones Git y rutas planificadas documentados y aprobados; sin materialización anticipada. |
| 7 | Inicialización del backend | Completada y aprobada | Fase 6 aprobada | Backend compila, inicia, conecta a MySQL y expone Swagger con evidencia real. |
| 8 | Autenticación y seguridad | Completada y aprobada | Fase 7 aprobada | Login, JWT, roles, `401`/`403`, datos iniciales y pruebas verificados. |
| 9 | Productos y materiales | Completada y aprobada | Fase 8 aprobada | CRUD, unicidad, validaciones, permisos, Swagger, Postman y pruebas verificados. |
| 10 | BOM | Completada y aprobada | Fase 9 aprobada | BOM y validaciones de materiales/cantidades verificadas mediante endpoints y pruebas. |
| 11 | Inventario | En progreso | Fase 10 aprobada | Movimientos y stock transaccional sin negativos, con historial, SQL y pruebas. |
| 12 | Órdenes de producción | No iniciada | Fase 11 aprobada | Estados, transiciones, stock y consumo transaccional verificados. |
| 13 | Inspecciones de calidad | No iniciada | Fase 12 aprobada | Inspecciones, resultados, permisos y reglas negativas verificados. |
| 14 | Frontend | No iniciada | Fase 13 aprobada | Módulos UI integrados y aprobados uno por uno, con rutas, permisos y `data-cy`. |
| 15 | Pruebas unitarias | No iniciada | Fase 14 aprobada | Suite JUnit/Mockito ejecutada y reporte generado; clasificación correcta. |
| 16 | Postman y Newman | No iniciada | Fase 15 aprobada | Colección, environment, assertions y reportes Newman ejecutados. |
| 17 | Cypress | No iniciada | Fase 16 aprobada | Once flujos requeridos automatizados y ejecutados sin esperas fijas injustificadas. |
| 18 | SQL y validación de datos | No iniciada | Fase 17 aprobada | Consultas de integridad y auditoría creadas y ejecutadas contra datos conocidos. |
| 19 | Pruebas de rendimiento | No iniciada | Fase 18 aprobada | Escenarios Locust ejecutados con carga, resultados y limitaciones documentados. |
| 20 | Docker | No iniciada | Fase 19 aprobada | `docker compose up --build` verificado con servicios saludables. |
| 21 | GitLab CI/CD | No iniciada | Fase 20 aprobada | Pipeline, jobs, reportes, artefactos y condiciones documentados y, si hay runner, ejecutados. |
| 22 | Documentación de defectos | No iniciada | Fase 21 aprobada | Cinco defectos reales o hallazgos históricos honestos documentados con regresión. |
| 23 | Documentación final | No iniciada | Fase 22 aprobada | README y documentos permiten instalar, ejecutar, probar y comprender limitaciones. |
| 24 | Preparación de entrevista | No iniciada | Fase 23 aprobada | Guiones, demo, preguntas, plan alterno y checklist ensayables. |
| 25 | Auditoría final | No iniciada | Fase 24 aprobada | Revisión integral ejecutada; reporte final distingue hechos, riesgos y límites. |

## Dependencias transversales

- El modelo de permisos de Fase 1 condiciona seguridad, API, frontend y pruebas.
- La estrategia de concurrencia de inventario debe resolverse antes de Fases 11–12.
- El contrato de errores condiciona backend, Postman, Cypress y documentación.
- Los selectores estables del frontend condicionan Cypress.
- Docker es la referencia reproducible para API, E2E, rendimiento y CI.
- Las evidencias de pruebas alimentan defectos, documentación, entrevista y auditoría.
