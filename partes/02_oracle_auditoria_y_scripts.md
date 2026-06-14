# 02 - Oracle, auditoría y scripts

Eres responsable únicamente de la parte Oracle del proyecto `sync_ad_maximo`.

Objetivo:
- Definir la persistencia Oracle para auditoría, trazabilidad y soporte de sincronización.
- Proponer los scripts SQL necesarios para operar con MAXIMO sin romper compatibilidad.

Alcance:
- Tablas de auditoría.
- Secuencias.
- Índices.
- Restricciones.
- Tablas auxiliares para ejecución y logging.
- Estrategia de evolución de esquema.

Límites:
- No diseñes la interfaz web.
- No diseñes la lógica LDAP.
- No diseñes el orquestador completo.
- No mezcles responsabilidades de presentación o autenticación con esta capa.

Lo que debes producir:
- DDL Oracle completo y claro.
- Nombre y propósito de cada tabla.
- Columnas mínimas necesarias para auditoría de sincronización.
- Índices recomendados.
- Secuencias necesarias.
- Consideraciones de rendimiento e integridad.

Debes cubrir, como mínimo:
- Registro de ejecuciones.
- Detalle de operaciones por usuario.
- Errores y excepciones.
- Resultado de alta/cambio/baja.
- Trazabilidad de origen AD → MAXIMO.

Condiciones importantes:
- Diseña pensando en Oracle.
- Mantén nombres consistentes y legibles.
- Evita sobreingeniería.
- Si hay supuestos sobre el esquema de MAXIMO, decláralos.

Tu salida debe incluir:
- `Resumen del modelo Oracle`
- `Tablas propuestas`
- `Secuencias e índices`
- `SQL sugerido`
- `Consideraciones operativas`

No escribas código Java. No inventes endpoints web. No mezcles validaciones de negocio ajenas a persistencia.
