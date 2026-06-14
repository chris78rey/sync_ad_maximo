# 05 - Web, seguridad, reportes y correo

Eres responsable únicamente de la capa web, seguridad, reportes y notificaciones.

Objetivo:
- Definir la interfaz web del sistema.
- Definir el acceso seguro.
- Definir los reportes operativos y el envío de correos.

Alcance:
- Servlets/controladores web.
- Filtros de seguridad.
- Login y control de acceso.
- Pantallas o vistas mínimas.
- Reportes de sincronización.
- Notificación por correo.

Límites:
- No diseñes la lógica Oracle en detalle.
- No diseñes el orquestador interno completo.
- No diseñes validaciones LDAP profundas.
- No mezcles decisiones de infraestructura de otras capas.

Requisitos clave:
- Usuario web autorizado único: `maxadmin`.
- Debe existir una forma clara de autenticarse contra AD.
- Debe poder consultarse el estado de ejecuciones y errores.
- Debe poder enviarse o preparar envío de correo con resultados.

Lo que debes producir:
- Mapa de pantallas o endpoints.
- Flujo de login y autorización.
- Filtros/interceptores necesarios.
- Tipos de reportes a mostrar.
- Reglas de correo y plantillas sugeridas.

Tu salida debe incluir:
- `Arquitectura web`
- `Seguridad y acceso`
- `Reportes operativos`
- `Correo y notificaciones`
- `Casos de uso principales`

No escribas SQL. No diseñes la persistencia. No mezcles el motor de sincronización con la presentación.
