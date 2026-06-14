# 01 - Arquitectura y esqueleto

Eres responsable únicamente de definir la arquitectura base del proyecto `sync_ad_maximo`.

Objetivo:
- Proponer el esqueleto del proyecto Java Web para Tomcat 9.
- Definir paquetes, capas, responsabilidades y dependencias base.
- Dejar lista la estructura para que los demás módulos encajen sin conflicto.

Alcance:
- Estructura de paquetes.
- Ciclo de arranque de la aplicación.
- Contratos base entre capas.
- Configuración y bootstrap.
- Dependencias Maven/Gradle solo a nivel de diseño.

Límites:
- No diseñes la lógica LDAP en detalle.
- No diseñes la lógica Oracle en detalle.
- No diseñes el orquestador completo ni la parte web completa.
- No reutilices espacio de otros módulos para resolver problemas de este módulo.

Lo que debes producir:
- Árbol de paquetes recomendado.
- Responsabilidad de cada paquete/clase principal.
- Punto de entrada de la aplicación.
- Componentes de configuración.
- Interfaz entre servicios, repositorios y controladores.
- Riesgos arquitectónicos y cómo mitigarlos.

Puntos que debes respetar:
- Tomcat 9.
- `javax.servlet.*`.
- Configuración externa por ambiente.
- Separación clara entre dominio, infraestructura y presentación.
- El proyecto debe soportar ejecución web y procesos de sincronización internos.

Tu salida debe incluir:
- `Resumen ejecutivo`
- `Estructura propuesta`
- `Componentes principales`
- `Dependencias sugeridas`
- `Riesgos y decisiones`

No toques la capa de negocio específica. No escribas SQL. No escribas validaciones LDAP detalladas.
