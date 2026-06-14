# 00 - Contexto compartido

Eres uno de varios agentes trabajando en paralelo sobre el proyecto `sync_ad_maximo`.

Objetivo común:
- Construir una solución Java Web para Tomcat 9 que sincronice Active Directory con Oracle MAXIMO.
- Mantener compatibilidad con `javax.servlet.*` y NO usar `jakarta.*`.
- Producir una salida clara, integrada y lista para ejecutar.

Reglas globales:
- Responde en español.
- No uses ni reproduzcas secretos. Si ves claves, trátalas como [REDACTED].
- No intentes modificar el trabajo de otros módulos.
- No dependas de resultados de otros agentes para avanzar; tu salida debe ser autocontenida.
- Si detectas una decisión de diseño compartida, indícala como supuesto explícito.

Contexto funcional crítico:
- AD y MAXIMO se integran con estas reglas principales:
  - `MAXIMO.PERSON.EPP_CEDULA = AD.postalCode`
  - `MAXIMO.PERSON.PERSONID = AD.sAMAccountName`
  - `MAXIMO.PERSON.EMAILADDRESS = AD.mail`
  - usuario web autorizado único: `maxadmin`
- El proyecto gira alrededor de sincronización de usuarios, validación de duplicados, auditoría, reportes y notificación por correo.

Contexto técnico crítico:
- Tomcat 9.
- Java Web clásico con servlets.
- Configuración externa por ambiente.
- Importa la trazabilidad y la separación por capas.

Tu trabajo como módulo compartido:
- Leer este contexto y el documento fuente principal del proyecto.
- Extraer solo los hechos que todos los módulos necesitan.
- No entrar en detalle de implementación específica de un subsistema.
- Preparar el terreno para que los demás módulos puedan trabajar sin pisarse.

Salida esperada de este prompt:
- Un resumen corto y estructurado del contexto común.
- Una lista de reglas que todos los demás módulos deben respetar.
- Una lista de supuestos comunes si faltan datos.

Formato sugerido:
- `Contexto del proyecto`
- `Reglas compartidas`
- `Supuestos comunes`
- `Riesgos transversales`

No escribas código. No inventes archivos. No cambies nada fuera de tu respuesta.
