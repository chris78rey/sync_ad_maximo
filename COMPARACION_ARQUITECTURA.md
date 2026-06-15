# Comparación de Arquitectura

Comparación entre lo que la aplicación **debería hacer** según [instr/instrucciones.md](/D:/CodexProjects/sync_ad_maximo/instr/instrucciones.md) y lo que **hay hoy** en el repositorio.

## Resumen

La base actual es funcional, pero todavía no está descompuesta con la misma granularidad que pide la especificación. Lo más importante ya existe: web, LDAP, Oracle, auditoría, scheduler y orquestación. Lo que falta es separar responsabilidades y completar algunos flujos de negocio.

## Tabla Comparativa

| Requerido | Existente hoy | Faltante o débil | Riesgo | Prioridad |
|---|---|---|---|---|
| WAR para Tomcat 9 | Sí, el proyecto empaqueta WAR y usa `javax.servlet` | Falta documentar mejor el despliegue estándar | Bajo | Media |
| Configuración externa por ambiente | Sí, `PropertyLoader` y `AppConfig` leen configuración externa | Falta una guía formal de configuración | Medio | Alta |
| Login contra AD | Sí, `LoginServlet`, `AuthFilter` y `DirectoryBackedWebAuthenticator` | No hay una integración AD completa garantizada en todos los entornos | Medio | Alta |
| Acceso solo a `maxadmin` | Sí, el filtro y el login restringen acceso | Falta reforzar pruebas y documentación de ese comportamiento | Medio | Alta |
| Sincronizar AD y MAXIMO por cédula | Sí, `SyncOrchestrator` lo hace | La lógica está concentrada en una sola clase | Medio | Alta |
| Detectar cambio de login | Parcial | No está separado como proceso explícito de migración | Medio | Alta |
| Migración de usuarios | Parcial | No existe `MigrationService` separado | Alto | Alta |
| Creación de usuarios nuevos | Parcial | No existe `CreationService` separado | Alto | Alta |
| Inactivación de usuarios | Parcial | No existe `InactivationService` separado | Alto | Alta |
| Sincronización de correo | Parcial | No existe `EmailSyncService` separado | Alto | Alta |
| Validar duplicados y conflictos | Sí, parte de la lógica actual y tests | Falta una capa más explícita y modular | Medio | Alta |
| Normalización de cédula, usuario y correo | Sí | La estrategia aún está bastante concentrada | Medio | Media |
| Auditoría en Oracle | Sí, `JdbcAuditRepository` persiste ejecución, issues, accesos y correos | Falta revisar cobertura funcional completa | Medio | Alta |
| Consulta de históricos desde web | Parcial | Hay reportes, pero no una capa formal de consulta histórica | Medio | Alta |
| Exportar CSV bajo demanda | Sí, `ReportServlet` soporta CSV | Falta consolidarlo como capa de reporte formal | Bajo | Media |
| Envío automático de reporte diario | Parcial | Hay envío de resumen, pero no un servicio diario separado y formal | Medio | Alta |
| Scheduler interno | Sí, `SyncScheduler` existe | Falta integración más clara con despliegue real | Medio | Media |
| Reenviar reporte por correo | Parcial | Existe la base de presentación, pero no un flujo completo dedicado | Medio | Media |
| Health endpoint sin info sensible | Sí, `HealthServlet` expone estado básico | Falta una validación de seguridad más fuerte | Bajo | Media |
| Estructura de paquetes solicitada | Parcial | La estructura real no coincide con la pedida en el documento | Medio | Alta |
| README requerido | No | Falta el README completo pedido por la especificación | Alto | Alta |
| Scripts SQL de auditoría/índices | No visible en el repo | Faltan `scripts/` SQL formales | Medio | Alta |
| Manejo configurado de `EMAILID` y `ROWSTAMP` | Parcial | Hay soporte por secuencias, pero falta documentación y scripts de apoyo | Medio | Alta |

## Qué Está Bien

- El proyecto ya compila y empaqueta.
- La web básica ya existe.
- Hay cliente LDAP.
- Hay persistencia Oracle.
- Hay auditoría.
- Hay scheduler.
- Hay scripts de entorno para Maven y Tomcat.

## Qué Falta Para Alinearla

1. Separar `SyncOrchestrator` en servicios por proceso.
2. Agregar reportes e histórico como capa formal.
3. Completar documentación operativa.
4. Aterrizar scripts SQL y despliegue.
5. Agregar más pruebas para los flujos críticos.

## Criterio Para Próximos Cambios

Antes de programar algo nuevo:

1. Revisar el requerimiento exacto.
2. Revisar lo que ya existe.
3. Reusar lo existente si cubre el caso.
4. Solo crear nuevas piezas si la arquitectura lo pide o si hay un hueco real.

