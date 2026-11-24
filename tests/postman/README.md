# Postman — Fase 9

Esta carpeta contiene una colección ejecutable y un environment sin secretos para validar productos y materiales.

1. Importar `QAFlow.phase9.postman_collection.json` y `QAFlow.local.postman_environment.json`.
2. Completar en el environment las contraseñas de los tres usuarios demo creados mediante el bootstrap opt-in.
3. Iniciar el backend local y ejecutar la colección en orden con Collection Runner.

La colección autentica los tres roles, genera SKU/código únicos, conserva IDs/versiones y valida creación, duplicados, actualización, consulta y permisos. La automatización Newman, el reporte HTML/JUnit y la colección consolidada pertenecen a la Fase 16; estos artefactos de Fase 9 se validan estructuralmente, pero no se presentan como una ejecución Newman.
